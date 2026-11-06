package com.meongcare.app.data.server

import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 013: 만료 전 익명 토큰 갱신 ([AnonymousAuth]). 가짜 HttpTransport와 손으로 움직이는 시계. */
class AnonymousAuthRefreshTest {

    private val config = serverConfigOf("demo-meongcare", "", "127.0.0.1", isDebugBuild = true)!!
    private val start = Instant.parse("2026-11-06T05:00:00Z")
    private var clock = start

    private class FakeTransport : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        var signUpCount = 0
        var refreshResponse: () -> HttpResponse = { HttpResponse(500, "{}") }

        override suspend fun send(request: HttpRequest): HttpResponse {
            requests += request
            return if (request.url.contains("accounts:signUp")) {
                signUpCount++
                HttpResponse(
                    200,
                    """{"idToken": "token-$signUpCount", "localId": "uid-$signUpCount", "refreshToken": "refresh/$signUpCount+=", "expiresIn": "3600"}""",
                )
            } else {
                refreshResponse()
            }
        }

        val refreshes get() = requests.filter { it.url.contains("securetoken") }
    }

    private fun refreshOk(token: String = "token-new", uid: String? = "uid-1", expires: Any = "\"3600\"") = {
        HttpResponse(
            200,
            """{"id_token": "$token", "refresh_token": "refresh-new", "expires_in": $expires${uid?.let { ", \"user_id\": \"$it\"" } ?: ""}}""",
        )
    }

    private fun auth(fake: FakeTransport, log: MutableList<String> = mutableListOf()) =
        AnonymousAuth(config, fake, now = { clock }, log = { log += it })

    /** 만료 5분 전 = 가입 + 55분 */
    private val refreshAt get() = start.plusSeconds(55 * 60)

    @Test
    fun `만료 5분 전이 되기 1ms 전에는 갱신하지 않는다`() = runTest {
        val fake = FakeTransport().apply { refreshResponse = refreshOk() }
        val auth = auth(fake)
        auth.session()
        clock = refreshAt.minusMillis(1)
        assertEquals(AuthSession("token-1", "uid-1"), auth.session())
        assertTrue(fake.refreshes.isEmpty())
    }

    @Test
    fun `만료 5분 전이 되면 갱신 토큰으로 새 토큰을 받는다`() = runTest {
        val fake = FakeTransport().apply { refreshResponse = refreshOk() }
        val auth = auth(fake)
        auth.session()
        clock = refreshAt
        assertEquals(AuthSession("token-new", "uid-1"), auth.session())
        val refresh = fake.refreshes.single()
        assertEquals("POST", refresh.method)
        assertEquals("application/x-www-form-urlencoded", refresh.headers["Content-Type"])
        assertEquals("grant_type=refresh_token&refresh_token=refresh%2F1%2B%3D", refresh.body)
        // 새 만료 시각(지금 + 3600초) 전에는 다시 갱신하지 않는다
        clock = refreshAt.plusSeconds(60)
        auth.session()
        assertEquals(1, fake.refreshes.size)
    }

    @Test
    fun `갱신해도 uid는 그대로다`() = runTest {
        // 응답에 user_id가 없어도 uid를 지킨다
        val fake = FakeTransport().apply { refreshResponse = refreshOk(uid = null) }
        val auth = auth(fake)
        auth.session()
        clock = refreshAt
        assertEquals("uid-1", auth.session()?.uid)
        assertEquals(1, fake.signUpCount)
    }

    @Test
    fun `갱신 응답의 uid가 다르면 실패로 본다`() = runTest {
        val fake = FakeTransport().apply { refreshResponse = refreshOk(uid = "uid-other") }
        val log = mutableListOf<String>()
        val auth = auth(fake, log)
        auth.session()
        clock = refreshAt
        assertEquals(AuthSession("token-1", "uid-1"), auth.session())
        assertEquals(listOf("익명 토큰 갱신: 실패 (형식 오류)"), log)
    }

    @Test
    fun `갱신에 실패해도 만료 전이면 지금 토큰을 쓴다`() = runTest {
        val fake = FakeTransport().apply { refreshResponse = { throw IOException("refused") } }
        val log = mutableListOf<String>()
        val auth = auth(fake, log)
        auth.session()
        clock = refreshAt
        assertEquals(AuthSession("token-1", "uid-1"), auth.session())
        assertEquals(listOf("익명 토큰 갱신: 실패 (네트워크)"), log)
    }

    @Test
    fun `만료 뒤 갱신에 실패하면 이번에는 null이고 다음에 다시 갱신한다`() = runTest {
        val fake = FakeTransport().apply { refreshResponse = { HttpResponse(503, "{}") } }
        val auth = auth(fake)
        auth.session()
        clock = start.plusSeconds(3600)
        assertNull(auth.session())
        fake.refreshResponse = refreshOk()
        assertEquals(AuthSession("token-new", "uid-1"), auth.session())
        assertEquals(2, fake.refreshes.size)
        assertEquals(1, fake.signUpCount)
    }

    @Test
    fun `갱신이 거부되면 새로 가입한다`() = runTest {
        val fake = FakeTransport().apply { refreshResponse = { HttpResponse(400, """{"error": {"message": "INVALID_REFRESH_TOKEN"}}""") } }
        val log = mutableListOf<String>()
        val auth = auth(fake, log)
        auth.session()
        clock = refreshAt
        assertEquals(AuthSession("token-2", "uid-2"), auth.session())
        assertEquals(2, fake.signUpCount)
        assertEquals(listOf("익명 토큰: 다시 가입 (갱신 거부)"), log)
    }

    @Test
    fun `만료 시각을 모르면 갱신하지 않는다`() = runTest {
        for (expires in listOf(null, "\"0\"", "\"86401\"", "\"한 시간\"")) {
            val fake = object : HttpTransport {
                var refreshes = 0
                override suspend fun send(request: HttpRequest): HttpResponse {
                    if (request.url.contains("securetoken")) refreshes++
                    val expiresField = expires?.let { ", \"expiresIn\": $it" } ?: ""
                    return HttpResponse(200, """{"idToken": "t", "localId": "u", "refreshToken": "r"$expiresField}""")
                }
            }
            clock = start
            val auth = AnonymousAuth(config, fake, now = { clock })
            auth.session()
            clock = start.plusSeconds(10 * 3600)
            assertEquals("expiresIn=$expires", AuthSession("t", "u"), auth.session())
            assertEquals("expiresIn=$expires", 0, fake.refreshes)
        }
    }

    @Test
    fun `동시에 불러도 갱신은 한 번이다`() = runTest {
        val fake = FakeTransport().apply { refreshResponse = refreshOk() }
        val auth = auth(fake)
        auth.session()
        clock = refreshAt
        val sessions = (1..5).map { async { auth.session() } }.awaitAll()
        assertTrue(sessions.all { it == AuthSession("token-new", "uid-1") })
        assertEquals(1, fake.refreshes.size)
    }

    @Test
    fun `갱신 토큰은 몸통에만 있고 URL에 없다`() = runTest {
        val fake = FakeTransport().apply { refreshResponse = refreshOk() }
        val auth = auth(fake)
        auth.session()
        clock = refreshAt
        auth.session()
        val refresh = fake.refreshes.single()
        assertEquals("http://127.0.0.1:9099/securetoken.googleapis.com/v1/token?key=demo-key", refresh.url)
        assertFalse(refresh.url.contains("refresh"))
    }

    @Test
    fun `로그에 토큰과 uid가 없다`() = runTest {
        val fake = FakeTransport().apply { refreshResponse = refreshOk() }
        val log = mutableListOf<String>()
        val auth = auth(fake, log)
        auth.session()
        clock = refreshAt
        auth.session()
        fake.refreshResponse = { HttpResponse(400, "{}") }
        clock = refreshAt.plusSeconds(3600)
        auth.session()
        assertTrue(log.isNotEmpty())
        for (line in log) {
            assertFalse(line, Regex("token|uid|refresh", RegexOption.IGNORE_CASE).containsMatchIn(line))
        }
    }
}
