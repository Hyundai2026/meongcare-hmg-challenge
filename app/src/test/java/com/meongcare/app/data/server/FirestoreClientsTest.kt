package com.meongcare.app.data.server

import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalBoard
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.trip.CaseEta
import com.meongcare.app.domain.trip.CaseRef
import com.meongcare.app.domain.trip.CaseStatus
import com.meongcare.app.domain.trip.SendResult
import com.meongcare.app.domain.trip.caseCardOf
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AnonymousAuth, FirestoreHospitalBoardSource, FirestoreCaseSender (계획 012 단계 12-2). 가짜 HttpTransport로 JVM에서 돈다. */
class FirestoreClientsTest {

    private val config = serverConfigOf("demo-meongcare", "", "10.0.2.2", isDebugBuild = true)!!
    private val at = Instant.parse("2026-11-06T05:00:00Z")

    /** URL 앞부분에 맞는 응답을 돌려주는 가짜. 받은 요청을 남긴다. */
    private class FakeTransport(val routes: MutableMap<String, () -> HttpResponse>) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        override suspend fun send(request: HttpRequest): HttpResponse {
            requests += request
            val handler = routes.entries.firstOrNull { request.url.contains(it.key) }?.value ?: error("모르는 요청 ${request.url}")
            return handler()
        }
    }

    private val signUpOk = { HttpResponse(200, """{"idToken": "token-1", "localId": "uid-1"}""") }
    private val listOk = {
        HttpResponse(
            200,
            """{"documents": [{"name": "projects/p/databases/(default)/documents/hospitals/demo-fast", "fields": {
                "schemaVersion": {"integerValue": "1"}, "name": {"stringValue": "바른"},
                "latitude": {"doubleValue": 37.367}, "longitude": {"doubleValue": 127.1083}, "isSample": {"booleanValue": true}}}]}""",
        )
    }

    private fun transport(vararg routes: Pair<String, () -> HttpResponse>) = FakeTransport(mutableMapOf(*routes))

    @Test
    fun `익명 가입은 한 번만 하고 토큰을 다시 쓴다`() = runTest {
        val fake = transport("accounts:signUp" to signUpOk, "/hospitals" to listOk)
        val source = FirestoreHospitalBoardSource(config, AnonymousAuth(config, fake), fake)
        listOf(async { source.load() }, async { source.load() }).awaitAll()
        source.load()
        assertEquals(1, fake.requests.count { it.url.contains("accounts:signUp") })
        assertEquals(3, fake.requests.count { it.method == "GET" })
    }

    @Test
    fun `요청에 Bearer 토큰을 붙이고 목록을 서버 출처로 준다`() = runTest {
        val fake = transport("accounts:signUp" to signUpOk, "/hospitals" to listOk)
        val log = mutableListOf<String>()
        val board = FirestoreHospitalBoardSource(config, AnonymousAuth(config, fake), fake, log = { log += it }).load() as HospitalBoard.Loaded
        assertEquals(HospitalOrigin.SERVER, board.origin)
        assertEquals(listOf("demo-fast"), board.candidates.map { it.hospital.id })
        val get = fake.requests.single { it.method == "GET" }
        assertEquals("Bearer token-1", get.headers["Authorization"])
        assertTrue(get.url.startsWith("http://10.0.2.2:8080/v1/projects/demo-meongcare/databases/(default)/documents/hospitals"))
        assertTrue(log.single().startsWith("병원 목록: 서버 1곳"))
    }

    @Test
    fun `HTTP 오류면 Unavailable이다`() = runTest {
        val fake = transport("accounts:signUp" to signUpOk, "/hospitals" to { HttpResponse(403, "{}") })
        assertEquals(HospitalBoard.Unavailable("HTTP 403"), FirestoreHospitalBoardSource(config, AnonymousAuth(config, fake), fake).load())
    }

    @Test
    fun `응답이 JSON이 아니면 Unavailable이다`() = runTest {
        val fake = transport("accounts:signUp" to signUpOk, "/hospitals" to { HttpResponse(200, "<html>") })
        assertEquals(HospitalBoard.Unavailable("형식 오류"), FirestoreHospitalBoardSource(config, AnonymousAuth(config, fake), fake).load())
    }

    @Test
    fun `네트워크가 안 되면 Unavailable이고 다음에 다시 가입한다`() = runTest {
        var fail = true
        val fake = transport("accounts:signUp" to { if (fail) throw IOException("refused") else signUpOk() }, "/hospitals" to listOk)
        val source = FirestoreHospitalBoardSource(config, AnonymousAuth(config, fake), fake)
        assertEquals(HospitalBoard.Unavailable("익명 로그인 실패"), source.load())
        fail = false
        assertTrue(source.load() is HospitalBoard.Loaded)
    }

    private val hospital = Hospital(Destination("바른", 37.367, 127.1083), "02-0000-0001", isSample = true, id = "demo-fast")
    private val card = caseCardOf(SamplePetProfiles.bori, setOf(Symptom.COUGH))

    @Test
    fun `카드는 POST로 만들고 문서 이름에서 ID를 돌려준다`() = runTest {
        val fake = transport(
            "accounts:signUp" to signUpOk,
            "/hospitals/demo-fast/cases" to { HttpResponse(200, """{"name": "projects/p/databases/(default)/documents/hospitals/demo-fast/cases/abc123"}""") },
        )
        val sender = FirestoreCaseSender(config, AnonymousAuth(config, fake), fake) { "기침" }
        val result = sender.send(hospital, card, CaseEta(12, at))
        assertEquals(SendResult.Sent(CaseRef("demo-fast", "abc123")), result)
        val post = fake.requests.last()
        assertEquals("POST", post.method)
        val fields = JSONObject(post.body!!).getJSONObject("fields")
        assertEquals("uid-1", fields.getJSONObject("createdBy").getString("stringValue"))
        assertEquals("en_route", fields.getJSONObject("status").getString("stringValue"))
    }

    @Test
    fun `병원 ID가 없거나 HTTP 오류면 실패를 돌려준다`() = runTest {
        val fake = transport("accounts:signUp" to signUpOk, "/cases" to { HttpResponse(403, "{}") })
        val sender = FirestoreCaseSender(config, AnonymousAuth(config, fake), fake) { it.name }
        assertEquals(SendResult.Failed("병원 ID 없음"), sender.send(hospital.copy(id = null), card, CaseEta(1, at)))
        assertEquals(SendResult.Failed("HTTP 403"), sender.send(hospital, card, CaseEta(1, at)))
    }

    @Test
    fun `도착 예정 갱신은 commit으로 바꾼 필드만 보낸다`() = runTest {
        val fake = transport("accounts:signUp" to signUpOk, ":commit" to { HttpResponse(200, "{}") })
        val sender = FirestoreCaseSender(config, AnonymousAuth(config, fake), fake) { it.name }
        assertTrue(sender.updateEta(CaseRef("demo-fast", "abc"), CaseEta(9, at)))
        val commit = fake.requests.last()
        assertEquals("http://10.0.2.2:8080/v1/projects/demo-meongcare/databases/(default)/documents:commit", commit.url)
        val write = JSONObject(commit.body!!).getJSONArray("writes").getJSONObject(0)
        assertEquals(
            "projects/demo-meongcare/databases/(default)/documents/hospitals/demo-fast/cases/abc",
            write.getJSONObject("update").getString("name"),
        )
        assertEquals(3, write.getJSONObject("updateMask").getJSONArray("fieldPaths").length())
    }

    @Test
    fun `실패는 던지지 않고 결과로 돌려준다`() = runTest {
        val fake = transport("accounts:signUp" to signUpOk, ":commit" to { throw IOException("down") })
        val sender = FirestoreCaseSender(config, AnonymousAuth(config, fake), fake) { it.name }
        assertFalse(sender.updateEta(CaseRef("h", "c"), CaseEta(1, at)))
        assertFalse(sender.finish(CaseRef("h", "c"), CaseStatus.ARRIVED, at))
    }
}
