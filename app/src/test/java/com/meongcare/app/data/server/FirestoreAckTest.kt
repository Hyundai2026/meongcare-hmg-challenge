package com.meongcare.app.data.server

import com.meongcare.app.domain.trip.AckRead
import com.meongcare.app.domain.trip.CaseRef
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 013 U1: `FirestoreJson.ackOf`와 `FirestoreCaseSender.readAck`. 가짜 HttpTransport로 JVM에서 돈다. */
class FirestoreAckTest {

    private val config = serverConfigOf("demo-meongcare", "", "127.0.0.1", isDebugBuild = true)!!
    private val ref = CaseRef("demo-fast", "abc123")
    private val signUp = HttpResponse(200, """{"idToken": "token-1", "localId": "uid-1"}""")

    private class FakeTransport(val respond: (HttpRequest) -> HttpResponse) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        override suspend fun send(request: HttpRequest): HttpResponse {
            requests += request
            return respond(request)
        }
    }

    private fun sender(caseResponse: (HttpRequest) -> HttpResponse): Pair<FirestoreCaseSender, FakeTransport> {
        val fake = FakeTransport { if (it.url.contains("accounts:signUp")) signUp else caseResponse(it) }
        return FirestoreCaseSender(config, AnonymousAuth(config, fake), fake) { it.name } to fake
    }

    private fun doc(ack: String?) = HttpResponse(
        200,
        """{"name": "projects/demo-meongcare/databases/(default)/documents/hospitals/demo-fast/cases/abc123",
           "fields": {${ack?.let { "\"acknowledgedAt\": $it" } ?: ""}}}""",
    )

    @Test
    fun `확인 시각이 있으면 확인됨이다`() = runTest {
        val (sender, _) = sender { doc("""{"timestampValue": "2026-11-06T05:01:30.123456Z"}""") }
        assertEquals(AckRead.Acknowledged(Instant.parse("2026-11-06T05:01:30.123456Z")), sender.readAck(ref))
    }

    @Test
    fun `확인 시각이 null이거나 없으면 아직이다`() = runTest {
        assertEquals(AckRead.Pending, sender { doc("""{"nullValue": null}""") }.first.readAck(ref))
        assertEquals(AckRead.Pending, sender { doc(null) }.first.readAck(ref))
        // mask로 읽은 필드가 모두 없으면 fields 자체가 빠질 수 있다
        assertEquals(AckRead.Pending, sender { HttpResponse(200, """{"name": "x"}""") }.first.readAck(ref))
    }

    @Test
    fun `확인 시각 형식이 틀리면 실패다`() = runTest {
        assertEquals(AckRead.Failed("형식 오류"), sender { doc("""{"stringValue": "2026-11-06T05:01:30Z"}""") }.first.readAck(ref))
        assertEquals(AckRead.Failed("형식 오류"), sender { doc("""{"timestampValue": "어제"}""") }.first.readAck(ref))
        assertEquals(AckRead.Failed("형식 오류"), sender { doc("\"문자열\"") }.first.readAck(ref))
        assertEquals(AckRead.Failed("형식 오류"), sender { HttpResponse(200, "<html>") }.first.readAck(ref))
    }

    @Test
    fun `404와 403은 건 없음이다`() = runTest {
        assertEquals(AckRead.Gone, sender { HttpResponse(404, "{}") }.first.readAck(ref))
        assertEquals(AckRead.Gone, sender { HttpResponse(403, "{}") }.first.readAck(ref))
    }

    @Test
    fun `401과 5xx는 실패다`() = runTest {
        assertEquals(AckRead.Failed("HTTP 401"), sender { HttpResponse(401, "{}") }.first.readAck(ref))
        assertEquals(AckRead.Failed("HTTP 503"), sender { HttpResponse(503, "{}") }.first.readAck(ref))
    }

    @Test
    fun `네트워크 오류는 실패다`() = runTest {
        assertEquals(AckRead.Failed("네트워크"), sender { throw IOException("reset") }.first.readAck(ref))
    }

    @Test
    fun `익명 로그인에 실패하면 실패다`() = runTest {
        val fake = FakeTransport { throw IOException("refused") }
        val sender = FirestoreCaseSender(config, AnonymousAuth(config, fake), fake) { it.name }
        assertEquals(AckRead.Failed("익명 로그인 실패"), sender.readAck(ref))
    }

    @Test
    fun `요청은 자기 건 주소에 acknowledgedAt만 마스크하고 Bearer 토큰을 붙인다`() = runTest {
        val (sender, fake) = sender { doc(null) }
        sender.readAck(ref)
        val get = fake.requests.last()
        assertEquals("GET", get.method)
        assertEquals(
            "http://127.0.0.1:8080/v1/projects/demo-meongcare/databases/(default)/documents/hospitals/demo-fast/cases/abc123" +
                "?mask.fieldPaths=acknowledgedAt",
            get.url,
        )
        assertEquals("Bearer token-1", get.headers["Authorization"])
        assertNull(get.body)
    }
}
