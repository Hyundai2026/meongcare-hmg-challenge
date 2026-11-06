package com.meongcare.app.data.server

import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 013 익명 토큰 갱신 경계 ([AnonymousAuth], 계획 013 설계 결정 8 표) */
class AnonymousAuthRefreshBoundaryTest {

    private val config = serverConfigOf("demo-meongcare", "", "127.0.0.1", isDebugBuild = true)!!
    private val start = Instant.parse("2026-11-06T05:00:00Z")
    private var clock = start

    /**
     * 가입은 [signUpBody]로, 갱신은 [refreshResponse]로 답한다. 가입할 때마다 번호가 하나씩 오른다 (token-N, uid-N).
     */
    private class FakeTransport(
        val signUpBody: (n: Int) -> String = { n ->
            """{"idToken": "token-$n", "localId": "uid-$n", "refreshToken": "refresh-$n", "expiresIn": "3600"}"""
        },
    ) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        var signUpCount = 0
        var signUpFails = false
        var refreshResponse: (HttpRequest) -> HttpResponse = { HttpResponse(500, "{}") }

        override suspend fun send(request: HttpRequest): HttpResponse {
            requests += request
            return if (request.url.contains("accounts:signUp")) {
                if (signUpFails) throw IOException("refused")
                signUpCount++
                HttpResponse(200, signUpBody(signUpCount))
            } else {
                refreshResponse(request)
            }
        }

        val refreshes get() = requests.filter { it.url.contains("securetoken") }
    }

    private fun refreshOk(token: String, refreshToken: String? = "refresh-rotated", expires: String = "\"3600\"") = { _: HttpRequest ->
        HttpResponse(
            200,
            """{"id_token": "$token", "expires_in": $expires, "user_id": "uid-1"${refreshToken?.let { ", \"refresh_token\": \"$it\"" } ?: ""}}""",
        )
    }

    private fun auth(fake: HttpTransport, log: MutableList<String> = mutableListOf()) =
        AnonymousAuth(config, fake, now = { clock }, log = { log += it })

    private fun minutes(m: Long) = start.plusSeconds(m * 60)

    @Test
    fun `다음 갱신은 새로 받은 갱신 토큰을 쓴다`() = runTest {
        val fake = FakeTransport().apply { refreshResponse = refreshOk("token-a", refreshToken = "refresh-a") }
        val auth = auth(fake)
        auth.session()
        clock = minutes(55)
        assertEquals(AuthSession("token-a", "uid-1"), auth.session())
        fake.refreshResponse = refreshOk("token-b", refreshToken = "refresh-b")
        clock = minutes(110)
        assertEquals(AuthSession("token-b", "uid-1"), auth.session())
        assertEquals(
            listOf("grant_type=refresh_token&refresh_token=refresh-1", "grant_type=refresh_token&refresh_token=refresh-a"),
            fake.refreshes.map { it.body },
        )
    }

    @Test
    fun `갱신 응답에 갱신 토큰이 없으면 이전 갱신 토큰을 계속 쓴다`() = runTest {
        val fake = FakeTransport().apply { refreshResponse = refreshOk("token-a", refreshToken = null) }
        val auth = auth(fake)
        auth.session()
        clock = minutes(55)
        auth.session()
        fake.refreshResponse = refreshOk("token-b", refreshToken = null)
        clock = minutes(110)
        assertEquals(AuthSession("token-b", "uid-1"), auth.session())
        assertEquals(2, fake.refreshes.size)
        assertTrue(fake.refreshes.all { it.body == "grant_type=refresh_token&refresh_token=refresh-1" })
    }

    @Test
    fun `다음 갱신 시각은 갱신한 때부터 새 만료 시간으로 센다`() = runTest {
        val fake = FakeTransport().apply { refreshResponse = refreshOk("token-a") }
        val auth = auth(fake)
        auth.session()
        clock = minutes(55)
        auth.session()
        assertEquals(1, fake.refreshes.size)
        // 새 만료 = 55분 + 60분 = 115분 → 갱신은 110분부터
        clock = minutes(110).minusMillis(1)
        assertEquals(AuthSession("token-a", "uid-1"), auth.session())
        assertEquals(1, fake.refreshes.size)
        fake.refreshResponse = refreshOk("token-b")
        clock = minutes(110)
        assertEquals(AuthSession("token-b", "uid-1"), auth.session())
        assertEquals(2, fake.refreshes.size)
    }

    @Test
    fun `가입 응답의 만료 시간이 숫자여도 읽는다`() = runTest {
        val fake = FakeTransport(signUpBody = { n ->
            """{"idToken": "token-$n", "localId": "uid-$n", "refreshToken": "refresh-$n", "expiresIn": 3600}"""
        }).apply { refreshResponse = refreshOk("token-a") }
        val auth = auth(fake)
        auth.session()
        clock = minutes(55)
        assertEquals(AuthSession("token-a", "uid-1"), auth.session())
        assertEquals(1, fake.refreshes.size)
    }

    @Test
    fun `만료 시간은 1초와 86400초까지 인정하고 음수는 모름이다`() = runTest {
        // 86400초 (하루): 만료 5분 전에 갱신한다
        run {
            val fake = FakeTransport(signUpBody = { """{"idToken": "t", "localId": "uid-1", "refreshToken": "r", "expiresIn": "86400"}""" })
                .apply { refreshResponse = refreshOk("token-a") }
            clock = start
            val auth = auth(fake)
            auth.session()
            clock = start.plusSeconds(86_400 - 300).minusMillis(1)
            auth.session()
            assertEquals(0, fake.refreshes.size)
            clock = start.plusSeconds(86_400 - 300)
            assertEquals(AuthSession("token-a", "uid-1"), auth.session())
        }
        // 1초: 이미 만료 5분 전 안이라 다음 부를 때 갱신한다
        run {
            val fake = FakeTransport(signUpBody = { """{"idToken": "t", "localId": "uid-1", "refreshToken": "r", "expiresIn": "1"}""" })
                .apply { refreshResponse = refreshOk("token-a") }
            clock = start
            val auth = auth(fake)
            auth.session()
            assertEquals(AuthSession("token-a", "uid-1"), auth.session())
            assertEquals(1, fake.refreshes.size)
        }
        // 음수: 모름 → 갱신하지 않는다
        run {
            val fake = FakeTransport(signUpBody = { """{"idToken": "t", "localId": "uid-1", "refreshToken": "r", "expiresIn": "-1"}""" })
            clock = start
            val auth = auth(fake)
            auth.session()
            clock = start.plusSeconds(10 * 3600)
            assertEquals(AuthSession("t", "uid-1"), auth.session())
            assertEquals(0, fake.refreshes.size)
        }
    }

    @Test
    fun `갱신 토큰 없이 만료 전이면 지금 토큰을 쓰고 아무것도 보내지 않는다`() = runTest {
        val fake = FakeTransport(signUpBody = { n -> """{"idToken": "token-$n", "localId": "uid-$n", "expiresIn": "3600"}""" })
        val auth = auth(fake)
        auth.session()
        clock = minutes(56)
        assertEquals(AuthSession("token-1", "uid-1"), auth.session())
        assertEquals(1, fake.requests.size)
    }

    @Test
    fun `갱신 토큰 없이 만료되면 새로 가입한다`() = runTest {
        val fake = FakeTransport(signUpBody = { n -> """{"idToken": "token-$n", "localId": "uid-$n", "expiresIn": "3600"}""" })
        val auth = auth(fake)
        auth.session()
        clock = minutes(60)
        assertEquals(AuthSession("token-2", "uid-2"), auth.session())
        assertEquals(0, fake.refreshes.size)
    }

    @Test
    fun `갱신 응답에 id_token이 없거나 JSON이 아니면 형식 오류이고 만료 전이면 지금 토큰을 쓴다`() = runTest {
        for (body in listOf("""{"expires_in": "3600"}""", "<html>", """{"id_token": ""}""")) {
            clock = start
            val fake = FakeTransport().apply { refreshResponse = { HttpResponse(200, body) } }
            val log = mutableListOf<String>()
            val auth = auth(fake, log)
            auth.session()
            clock = minutes(55)
            assertEquals(body, AuthSession("token-1", "uid-1"), auth.session())
            assertEquals(body, listOf("익명 토큰 갱신: 실패 (형식 오류)"), log)
            assertEquals(body, 1, fake.signUpCount)
        }
    }

    @Test
    fun `갱신이 401이나 403이면 거부가 아니라 실패로 보고 다시 가입하지 않는다`() = runTest {
        for (status in listOf(401, 403)) {
            clock = start
            val fake = FakeTransport().apply { refreshResponse = { HttpResponse(status, "{}") } }
            val log = mutableListOf<String>()
            val auth = auth(fake, log)
            auth.session()
            clock = minutes(55)
            assertEquals("$status", AuthSession("token-1", "uid-1"), auth.session())
            assertEquals("$status", 1, fake.signUpCount)
            assertEquals("$status", listOf("익명 토큰 갱신: 실패 (HTTP $status)"), log)
        }
    }

    @Test
    fun `만료 1ms 전에는 갱신에 실패해도 지금 토큰을 쓴다`() = runTest {
        val fake = FakeTransport().apply { refreshResponse = { throw IOException("reset") } }
        val auth = auth(fake)
        auth.session()
        clock = minutes(60).minusMillis(1)
        assertEquals(AuthSession("token-1", "uid-1"), auth.session())
        clock = minutes(61)
        assertNull(auth.session())
        assertEquals(1, fake.signUpCount)
    }

    @Test
    fun `만료 뒤 갱신이 거부되면 새로 가입한 토큰을 쓴다`() = runTest {
        val fake = FakeTransport().apply { refreshResponse = { HttpResponse(400, """{"error": {"message": "INVALID_REFRESH_TOKEN"}}""") } }
        val auth = auth(fake)
        auth.session()
        clock = minutes(90)
        assertEquals(AuthSession("token-2", "uid-2"), auth.session())
    }

    @Test
    fun `갱신 거부 뒤 다시 가입도 실패하면 null이고 다음에 부르면 다시 가입한다`() = runTest {
        val fake = FakeTransport().apply { refreshResponse = { HttpResponse(400, "{}") } }
        val auth = auth(fake)
        auth.session()
        clock = minutes(55)
        fake.signUpFails = true
        assertNull(auth.session())
        fake.signUpFails = false
        assertEquals(AuthSession("token-2", "uid-2"), auth.session())
        // 새 자격으로 시작했으니 갱신 요청은 처음 한 번뿐이다
        assertEquals(1, fake.refreshes.size)
    }

    @Test
    fun `동시에 불러도 갱신 거부 뒤 다시 가입은 한 번이다`() = runTest {
        val fake = FakeTransport().apply { refreshResponse = { HttpResponse(400, "{}") } }
        val auth = auth(fake)
        auth.session()
        clock = minutes(58)
        val sessions = (1..5).map { async { auth.session() } }.awaitAll()
        assertTrue(sessions.all { it == AuthSession("token-2", "uid-2") })
        assertEquals(2, fake.signUpCount)
        assertEquals(1, fake.refreshes.size)
    }

    @Test
    fun `갱신 토큰의 공백과 기호와 한글은 모두 퍼센트 인코딩한다`() = runTest {
        val fake = FakeTransport(signUpBody = { n ->
            """{"idToken": "token-$n", "localId": "uid-$n", "refreshToken": "a b&c=d?e#f%g~h.i_j-k가", "expiresIn": "3600"}"""
        }).apply { refreshResponse = refreshOk("token-a") }
        val auth = auth(fake)
        auth.session()
        clock = minutes(55)
        auth.session()
        assertEquals(
            "grant_type=refresh_token&refresh_token=a%20b%26c%3Dd%3Fe%23f%25g~h.i_j-k%EA%B0%80",
            fake.refreshes.single().body,
        )
        assertTrue(fake.refreshes.single().url.endsWith("?key=demo-key"))
    }
}
