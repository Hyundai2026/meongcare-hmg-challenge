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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AnonymousAuth, FirestoreHospitalBoardSource, FirestoreCaseSender 경계 (가짜 HttpTransport, JVM).
 * 기준: 계획 012 "설계 결정 3"(익명 가입은 한 번, 실패하면 다음에 다시, 결과 값, 로그 규칙),
 * "설계 결정 6"(로그: 결과 종류·개수·시간·HTTP 상태만), `HospitalBoardSource`·`CaseSender` KDoc(취소 말고는 던지지 않는다),
 * docs/data/emergency-case.md(경로 `hospitals/{id}/cases`).
 */
class FirestoreClientsBoundaryTest {

    private val config = serverConfigOf("demo-meongcare", "", "10.0.2.2", isDebugBuild = true)!!
    private val at = Instant.parse("2026-11-06T05:00:00Z")

    private class FakeTransport(val routes: Map<String, suspend (HttpRequest) -> HttpResponse>) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        override suspend fun send(request: HttpRequest): HttpResponse {
            requests += request
            val handler = routes.entries.firstOrNull { request.url.contains(it.key) }?.value ?: error("모르는 요청 ${request.url}")
            return handler(request)
        }
    }

    private fun transport(vararg routes: Pair<String, suspend (HttpRequest) -> HttpResponse>) = FakeTransport(mapOf(*routes))

    /** URL에 이 글자가 들어 있는 요청에 [handler]로 답한다 */
    private infix fun String.answers(handler: suspend (HttpRequest) -> HttpResponse): Pair<String, suspend (HttpRequest) -> HttpResponse> = this to handler

    private val signUpOk: suspend (HttpRequest) -> HttpResponse = { HttpResponse(200, """{"idToken": "token-1", "localId": "uid-1"}""") }

    private val listBody = """{"documents": [{"name": "projects/p/databases/(default)/documents/hospitals/demo-fast", "fields": {
        "schemaVersion": {"integerValue": "1"}, "name": {"stringValue": "시연용 바른 동물병원"},
        "latitude": {"doubleValue": 37.367}, "longitude": {"doubleValue": 127.1083}, "isSample": {"booleanValue": true}}},
        {"name": "projects/p/databases/(default)/documents/hospitals/broken", "fields": {
        "schemaVersion": {"integerValue": "2"}, "name": {"stringValue": "모르는 버전"},
        "latitude": {"doubleValue": 37.0}, "longitude": {"doubleValue": 127.0}}}]}"""

    // ---- 익명 가입 ----

    @Test
    fun `가입 요청은 설정의 가입 주소로 returnSecureToken을 보낸다`() = runTest {
        val fake = transport("accounts:signUp" answers signUpOk)
        assertEquals(AuthSession("token-1", "uid-1"), AnonymousAuth(config, fake).session())
        val request = fake.requests.single()
        assertEquals("POST", request.method)
        assertEquals(config.signUpUrl, request.url)
        assertTrue(JSONObject(request.body!!).getBoolean("returnSecureToken"))
        assertNull("가입 요청에는 토큰이 없다", request.headers["Authorization"])
    }

    @Test
    fun `가입 응답에 토큰이나 uid가 없으면 실패이고 다음에 다시 가입한다`() = runTest {
        listOf(
            """{"localId": "uid-1"}""",
            """{"idToken": "token-1"}""",
            """{"idToken": "", "localId": "uid-1"}""",
            "not json",
            "[]",
        ).forEach { body ->
            var answer = HttpResponse(200, body)
            val fake = transport("accounts:signUp" answers { answer })
            val auth = AnonymousAuth(config, fake)
            assertNull(body, auth.session())
            answer = HttpResponse(200, """{"idToken": "token-2", "localId": "uid-2"}""")
            assertEquals(body, AuthSession("token-2", "uid-2"), auth.session())
            assertEquals(body, 2, fake.requests.size)
        }
    }

    @Test
    fun `가입이 HTTP 오류면 실패이고 다음에 다시 가입한 뒤에는 다시 가입하지 않는다`() = runTest {
        var status = 400
        val fake = transport("accounts:signUp" answers { HttpResponse(status, """{"idToken": "t", "localId": "u"}""") })
        val auth = AnonymousAuth(config, fake)
        assertNull(auth.session())
        status = 200
        assertEquals(AuthSession("t", "u"), auth.session())
        auth.session()
        assertEquals(2, fake.requests.size)
    }

    // ---- 병원 목록 ----

    @Test
    fun `가입에 실패하면 목록을 요청하지 않고 Unavailable이다`() = runTest {
        val fake = transport("accounts:signUp" answers { HttpResponse(500, "") }, "/hospitals" answers { error("목록을 요청하면 안 된다") })
        val board = FirestoreHospitalBoardSource(config, AnonymousAuth(config, fake), fake).load()
        assertEquals(HospitalBoard.Unavailable("익명 로그인 실패"), board)
        assertTrue(fake.requests.none { it.method == "GET" })
    }

    @Test
    fun `목록 요청이 네트워크 오류면 Unavailable이다`() = runTest {
        val fake = transport("accounts:signUp" answers signUpOk, "/hospitals" answers { throw IOException("timeout") })
        assertEquals(HospitalBoard.Unavailable("네트워크"), FirestoreHospitalBoardSource(config, AnonymousAuth(config, fake), fake).load())
    }

    @Test
    fun `병원 문서가 없으면 빈 목록을 서버 출처로 준다`() = runTest {
        val fake = transport("accounts:signUp" answers signUpOk, "/hospitals" answers { HttpResponse(200, "{}") })
        assertEquals(HospitalBoard.Loaded(emptyList(), HospitalOrigin.SERVER), FirestoreHospitalBoardSource(config, AnonymousAuth(config, fake), fake).load())
    }

    @Test
    fun `형식이 틀린 병원만 빼고 준다`() = runTest {
        val fake = transport("accounts:signUp" answers signUpOk, "/hospitals" answers { HttpResponse(200, listBody) })
        val board = FirestoreHospitalBoardSource(config, AnonymousAuth(config, fake), fake).load() as HospitalBoard.Loaded
        assertEquals(listOf("demo-fast"), board.candidates.map { it.hospital.id })
        assertTrue(board.candidates.all { it.origin == HospitalOrigin.SERVER })
    }

    @Test
    fun `목록 요청은 GET으로 hospitals 컬렉션을 묻고 몸통이 없다`() = runTest {
        val fake = transport("accounts:signUp" answers signUpOk, "/hospitals" answers { HttpResponse(200, "{}") })
        FirestoreHospitalBoardSource(config, AnonymousAuth(config, fake), fake).load()
        val get = fake.requests.single { it.method == "GET" }
        assertTrue(get.url, get.url.startsWith("${config.documentsUrl}/hospitals"))
        assertFalse("하위 컬렉션을 묻지 않는다", get.url.contains("/cases") || get.url.contains("/staff"))
        assertNull(get.body)
    }

    @Test
    fun `성공 로그에는 개수와 걸린 시간만 있고 병원 이름과 좌표와 토큰이 없다`() = runTest {
        var clock = 1_000L
        val fake = transport("accounts:signUp" answers signUpOk, "/hospitals" answers { clock += 812; HttpResponse(200, listBody) })
        val log = mutableListOf<String>()
        FirestoreHospitalBoardSource(config, AnonymousAuth(config, fake), fake, clockMillis = { clock }, log = { log += it }).load()
        assertEquals(listOf("병원 목록: 서버 1곳 (812ms)"), log)
    }

    @Test
    fun `실패 로그에는 이유와 걸린 시간만 있다`() = runTest {
        val fake = transport("accounts:signUp" answers signUpOk, "/hospitals" answers { HttpResponse(403, """{"error": {"message": "uid-1 denied"}}""") })
        val log = mutableListOf<String>()
        FirestoreHospitalBoardSource(config, AnonymousAuth(config, fake), fake, clockMillis = { 0L }, log = { log += it }).load()
        assertEquals(listOf("병원 목록: 서버 실패 (HTTP 403, 0ms)"), log)
    }

    @Test
    fun `목록을 받는 중에 취소되면 취소를 그대로 전한다`() = runTest {
        val started = CompletableDeferred<Unit>()
        val fake = transport("accounts:signUp" answers signUpOk, "/hospitals" answers { started.complete(Unit); awaitCancellation() })
        val job = async { FirestoreHospitalBoardSource(config, AnonymousAuth(config, fake), fake).load() }
        started.await()
        job.cancel()
        assertTrue(runCatching { job.await() }.exceptionOrNull() is CancellationException)
    }

    // ---- 카드 전송 ----

    private val hospital = Hospital(Destination("시연용 바른 동물병원", 37.367, 127.1083), "02-0000-0001", isSample = true, id = "demo-fast")
    private val card = caseCardOf(SamplePetProfiles.demoPet, setOf(Symptom.COUGH))
    private val created: suspend (HttpRequest) -> HttpResponse =
        { HttpResponse(200, """{"name": "projects/demo-meongcare/databases/(default)/documents/hospitals/demo-fast/cases/abc"}""") }

    @Test
    fun `카드 요청은 그 병원의 cases 주소로 토큰과 JSON 형식을 붙여 보낸다`() = runTest {
        val fake = transport("accounts:signUp" answers signUpOk, "/cases" answers created)
        FirestoreCaseSender(config, AnonymousAuth(config, fake), fake) { it.name }.send(hospital, card, CaseEta(12, at))
        val post = fake.requests.last()
        assertEquals("POST", post.method)
        assertEquals("${config.documentsUrl}/hospitals/demo-fast/cases", post.url)
        assertEquals("Bearer token-1", post.headers["Authorization"])
        assertEquals("application/json", post.headers["Content-Type"])
        assertEquals(setOf("fields"), JSONObject(post.body!!).keySet())
    }

    @Test
    fun `가입에 실패하면 카드를 보내지 않고 실패를 돌려준다`() = runTest {
        val fake = transport("accounts:signUp" answers { throw IOException("down") }, "/cases" answers { error("보내면 안 된다") })
        val result = FirestoreCaseSender(config, AnonymousAuth(config, fake), fake) { it.name }.send(hospital, card, CaseEta(1, at))
        assertEquals(SendResult.Failed("익명 로그인 실패"), result)
        assertTrue(fake.requests.none { it.url.contains("/cases") })
    }

    @Test
    fun `카드 요청이 네트워크 오류면 실패를 돌려준다`() = runTest {
        val fake = transport("accounts:signUp" answers signUpOk, "/cases" answers { throw IOException("reset") })
        assertEquals(SendResult.Failed("네트워크"), FirestoreCaseSender(config, AnonymousAuth(config, fake), fake) { it.name }.send(hospital, card, CaseEta(1, at)))
    }

    @Test
    fun `응답에 문서 이름이 없거나 JSON이 아니면 형식 오류로 실패한다`() = runTest {
        listOf("{}", """{"name": ""}""", "<html>", """{"name": "projects/p/databases/(default)/documents/hospitals/demo-fast/cases/"}""").forEach { body ->
            val fake = transport("accounts:signUp" answers signUpOk, "/cases" answers { HttpResponse(200, body) })
            val result = FirestoreCaseSender(config, AnonymousAuth(config, fake), fake) { it.name }.send(hospital, card, CaseEta(1, at))
            assertEquals(body, SendResult.Failed("형식 오류"), result)
        }
    }

    @Test
    fun `가입에 실패하면 갱신과 도착 알림을 보내지 않고 false다`() = runTest {
        val fake = transport("accounts:signUp" answers { HttpResponse(400, "{}") }, ":commit" answers { error("보내면 안 된다") })
        val sender = FirestoreCaseSender(config, AnonymousAuth(config, fake), fake) { it.name }
        assertFalse(sender.updateEta(CaseRef("demo-fast", "abc"), CaseEta(3, at)))
        assertFalse(sender.finish(CaseRef("demo-fast", "abc"), CaseStatus.ARRIVED, at))
        assertTrue(fake.requests.none { it.url.contains(":commit") })
    }

    @Test
    fun `서버가 갱신을 거부하면 false다`() = runTest {
        listOf(400, 403, 404, 500).forEach { status ->
            val fake = transport("accounts:signUp" answers signUpOk, ":commit" answers { HttpResponse(status, "{}") })
            val sender = FirestoreCaseSender(config, AnonymousAuth(config, fake), fake) { it.name }
            assertFalse("$status", sender.updateEta(CaseRef("demo-fast", "abc"), CaseEta(3, at)))
            assertFalse("$status", sender.finish(CaseRef("demo-fast", "abc"), CaseStatus.CANCELLED, at))
        }
    }

    @Test
    fun `취소 알림은 cancelled와 status·etaUpdatedAt만 그 건의 이름으로 보낸다`() = runTest {
        val fake = transport("accounts:signUp" answers signUpOk, ":commit" answers { HttpResponse(200, "{}") })
        assertTrue(FirestoreCaseSender(config, AnonymousAuth(config, fake), fake) { it.name }.finish(CaseRef("demo-near", "c9"), CaseStatus.CANCELLED, at))
        val commit = fake.requests.last()
        assertEquals("POST", commit.method)
        assertEquals("Bearer token-1", commit.headers["Authorization"])
        val write = JSONObject(commit.body!!).getJSONArray("writes").getJSONObject(0)
        val update = write.getJSONObject("update")
        assertEquals("${config.documentsName}/hospitals/demo-near/cases/c9", update.getString("name"))
        assertEquals("cancelled", update.getJSONObject("fields").getJSONObject("status").getString("stringValue"))
        assertEquals(setOf("status", "etaUpdatedAt"), update.getJSONObject("fields").keySet())
    }

    @Test
    fun `한 번 가입한 계정으로 보내기와 갱신과 도착을 모두 한다`() = runTest {
        val fake = transport("accounts:signUp" answers signUpOk, ":commit" answers { HttpResponse(200, "{}") }, "/cases" answers created)
        val sender = FirestoreCaseSender(config, AnonymousAuth(config, fake), fake) { it.name }
        val ref = (sender.send(hospital, card, CaseEta(12, at)) as SendResult.Sent).ref
        assertTrue(sender.updateEta(ref, CaseEta(11, at.plusSeconds(60))))
        assertTrue(sender.finish(ref, CaseStatus.ARRIVED, at.plusSeconds(600)))
        assertEquals(1, fake.requests.count { it.url.contains("accounts:signUp") })
        assertEquals(CaseRef("demo-fast", "abc"), ref)
    }

    @Test
    fun `보낸 카드의 createdBy는 가입한 계정의 uid다`() = runTest {
        val fake = transport("accounts:signUp" answers { HttpResponse(200, """{"idToken": "tk", "localId": "car-uid-7"}""") }, "/cases" answers created)
        FirestoreCaseSender(config, AnonymousAuth(config, fake), fake) { it.name }.send(hospital, card, CaseEta(12, at))
        val fields = JSONObject(fake.requests.last().body!!).getJSONObject("fields")
        assertEquals("car-uid-7", fields.getJSONObject("createdBy").getString("stringValue"))
        assertFalse("토큰은 몸통에 넣지 않는다", fake.requests.last().body!!.contains("tk\""))
    }
}
