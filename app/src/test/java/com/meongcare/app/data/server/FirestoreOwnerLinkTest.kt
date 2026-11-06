package com.meongcare.app.data.server

import com.meongcare.app.domain.link.LinkCheck
import com.meongcare.app.domain.link.LinkStart
import com.meongcare.app.domain.link.OwnerLinkState
import java.io.IOException
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 계획 014 단계 8-2 (docs/data/owner-link.md). 가짜 HttpTransport로 JVM에서 돈다 */
class FirestoreOwnerLinkTest {

    private val config = serverConfigOf("demo-meongcare", "", "10.0.2.2", isDebugBuild = true)!!
    private val at = Instant.parse("2026-11-06T09:00:00Z")
    private val docs = "http://10.0.2.2:8080/v1/projects/demo-meongcare/databases/(default)/documents"

    /** (메서드, URL 일부) → 응답. 받은 요청을 남긴다 */
    private class FakeTransport(val handler: (HttpRequest) -> HttpResponse) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        override suspend fun send(request: HttpRequest): HttpResponse {
            if (request.url.contains("accounts:signUp")) return HttpResponse(200, """{"idToken": "token-1", "localId": "car-uid-1"}""")
            requests += request
            return handler(request)
        }
    }

    private fun codeDoc(ownerUid: String?) = """{"fields": {"schemaVersion": {"integerValue": "1"}, "carUid": {"stringValue": "car-uid-1"},
        "createdAt": {"timestampValue": "2026-11-06T09:00:00Z"},
        "ownerUid": ${if (ownerUid == null) """{"nullValue": null}""" else """{"stringValue": "$ownerUid"}"""},
        "claimedAt": {"nullValue": null}}}"""

    private fun link(fake: FakeTransport, log: MutableList<String> = mutableListOf()) =
        FirestoreOwnerLink(config, AnonymousAuth(config, fake), fake, Random(5), now = { at }, log = { log += it })

    @Test
    fun `코드 문서는 서버 시각과 빈 보호자로 새로 만든다`() = runTest {
        val fake = FakeTransport { HttpResponse(200, "{}") }
        val created = link(fake).start() as LinkStart.Created
        assertEquals(at, created.createdAt)
        val commit = fake.requests.single()
        assertEquals("POST", commit.method)
        assertEquals("$docs:commit", commit.url)
        val write = JSONObject(commit.body!!).getJSONArray("writes").getJSONObject(0)
        assertEquals(
            "projects/demo-meongcare/databases/(default)/documents/linkCodes/${created.code.value}",
            write.getJSONObject("update").getString("name"),
        )
        val fields = write.getJSONObject("update").getJSONObject("fields")
        assertEquals("car-uid-1", fields.getJSONObject("carUid").getString("stringValue"))
        assertTrue(fields.getJSONObject("ownerUid").has("nullValue"))
        assertTrue(fields.getJSONObject("claimedAt").has("nullValue"))
        assertEquals("1", fields.getJSONObject("schemaVersion").getString("integerValue"))
        assertFalse(fields.has("createdAt"))
        val transform = write.getJSONArray("updateTransforms").getJSONObject(0)
        assertEquals("createdAt", transform.getString("fieldPath"))
        assertEquals("REQUEST_TIME", transform.getString("setToServerValue"))
        assertFalse(write.getJSONObject("currentDocument").getBoolean("exists"))
    }

    @Test
    fun `코드가 이미 있으면 새 코드로 다시 만들고 세 번까지 한다`() = runTest {
        var calls = 0
        val fake = FakeTransport { calls++; if (calls < 3) HttpResponse(409, "{}") else HttpResponse(200, "{}") }
        val created = link(fake).start()
        assertTrue(created is LinkStart.Created)
        val names = fake.requests.map { JSONObject(it.body!!).getJSONArray("writes").getJSONObject(0).getJSONObject("update").getString("name") }
        assertEquals(3, names.toSet().size)

        val always = FakeTransport { HttpResponse(409, "{}") }
        assertTrue(link(always).start() is LinkStart.Failed)
        assertEquals(3, always.requests.size)
    }

    @Test
    fun `claim 전이면 기다림이다`() = runTest {
        val fake = FakeTransport { HttpResponse(200, codeDoc(null)) }
        val link = link(fake)
        val code = (link.start() as LinkStart.Created).code
        assertEquals(LinkCheck.Waiting, link.check(code))
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
        assertNull(link.ownerUid)
    }

    @Test
    fun `보호자가 claim하면 연결됨이고 코드 문서를 지운다`() = runTest {
        val fake = FakeTransport { if (it.method == "GET") HttpResponse(200, codeDoc("owner-uid-1")) else HttpResponse(200, "{}") }
        val link = link(fake)
        val code = (link.start() as LinkStart.Created).code
        assertEquals(LinkCheck.Linked, link.check(code))
        assertEquals(OwnerLinkState.Linked(at), link.state.value)
        assertEquals("owner-uid-1", link.ownerUid)
        val delete = fake.requests.last()
        assertEquals("DELETE", delete.method)
        assertEquals("$docs/linkCodes/${code.value}", delete.url)
    }

    @Test
    fun `코드 문서가 없으면 코드 없음이다`() = runTest {
        for (status in listOf(403, 404)) {
            val fake = FakeTransport { if (it.method == "GET") HttpResponse(status, "{}") else HttpResponse(200, "{}") }
            val link = link(fake)
            val code = (link.start() as LinkStart.Created).code
            assertEquals(LinkCheck.Gone, link.check(code))
        }
    }

    @Test
    fun `연결을 끊으면 허가 문서를 지우고 연결 전이다`() = runTest {
        val fake = FakeTransport { if (it.method == "GET") HttpResponse(200, codeDoc("owner-uid-1")) else HttpResponse(200, "{}") }
        val link = link(fake)
        link.check((link.start() as LinkStart.Created).code)
        assertTrue(link.unlink())
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
        assertNull(link.ownerUid)
        val delete = fake.requests.last()
        assertEquals("DELETE", delete.method)
        assertEquals("$docs/petProfiles/owner-uid-1/readers/car-uid-1", delete.url)
    }

    @Test
    fun `허가를 지우지 못해도 차는 보호자를 잊는다`() = runTest {
        var failDeletes = false
        val fake = FakeTransport {
            when {
                it.method == "GET" -> HttpResponse(200, codeDoc("owner-uid-1"))
                it.method == "DELETE" && failDeletes -> throw IOException("끊김")
                else -> HttpResponse(200, "{}")
            }
        }
        val link = link(fake)
        link.check((link.start() as LinkStart.Created).code)
        failDeletes = true
        assertFalse(link.unlink())
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
        assertNull(link.ownerUid)
    }

    @Test
    fun `새로 연결하면 이전 허가를 먼저 지운다`() = runTest {
        var owner = "owner-uid-1"
        val fake = FakeTransport { if (it.method == "GET") HttpResponse(200, codeDoc(owner)) else HttpResponse(200, "{}") }
        val link = link(fake)
        link.check((link.start() as LinkStart.Created).code)
        owner = "owner-uid-2"
        link.check((link.start() as LinkStart.Created).code)
        assertTrue(fake.requests.any { it.method == "DELETE" && it.url == "$docs/petProfiles/owner-uid-1/readers/car-uid-1" })
        assertEquals("owner-uid-2", link.ownerUid)
    }

    @Test
    fun `보호자가 끊었다는 것을 알면 끊김을 거쳐 연결 전이다`() = runTest {
        val fake = FakeTransport { if (it.method == "GET") HttpResponse(200, codeDoc("owner-uid-1")) else HttpResponse(200, "{}") }
        val link = link(fake)
        link.check((link.start() as LinkStart.Created).code)
        link.markRevoked()
        assertEquals(OwnerLinkState.Revoked, link.state.value)
        assertNull(link.ownerUid)
        link.acknowledgeRevoked()
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
    }

    @Test
    fun `토큰은 헤더에만 있고 URL에 없다`() = runTest {
        val fake = FakeTransport { if (it.method == "GET") HttpResponse(200, codeDoc("owner-uid-1")) else HttpResponse(200, "{}") }
        val link = link(fake)
        val code = (link.start() as LinkStart.Created).code
        link.check(code)
        link.cancel(code)
        link.unlink()
        assertTrue(fake.requests.all { it.headers["Authorization"] == "Bearer token-1" })
        assertTrue(fake.requests.none { it.url.contains("token-1") })
    }

    @Test
    fun `로그에 코드와 uid와 토큰이 없다`() = runTest {
        val log = mutableListOf<String>()
        val fake = FakeTransport { if (it.method == "GET") HttpResponse(200, codeDoc("owner-uid-1")) else HttpResponse(200, "{}") }
        val link = link(fake, log)
        val code = (link.start() as LinkStart.Created).code
        link.check(code)
        link.unlink()
        val all = log.joinToString("\n")
        assertTrue(log.isNotEmpty())
        for (secret in listOf(code.value, code.display, "owner-uid-1", "car-uid-1", "token-1")) assertFalse(secret, all.contains(secret))
    }

    @Test
    fun `네트워크가 끊기면 실패이고 던지지 않는다`() = runTest {
        val fake = FakeTransport { throw IOException("끊김") }
        assertTrue(link(fake).start() is LinkStart.Failed)
    }
}
