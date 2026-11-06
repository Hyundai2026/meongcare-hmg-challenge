package com.meongcare.app.data.server

import com.meongcare.app.domain.trip.AckRead
import com.meongcare.app.domain.trip.CaseRef
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 013 U1 경계: `FirestoreJson.ackOf`와 `FirestoreCaseSender.readAck` (계획 013 설계 결정 5 표) */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreAckBoundaryTest {

    private val config = serverConfigOf("demo-meongcare", "", "127.0.0.1", isDebugBuild = true)!!
    private val ref = CaseRef("demo-fast", "abc123")
    private val signUp = HttpResponse(200, """{"idToken": "token-1", "localId": "uid-1"}""")

    private class FakeTransport(val respond: suspend (HttpRequest) -> HttpResponse) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        override suspend fun send(request: HttpRequest): HttpResponse {
            requests += request
            return respond(request)
        }
    }

    private fun sender(caseResponse: suspend (HttpRequest) -> HttpResponse): FirestoreCaseSender {
        val fake = FakeTransport { if (it.url.contains("accounts:signUp")) signUp else caseResponse(it) }
        return FirestoreCaseSender(config, AnonymousAuth(config, fake), fake) { it.name }
    }

    @Test
    fun `마스크가 무시되어 다른 필드가 함께 와도 확인 시각만 본다`() = runTest {
        val body = """{"name": "x", "fields": {
            "status": {"stringValue": "arrived"},
            "createdBy": {"stringValue": "uid-1"},
            "acknowledgedAt": {"timestampValue": "2026-11-06T05:01:30Z"}}}"""
        assertEquals(AckRead.Acknowledged(Instant.parse("2026-11-06T05:01:30Z")), sender { HttpResponse(200, body) }.readAck(ref))
        val pending = """{"name": "x", "fields": {"status": {"stringValue": "en_route"}}}"""
        assertEquals(AckRead.Pending, sender { HttpResponse(200, pending) }.readAck(ref))
    }

    @Test
    fun `소수점 없는 확인 시각과 나노초 확인 시각을 모두 읽는다`() = runTest {
        for (text in listOf("2026-11-06T05:01:30Z", "2026-11-06T05:01:30.123456789Z")) {
            val body = """{"fields": {"acknowledgedAt": {"timestampValue": "$text"}}}"""
            assertEquals(text, AckRead.Acknowledged(Instant.parse(text)), sender { HttpResponse(200, body) }.readAck(ref))
        }
    }

    @Test
    fun `빈 몸통이나 배열 몸통은 형식 오류다`() = runTest {
        for (body in listOf("", "[]", "null")) {
            assertEquals("body=$body", AckRead.Failed("형식 오류"), sender { HttpResponse(200, body) }.readAck(ref))
        }
    }

    @Test
    fun `확인 시각 값이 null이거나 숫자이거나 맵이면 형식 오류다`() = runTest {
        val values = listOf(
            """{"timestampValue": null}""",
            """{"timestampValue": 1762405290}""",
            """{"mapValue": {"fields": {}}}""",
            """{}""",
        )
        for (value in values) {
            val body = """{"fields": {"acknowledgedAt": $value}}"""
            assertEquals(value, AckRead.Failed("형식 오류"), sender { HttpResponse(200, body) }.readAck(ref))
        }
    }

    @Test
    fun `404와 403은 몸통이 JSON이 아니어도 건 없음이다`() = runTest {
        assertEquals(AckRead.Gone, sender { HttpResponse(404, "<html>Not Found</html>") }.readAck(ref))
        assertEquals(AckRead.Gone, sender { HttpResponse(403, "") }.readAck(ref))
    }

    @Test
    fun `그 밖의 HTTP 상태는 실패이고 상태 코드를 이유로 쓴다`() = runTest {
        for (status in listOf(400, 429, 500, 502)) {
            assertEquals("$status", AckRead.Failed("HTTP $status"), sender { HttpResponse(status, "{}") }.readAck(ref))
        }
    }

    @Test
    fun `읽는 중에 취소되면 실패로 바꾸지 않고 취소를 그대로 전한다`() = runTest {
        val sender = sender { awaitCancellation() }
        var result: AckRead? = null
        val job = launch { result = sender.readAck(ref) }
        runCurrent()
        job.cancel()
        runCurrent()
        assertTrue(job.isCancelled)
        assertNull(result)
    }

    @Test
    fun `토큰을 미리 갱신하면 새 토큰으로 같은 계정의 건을 읽는다`() = runTest {
        var clock = Instant.parse("2026-11-06T05:00:00Z")
        val fake = FakeTransport { request ->
            when {
                request.url.contains("accounts:signUp") -> HttpResponse(
                    200,
                    """{"idToken": "token-1", "localId": "uid-1", "refreshToken": "r1", "expiresIn": "3600"}""",
                )
                request.url.contains("securetoken") -> HttpResponse(
                    200,
                    """{"id_token": "token-2", "refresh_token": "r2", "expires_in": "3600", "user_id": "uid-1"}""",
                )
                else -> HttpResponse(200, """{"fields": {"acknowledgedAt": {"nullValue": null}}}""")
            }
        }
        val sender = FirestoreCaseSender(config, AnonymousAuth(config, fake, now = { clock }), fake) { it.name }
        assertEquals(AckRead.Pending, sender.readAck(ref))
        assertEquals("Bearer token-1", fake.requests.last().headers["Authorization"])

        clock = clock.plusSeconds(55 * 60) // 만료 5분 전
        assertEquals(AckRead.Pending, sender.readAck(ref))
        val get = fake.requests.last()
        assertEquals("GET", get.method)
        assertEquals("Bearer token-2", get.headers["Authorization"])
        assertEquals(1, fake.requests.count { it.url.contains("accounts:signUp") })
    }

    @Test
    fun `ackOf는 fields가 없으면 아직이고 acknowledgedAt 이름만 있고 값이 객체가 아니면 형식 오류다`() {
        assertEquals(FirestoreJson.Ack.Pending, FirestoreJson.ackOf("""{"name": "x"}"""))
        assertEquals(FirestoreJson.Ack.Malformed, FirestoreJson.ackOf("""{"fields": {"acknowledgedAt": null}}"""))
        assertEquals(FirestoreJson.Ack.Malformed, FirestoreJson.ackOf("""{"fields": {"acknowledgedAt": "2026-11-06T05:01:30Z"}}"""))
        assertEquals(FirestoreJson.Ack.Malformed, FirestoreJson.ackOf("not json"))
    }
}
