package com.meongcare.app.data.server

import com.meongcare.app.domain.link.LinkCheck
import com.meongcare.app.domain.link.LinkCode
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

/**
 * docs/data/owner-link.md "연결 코드"·"연결과 해제", 계획 014 설계 5절·K5·U2, OwnerLinkService KDoc의 경계.
 * 가짜 HttpTransport로 JVM에서 돈다.
 */
class FirestoreOwnerLinkBoundaryTest {

    private val config = serverConfigOf("demo-meongcare", "", "10.0.2.2", isDebugBuild = true)!!
    private val at = Instant.parse("2026-11-06T09:00:00Z")
    private val docs = "http://10.0.2.2:8080/v1/projects/demo-meongcare/databases/(default)/documents"

    private class FakeTransport(
        var signUp: () -> HttpResponse = { HttpResponse(200, """{"idToken": "token-1", "localId": "car-uid-1"}""") },
        val handler: (HttpRequest) -> HttpResponse,
    ) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        override suspend fun send(request: HttpRequest): HttpResponse {
            if (request.url.contains("accounts:signUp")) return signUp()
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

    private fun claimedBy(owner: () -> String?) = FakeTransport { if (it.method == "GET") HttpResponse(200, codeDoc(owner())) else HttpResponse(200, "{}") }

    private fun writeOf(request: HttpRequest): JSONObject = JSONObject(request.body!!).getJSONArray("writes").getJSONObject(0)

    private fun codeOf(request: HttpRequest): String = writeOf(request).getJSONObject("update").getString("name").substringAfterLast('/')

    private suspend fun FirestoreOwnerLink.linkTo(): LinkCode {
        val code = (start() as LinkStart.Created).code
        assertEquals(LinkCheck.Linked, check(code))
        return code
    }

    // ---- 코드 만들기 ----

    @Test
    fun `겹친 뒤 받은 코드는 서버가 마지막에 받아들인 코드다`() = runTest {
        var calls = 0
        val fake = FakeTransport { calls++; if (calls < 3) HttpResponse(409, "{}") else HttpResponse(200, "{}") }
        val created = link(fake).start() as LinkStart.Created
        assertEquals(codeOf(fake.requests.last()), created.code.value)
    }

    @Test
    fun `다시 만들 때마다 새 문서로만 서버 시각과 빈 보호자로 만든다`() = runTest {
        val fake = FakeTransport { HttpResponse(409, "{}") }
        link(fake).start()
        assertEquals(3, fake.requests.size)
        for (request in fake.requests) {
            val write = writeOf(request)
            assertFalse(write.getJSONObject("currentDocument").getBoolean("exists"))
            assertEquals("REQUEST_TIME", write.getJSONArray("updateTransforms").getJSONObject(0).getString("setToServerValue"))
            val fields = write.getJSONObject("update").getJSONObject("fields")
            assertTrue(fields.getJSONObject("ownerUid").has("nullValue"))
            assertEquals("car-uid-1", fields.getJSONObject("carUid").getString("stringValue"))
            assertTrue(LinkCode.of(codeOf(request)) != null)
        }
    }

    @Test
    fun `겹침이 아닌 실패는 다시 만들지 않는다`() = runTest {
        for (status in listOf(400, 401, 403, 429, 500, 503)) {
            val fake = FakeTransport { HttpResponse(status, "{}") }
            assertTrue("HTTP $status", link(fake).start() is LinkStart.Failed)
            assertEquals("HTTP $status", 1, fake.requests.size)
        }
    }

    @Test
    fun `겹친 뒤 네트워크가 끊기면 실패이고 더 만들지 않는다`() = runTest {
        var calls = 0
        val fake = FakeTransport { calls++; if (calls == 1) HttpResponse(409, "{}") else throw IOException("끊김") }
        assertTrue(link(fake).start() is LinkStart.Failed)
        assertEquals(2, fake.requests.size)
    }

    @Test
    fun `익명 로그인에 실패하면 코드를 만들지 않고 실패다`() = runTest {
        val fake = FakeTransport(signUp = { HttpResponse(500, "") }) { HttpResponse(200, "{}") }
        assertTrue(link(fake).start() is LinkStart.Failed)
        assertTrue(fake.requests.isEmpty())
    }

    // ---- 확인 ----

    @Test
    fun `확인은 자기 코드 문서 한 건만 GET으로 읽는다`() = runTest {
        val fake = claimedBy { null }
        val link = link(fake)
        val code = (link.start() as LinkStart.Created).code
        link.check(code)
        val get = fake.requests.last()
        assertEquals("GET", get.method)
        assertEquals("$docs/linkCodes/${code.value}", get.url)
        assertNull(get.body)
    }

    @Test
    fun `서버 오류와 토큰 오류는 코드 없음이 아니라 실패이고 연결 상태는 그대로다`() = runTest {
        // 코드 없음(Gone)이면 화면이 만료를 보이므로, 잠깐의 서버 오류를 만료로 보이면 안 된다
        for (status in listOf(400, 401, 429, 500, 503)) {
            val fake = FakeTransport { if (it.method == "GET") HttpResponse(status, "{}") else HttpResponse(200, "{}") }
            val link = link(fake)
            val code = (link.start() as LinkStart.Created).code
            assertTrue("HTTP $status", link.check(code) is LinkCheck.Failed)
            assertEquals(OwnerLinkState.Unlinked, link.state.value)
        }
    }

    @Test
    fun `확인 중 네트워크가 끊기면 실패이고 던지지 않는다`() = runTest {
        val fake = FakeTransport { if (it.method == "GET") throw IOException("끊김") else HttpResponse(200, "{}") }
        val link = link(fake)
        val code = (link.start() as LinkStart.Created).code
        assertTrue(link.check(code) is LinkCheck.Failed)
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
    }

    @Test
    fun `보호자 uid가 비었거나 형식이 틀린 코드 문서로는 연결하지 않는다`() = runTest {
        val bodies = listOf(
            codeDoc(""),
            """{"fields": {"ownerUid": {"integerValue": "3"}}}""",
            """{"fields": {"carUid": {"stringValue": "car-uid-1"}}}""",
            """{"fields": {}}""",
            "not json",
            "",
        )
        for (body in bodies) {
            val fake = FakeTransport { if (it.method == "GET") HttpResponse(200, body) else HttpResponse(200, "{}") }
            val link = link(fake)
            val code = (link.start() as LinkStart.Created).code
            val result = link.check(code)
            assertFalse(body, result == LinkCheck.Linked)
            assertEquals(body, OwnerLinkState.Unlinked, link.state.value)
            assertNull(body, link.ownerUid)
        }
    }

    @Test
    fun `쓴 코드를 지우지 못해도 연결됨이고 던지지 않는다`() = runTest {
        val fake = FakeTransport {
            when (it.method) {
                "GET" -> HttpResponse(200, codeDoc("owner-uid-1"))
                "DELETE" -> throw IOException("끊김")
                else -> HttpResponse(200, "{}")
            }
        }
        val link = link(fake)
        link.linkTo()
        assertEquals(OwnerLinkState.Linked(at), link.state.value)
        assertEquals("owner-uid-1", link.ownerUid)
    }

    @Test
    fun `연결된 뒤 지운 코드를 다시 물으면 코드 없음이고 연결은 그대로다`() = runTest {
        var deleted = false
        val fake = FakeTransport {
            when {
                it.method == "DELETE" -> { deleted = true; HttpResponse(200, "{}") }
                it.method == "GET" && deleted -> HttpResponse(404, "{}")
                it.method == "GET" -> HttpResponse(200, codeDoc("owner-uid-1"))
                else -> HttpResponse(200, "{}")
            }
        }
        val link = link(fake)
        val code = link.linkTo()
        assertEquals(LinkCheck.Gone, link.check(code))
        assertEquals(OwnerLinkState.Linked(at), link.state.value)
        assertEquals("owner-uid-1", link.ownerUid)
    }

    // ---- 한 차 = 보호자 한 명 (K5) ----

    @Test
    fun `같은 보호자와 다시 연결하면 그 허가는 지우지 않는다`() = runTest {
        // 보호자 웹이 같은 허가 문서를 다시 썼다 (R19b). 지우면 방금 한 연결이 끊긴다
        val fake = claimedBy { "owner-uid-1" }
        val link = link(fake)
        link.linkTo()
        link.linkTo()
        assertTrue(fake.requests.none { it.method == "DELETE" && it.url.contains("/readers/") })
        assertEquals("owner-uid-1", link.ownerUid)
    }

    @Test
    fun `보호자가 끊은 뒤 다른 보호자와 연결하면 이전 허가를 지우려 하지 않는다`() = runTest {
        var owner = "owner-uid-1"
        val fake = claimedBy { owner }
        val link = link(fake)
        link.linkTo()
        link.markRevoked()
        link.acknowledgeRevoked()
        owner = "owner-uid-2"
        link.linkTo()
        assertTrue(fake.requests.none { it.method == "DELETE" && it.url.contains("/readers/") })
        assertEquals("owner-uid-2", link.ownerUid)
    }

    @Test
    fun `다른 보호자와 새로 연결하면 이전 보호자의 허가 문서만 지운다`() = runTest {
        var owner = "owner-uid-1"
        val fake = claimedBy { owner }
        val link = link(fake)
        link.linkTo()
        owner = "owner-uid-2"
        link.linkTo()
        val readerDeletes = fake.requests.filter { it.method == "DELETE" && it.url.contains("/readers/") }.map { it.url }
        assertEquals(listOf("$docs/petProfiles/owner-uid-1/readers/car-uid-1"), readerDeletes)
    }

    @Test
    fun `새 코드를 기다리는 동안에는 이전 보호자와 연결이 그대로다`() = runTest {
        var owner: String? = "owner-uid-1"
        val fake = claimedBy { owner }
        val link = link(fake)
        link.linkTo()
        owner = null
        val code = (link.start() as LinkStart.Created).code
        assertEquals(LinkCheck.Waiting, link.check(code))
        assertEquals(OwnerLinkState.Linked(at), link.state.value)
        assertEquals("owner-uid-1", link.ownerUid)
        assertTrue(fake.requests.none { it.method == "DELETE" && it.url.contains("/readers/") })
    }

    // ---- 끊기·버리기 ----

    @Test
    fun `연결 전에 끊으면 서버에 요청하지 않는다`() = runTest {
        val fake = claimedBy { "owner-uid-1" }
        val link = link(fake)
        link.unlink()
        assertTrue(fake.requests.isEmpty())
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
    }

    @Test
    fun `보호자가 끊은 상태에서 차가 끊어도 연결 전이고 보호자를 잊는다`() = runTest {
        val fake = claimedBy { "owner-uid-1" }
        val link = link(fake)
        link.linkTo()
        link.markRevoked()
        link.unlink()
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
        assertNull(link.ownerUid)
    }

    @Test
    fun `코드를 버리면 그 코드 문서를 지우고 실패해도 던지지 않는다`() = runTest {
        var fail = false
        val fake = FakeTransport { if (it.method == "DELETE" && fail) throw IOException("끊김") else HttpResponse(200, codeDoc(null)) }
        val link = link(fake)
        val code = (link.start() as LinkStart.Created).code
        link.cancel(code)
        val delete = fake.requests.last()
        assertEquals("DELETE", delete.method)
        assertEquals("$docs/linkCodes/${code.value}", delete.url)
        fail = true
        link.cancel(code)
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
    }

    @Test
    fun `끊김 표시와 확인은 해당 상태에서만 바뀐다`() = runTest {
        val link = link(claimedBy { "owner-uid-1" })
        link.markRevoked()
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
        link.linkTo()
        link.acknowledgeRevoked()
        assertEquals(OwnerLinkState.Linked(at), link.state.value)
        link.markRevoked()
        link.markRevoked()
        assertEquals(OwnerLinkState.Revoked, link.state.value)
        assertNull(link.ownerUid)
    }

    @Test
    fun `겹침·실패·지우기 로그에도 코드와 uid와 토큰이 없다`() = runTest {
        val log = mutableListOf<String>()
        var commits = 0
        var getStatus = 500
        val fake = FakeTransport {
            when (it.method) {
                "POST" -> { commits++; if (commits == 1) HttpResponse(409, "{}") else HttpResponse(200, "{}") }
                "GET" -> if (getStatus == 200) HttpResponse(200, codeDoc("owner-uid-1")) else HttpResponse(getStatus, "{}")
                else -> throw IOException("끊김")
            }
        }
        val link = link(fake, log)
        val code = (link.start() as LinkStart.Created).code
        link.check(code) // 500
        getStatus = 403
        link.check(code) // 코드 없음
        link.cancel(code) // 네트워크 실패
        getStatus = 200
        link.check(code) // 연결됨, 코드 지우기 실패
        link.unlink() // 허가 지우기 실패
        assertTrue(log.size >= 4)
        val all = log.joinToString("\n")
        for (secret in listOf(code.value, code.display, "owner-uid-1", "car-uid-1", "token-1")) assertFalse(secret, all.contains(secret))
    }
}
