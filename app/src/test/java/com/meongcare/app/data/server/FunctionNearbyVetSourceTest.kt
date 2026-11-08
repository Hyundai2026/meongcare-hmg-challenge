package com.meongcare.app.data.server

import com.meongcare.app.domain.hospital.NearbyVets
import com.meongcare.app.domain.location.CurrentLocation
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 계획 015 "설계 결정 6"의 `nearbyFunctionUrlOf`와 `FunctionNearbyVetSource`. */
class FunctionNearbyVetSourceTest {

    private val config = serverConfigOf("demo-meongcare", "", "10.0.2.2", isDebugBuild = true)!!
    private val url = nearbyFunctionUrlOf("demo-meongcare", "10.0.2.2", isDebugBuild = true)!!
    private val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")

    private class FakeTransport(val signUp: () -> HttpResponse, val function: () -> HttpResponse) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        override suspend fun send(request: HttpRequest): HttpResponse {
            requests += request
            return if (request.url.contains("accounts:signUp")) signUp() else function()
        }
    }

    private val signUpOk = { HttpResponse(200, """{"idToken": "token-1", "localId": "uid-1"}""") }

    private fun ok(vets: String) = { HttpResponse(200, """{"result": {"schemaVersion": 1, "radiusMeters": 5000, "vets": [$vets]}}""") }

    private fun source(fake: FakeTransport, log: MutableList<String> = mutableListOf()) =
        FunctionNearbyVetSource(url, AnonymousAuth(config, fake), fake, log = { log += it })

    @Test
    fun `함수 주소는 debug와 에뮬레이터 주소가 있을 때만 있다`() {
        assertEquals("http://10.0.2.2:5001/demo-meongcare/asia-northeast3/nearbyVets", url)
        assertEquals("http://127.0.0.1:5001/p/asia-northeast3/nearbyVets", nearbyFunctionUrlOf(" p ", " 127.0.0.1 ", true))
        assertEquals("http://[::1]:5001/p/asia-northeast3/nearbyVets", nearbyFunctionUrlOf("p", "::1", true))
        assertNull(nearbyFunctionUrlOf("p", "10.0.2.2", isDebugBuild = false))
        assertNull(nearbyFunctionUrlOf("", "10.0.2.2", true))
        assertNull(nearbyFunctionUrlOf("p", "  ", true))
    }

    @Test
    fun `callable 규약대로 POST하고 결과를 병원으로 바꾼다`() = runTest {
        val fake = FakeTransport(
            signUpOk,
            ok("""{"name": " 가상 동물병원 ", "phone": "031-000-0000", "latitude": 37.42, "longitude": 127.09, "address": "가상로 1"},
                  {"name": "전화 없음", "phone": null, "latitude": 37.41, "longitude": 127.1, "address": null}"""),
        )
        val log = mutableListOf<String>()
        val found = source(fake, log).search(here) as NearbyVets.Found

        val post = fake.requests.single { it.url == url }
        assertEquals("POST", post.method)
        assertEquals("Bearer token-1", post.headers["Authorization"])
        val data = JSONObject(post.body!!).getJSONObject("data")
        assertEquals(here.latitude, data.getDouble("latitude"), 0.0)
        assertEquals(here.longitude, data.getDouble("longitude"), 0.0)

        assertEquals(listOf("가상 동물병원", "전화 없음"), found.hospitals.map { it.name })
        val first = found.hospitals.first()
        assertEquals("031-000-0000", first.phoneNumber)
        assertEquals("가상로 1", first.destination.address)
        assertEquals(false, first.isSample)
        assertNull(first.id)
        assertNull(found.hospitals[1].phoneNumber)
        assertTrue(log.single().startsWith("근처 병원: 2곳"))
        assertTrue("로그에 이름·좌표가 없다", log.none { it.contains("가상") || it.contains("37.") })
    }

    @Test
    fun `틀린 항목만 뺀다`() = runTest {
        val fake = FakeTransport(
            signUpOk,
            ok("""{"name": "", "latitude": 37.4, "longitude": 127.0},
                  {"name": "${"가".repeat(41)}", "latitude": 37.4, "longitude": 127.0},
                  {"name": "범위 밖", "latitude": 91, "longitude": 127.0},
                  {"name": "문자열 좌표", "latitude": "37.4", "longitude": 127.0},
                  {"name": "좌표 없음"},
                  {"name": "좋음", "latitude": 37.4, "longitude": 127.0}"""),
        )
        val found = source(fake).search(here) as NearbyVets.Found
        assertEquals(listOf("좋음"), found.hospitals.map { it.name })
    }

    @Test
    fun `형식이 틀리면 실패다`() = runTest {
        listOf(
            """{"result": {"schemaVersion": 2, "vets": []}}""",
            """{"result": {"schemaVersion": 1}}""",
            """{"other": 1}""",
            "not json",
        ).forEach { body ->
            val result = source(FakeTransport(signUpOk) { HttpResponse(200, body) }).search(here)
            assertEquals(body, NearbyVets.Unavailable("형식 오류"), result)
        }
    }

    @Test
    fun `HTTP 오류와 네트워크 실패와 인증 실패는 실패다`() = runTest {
        listOf(401, 429, 500).forEach { status ->
            val body = """{"error": {"status": "X", "message": "m"}}"""
            assertEquals(NearbyVets.Unavailable("HTTP $status"), source(FakeTransport(signUpOk) { HttpResponse(status, body) }).search(here))
        }
        assertEquals(NearbyVets.Unavailable("네트워크"), source(FakeTransport(signUpOk) { throw IOException("x") }).search(here))

        val noAuth = FakeTransport({ HttpResponse(500, "") }) { error("함수를 부르지 않는다") }
        assertEquals(NearbyVets.Unavailable("인증 실패"), source(noAuth).search(here))
        assertTrue(noAuth.requests.none { it.method == "POST" && it.url == url })
    }
}
