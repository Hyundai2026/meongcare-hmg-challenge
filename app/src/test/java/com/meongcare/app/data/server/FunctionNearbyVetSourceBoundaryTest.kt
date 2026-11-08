package com.meongcare.app.data.server

import com.meongcare.app.domain.hospital.NearbyVets
import com.meongcare.app.domain.location.CurrentLocation
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** 계획 015 "설계 결정 6" `FunctionNearbyVetSource`·`nearbyFunctionUrlOf`의 경계 (tester). */
class FunctionNearbyVetSourceBoundaryTest {

    private val config = serverConfigOf("demo-meongcare", "", "127.0.0.1", isDebugBuild = true)!!
    private val url = nearbyFunctionUrlOf("demo-meongcare", "127.0.0.1", isDebugBuild = true)!!
    private val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")

    private class FakeTransport(val signUp: () -> HttpResponse, val function: () -> HttpResponse) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        override suspend fun send(request: HttpRequest): HttpResponse {
            requests += request
            return if (request.url.contains("accounts:signUp")) signUp() else function()
        }
        val functionCalls get() = requests.count { !it.url.contains("accounts:signUp") }
    }

    private val signUpOk = { HttpResponse(200, """{"idToken": "secret-token-xyz", "localId": "uid-secret"}""") }

    private fun result(vets: String, schema: String = "1") =
        HttpResponse(200, """{"result": {"schemaVersion": $schema, "radiusMeters": 5000, "vets": [$vets]}}""")

    private fun source(fake: FakeTransport, log: MutableList<String> = mutableListOf()) =
        FunctionNearbyVetSource(url, AnonymousAuth(config, fake), fake, log = { log += it })

    private suspend fun names(vets: String): List<String> {
        val found = source(FakeTransport(signUpOk) { result(vets) }).search(here)
        return (found as NearbyVets.Found).hospitals.map { it.name }
    }

    @Test
    fun `요청 몸통은 data 안의 위도와 경도뿐이고 반경이나 개수를 보내지 않는다`() = runTest {
        val fake = FakeTransport(signUpOk) { result("") }
        source(fake).search(here)
        val post = fake.requests.single { it.url == url }
        val root = JSONObject(post.body!!)
        assertEquals(setOf("data"), root.keys().asSequence().toSet())
        assertEquals(setOf("latitude", "longitude"), root.getJSONObject("data").keys().asSequence().toSet())
        assertTrue(post.headers["Content-Type"].orEmpty().startsWith("application/json"))
    }

    @Test
    fun `빈 vets 배열은 0곳을 찾은 것이다`() = runTest {
        assertEquals(NearbyVets.Found(emptyList()), source(FakeTransport(signUpOk) { result("") }).search(here))
    }

    @Test
    fun `이름은 40자까지 받고 41자는 뺀다`() = runTest {
        val forty = "가".repeat(40)
        val fortyOne = "가".repeat(41)
        assertEquals(
            listOf(forty),
            names("""{"name": "$forty", "latitude": 37.4, "longitude": 127.0},
                     {"name": "$fortyOne", "latitude": 37.4, "longitude": 127.0}"""),
        )
    }

    @Test
    fun `공백뿐인 이름과 문자열이 아닌 이름은 뺀다`() = runTest {
        assertEquals(
            listOf("좋음"),
            names("""{"name": "   ", "latitude": 37.4, "longitude": 127.0},
                     {"name": 12, "latitude": 37.4, "longitude": 127.0},
                     {"name": null, "latitude": 37.4, "longitude": 127.0},
                     {"name": "좋음", "latitude": 37.4, "longitude": 127.0}"""),
        )
    }

    @Test
    fun `좌표 범위의 끝값은 받고 바로 밖은 뺀다`() = runTest {
        assertEquals(
            listOf("남극", "북극", "날짜변경선 동", "날짜변경선 서"),
            names("""{"name": "남극", "latitude": -90, "longitude": 0},
                     {"name": "북극", "latitude": 90, "longitude": 0},
                     {"name": "날짜변경선 동", "latitude": 0, "longitude": 180},
                     {"name": "날짜변경선 서", "latitude": 0, "longitude": -180},
                     {"name": "위도 밖", "latitude": -90.0001, "longitude": 0},
                     {"name": "경도 밖", "latitude": 0, "longitude": 180.0001},
                     {"name": "경도 없음", "latitude": 0},
                     {"name": "경도 null", "latitude": 0, "longitude": null}"""),
        )
    }

    @Test
    fun `공백뿐인 전화는 없는 것으로 본다`() = runTest {
        val found = source(FakeTransport(signUpOk) {
            result("""{"name": "가", "phone": "   ", "latitude": 37.4, "longitude": 127.0}""")
        }).search(here) as NearbyVets.Found
        assertNull(found.hospitals.single().phoneNumber)
    }

    @Test
    fun `vets 안의 객체가 아닌 항목은 그것만 뺀다`() = runTest {
        assertEquals(
            listOf("좋음"),
            names(""""문자열", 1, null, [], {"name": "좋음", "latitude": 37.4, "longitude": 127.0}"""),
        )
    }

    @Test
    fun `모르는 필드는 무시한다`() = runTest {
        val body = """{"result": {"schemaVersion": 1, "radiusMeters": 5000, "extra": true,
            "vets": [{"name": "가", "latitude": 37.4, "longitude": 127.0, "rating": 5}]}}"""
        val found = source(FakeTransport(signUpOk) { HttpResponse(200, body) }).search(here)
        assertEquals(listOf("가"), (found as NearbyVets.Found).hospitals.map { it.name })
    }

    @Test
    fun `schemaVersion이 1이 아닌 수이거나 null이거나 참거짓이면 형식 오류다`() = runTest {
        listOf("0", "2", "-1", "null", "true").forEach { schema ->
            val got = source(FakeTransport(signUpOk) { result("", schema = schema) }).search(here)
            assertEquals("schemaVersion=$schema", NearbyVets.Unavailable("형식 오류"), got)
        }
    }

    @Test
    fun `schemaVersion이 문자열 1이나 소수 1점5면 짐작하지 않고 형식 오류다`() = runTest {
        listOf("\"1\"", "1.5").forEach { schema ->
            val got = source(FakeTransport(signUpOk) { result("", schema = schema) }).search(here)
            assertEquals("schemaVersion=$schema", NearbyVets.Unavailable("형식 오류"), got)
        }
    }

    @Test
    fun `vets가 배열이 아니거나 result가 객체가 아니면 형식 오류다`() = runTest {
        listOf(
            """{"result": {"schemaVersion": 1, "vets": {}}}""",
            """{"result": {"schemaVersion": 1, "vets": "x"}}""",
            """{"result": []}""",
            """{"result": null}""",
            "[]",
            "",
        ).forEach { body ->
            val got = source(FakeTransport(signUpOk) { HttpResponse(200, body) }).search(here)
            assertEquals(body, NearbyVets.Unavailable("형식 오류"), got)
        }
    }

    @Test
    fun `200이어도 callable error 몸통이면 찾지 못한 것이다`() = runTest {
        val got = source(FakeTransport(signUpOk) {
            HttpResponse(200, """{"error": {"status": "INTERNAL", "message": "m"}}""")
        }).search(here)
        assertTrue(got.toString(), got is NearbyVets.Unavailable)
    }

    @Test
    fun `HTTP 403과 400과 503도 상태 번호로 실패한다`() = runTest {
        listOf(400, 403, 503).forEach { status ->
            val got = source(FakeTransport(signUpOk) { HttpResponse(status, "") }).search(here)
            assertEquals(NearbyVets.Unavailable("HTTP $status"), got)
        }
    }

    @Test
    fun `익명 가입이 네트워크로 실패해도 예외 없이 실패를 돌려주고 함수를 부르지 않는다`() = runTest {
        val fake = FakeTransport({ throw IOException("down") }) { error("부르지 않는다") }
        val got = source(fake).search(here)
        assertTrue(got.toString(), got is NearbyVets.Unavailable)
        assertEquals(0, fake.functionCalls)
    }

    @Test
    fun `transport가 IOException이 아닌 예외를 던져도 밖으로 던지지 않는다`() = runTest {
        val got = source(FakeTransport(signUpOk) { throw IllegalStateException("x") }).search(here)
        assertTrue(got.toString(), got is NearbyVets.Unavailable)
    }

    @Test
    fun `취소는 실패로 바꾸지 않고 다시 던진다`() = runTest {
        val fake = FakeTransport(signUpOk) { throw CancellationException("취소") }
        try {
            source(fake).search(here)
            fail("취소가 밖으로 나와야 한다")
        } catch (_: CancellationException) {
            // 기대한 대로
        }
    }

    @Test
    fun `실패 로그에는 이유가 있고 토큰과 uid와 좌표는 없다`() = runTest {
        val log = mutableListOf<String>()
        source(FakeTransport(signUpOk) { HttpResponse(429, """{"error": {"status": "RESOURCE_EXHAUSTED"}}""") }, log).search(here)
        val line = log.single()
        assertTrue(line, line.startsWith("근처 병원: 실패"))
        assertTrue(line, line.contains("HTTP 429"))
        listOf("secret-token-xyz", "uid-secret", "37.41", "127.09", "시흥동").forEach {
            assertTrue("로그에 $it 없음: $line", !line.contains(it))
        }
    }

    @Test
    fun `성공 로그에는 개수만 있고 병원 이름과 전화와 주소는 없다`() = runTest {
        val log = mutableListOf<String>()
        source(FakeTransport(signUpOk) {
            result("""{"name": "비밀 동물병원", "phone": "031-123-4567", "latitude": 37.4, "longitude": 127.0, "address": "비밀로 1"}""")
        }, log).search(here)
        val line = log.single()
        assertTrue(line, line.startsWith("근처 병원: 1곳"))
        listOf("비밀", "031-123-4567", "secret-token-xyz").forEach { assertTrue(line, !line.contains(it)) }
    }

    @Test
    fun `부를 때마다 새로 찾는다`() = runTest {
        val fake = FakeTransport(signUpOk) { result("") }
        val s = source(fake)
        s.search(here)
        s.search(here)
        assertEquals(2, fake.functionCalls)
    }

    @Test
    fun `함수 주소는 이미 괄호로 감싼 IPv6를 다시 감싸지 않고 localhost도 받는다`() {
        assertEquals("http://[::1]:5001/p/asia-northeast3/nearbyVets", nearbyFunctionUrlOf("p", "[::1]", true))
        assertEquals("http://localhost:5001/p/asia-northeast3/nearbyVets", nearbyFunctionUrlOf("p", "localhost", true))
        assertNull(nearbyFunctionUrlOf("  ", "127.0.0.1", true))
        assertNull(nearbyFunctionUrlOf("p", "", isDebugBuild = false))
    }
}
