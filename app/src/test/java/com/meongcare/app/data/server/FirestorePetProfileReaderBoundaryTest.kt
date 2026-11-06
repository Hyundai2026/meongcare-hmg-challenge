package com.meongcare.app.data.server

import com.meongcare.app.data.pet.LinkedProfileRead
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * docs/data/owner-link.md "차가 읽을 때" 표의 경계: 403만 연결 끊김, 404만 등록 안 됨, 그 밖은 모두 읽지 못함.
 * 계획 014 설계 7절: 로그에 반려견 이름·uid·토큰이 없다.
 */
class FirestorePetProfileReaderBoundaryTest {

    private val config = serverConfigOf("demo-meongcare", "", "10.0.2.2", isDebugBuild = true)!!
    private val docs = "http://10.0.2.2:8080/v1/projects/demo-meongcare/databases/(default)/documents"

    private class FakeTransport(
        private val signUp: () -> HttpResponse = { HttpResponse(200, """{"idToken": "token-1", "localId": "car-uid-1"}""") },
        private val profile: (HttpRequest) -> HttpResponse,
    ) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        override suspend fun send(request: HttpRequest): HttpResponse {
            requests += request
            return if (request.url.contains("accounts:signUp")) signUp() else profile(request)
        }
    }

    private val kongi = """{"fields": {
        "schemaVersion": {"integerValue": "1"}, "name": {"stringValue": "콩이"}, "breed": {"stringValue": "말티즈"},
        "sex": {"stringValue": "female"}, "birthDate": {"stringValue": "2020-03-01"}, "birthDateIsEstimated": {"booleanValue": false},
        "photoDataUrl": {"nullValue": null},
        "medical": {"mapValue": {"fields": {"conditions": {"arrayValue": {"values": [{"stringValue": "슬개골 탈구"}]}},
            "allergies": {"arrayValue": {"values": [{"stringValue": "닭고기"}]}}, "medications": {"arrayValue": {}}}}},
        "updatedAt": {"stringValue": "2026-11-06T09:00:00.000Z"}}}"""

    private fun reader(fake: FakeTransport, owner: () -> String? = { "owner-uid-1" }, log: MutableList<String> = mutableListOf()) =
        FirestorePetProfileReader(config, AnonymousAuth(config, fake), fake, ownerUid = owner, log = { log += it })

    private suspend fun readWith(status: Int, body: String = "{}") = reader(FakeTransport { HttpResponse(status, body) }).read()

    @Test
    fun `401은 연결 끊김이 아니라 읽지 못함이다`() = runTest {
        // 토큰 문제로 연결을 끊어 버리면 안 된다. 허가가 없어진 것(403)만 끊김이다
        assertEquals(LinkedProfileRead.Unavailable, readWith(401))
    }

    @Test
    fun `403과 404가 아닌 실패 상태는 모두 읽지 못함이다`() = runTest {
        for (status in listOf(301, 400, 408, 409, 429, 500, 502, 504)) {
            assertEquals("HTTP $status", LinkedProfileRead.Unavailable, readWith(status))
        }
    }

    @Test
    fun `200이어도 형식 버전이 1이 아니거나 몸통이 비면 읽지 못함이다`() = runTest {
        assertEquals(LinkedProfileRead.Unavailable, readWith(200, kongi.replace("\"integerValue\": \"1\"", "\"integerValue\": \"2\"")))
        assertEquals(LinkedProfileRead.Unavailable, readWith(200, ""))
        assertEquals(LinkedProfileRead.Unavailable, readWith(204, ""))
    }

    @Test
    fun `익명 로그인에 실패하면 읽지 못함이고 프로필을 요청하지 않는다`() = runTest {
        val fake = FakeTransport(signUp = { HttpResponse(500, "") }) { HttpResponse(200, kongi) }
        assertEquals(LinkedProfileRead.Unavailable, reader(fake).read())
        assertTrue(fake.requests.none { it.url.contains("/petProfiles/") })

        val offline = FakeTransport(signUp = { throw IOException("끊김") }) { HttpResponse(200, kongi) }
        assertEquals(LinkedProfileRead.Unavailable, reader(offline).read())
    }

    @Test
    fun `연결된 보호자가 바뀌면 바뀐 보호자의 문서를 읽는다`() = runTest {
        var owner = "owner-uid-1"
        val fake = FakeTransport { HttpResponse(200, kongi) }
        val reader = reader(fake, owner = { owner })
        reader.read()
        owner = "owner-uid-2"
        reader.read()
        val urls = fake.requests.filter { it.method == "GET" }.map { it.url }
        assertEquals(listOf("$docs/petProfiles/owner-uid-1", "$docs/petProfiles/owner-uid-2"), urls)
    }

    @Test
    fun `프로필 요청은 GET이고 몸통이 없으며 토큰은 URL에 없다`() = runTest {
        val fake = FakeTransport { HttpResponse(200, kongi) }
        reader(fake).read()
        val get = fake.requests.single { it.url.contains("/petProfiles/") }
        assertEquals("GET", get.method)
        assertEquals(null, get.body)
        assertFalse(get.url.contains("token-1"))
        assertEquals("Bearer token-1", get.headers["Authorization"])
    }

    @Test
    fun `어느 결과에서도 로그에 반려견 정보와 uid와 토큰이 없다`() = runTest {
        val log = mutableListOf<String>()
        reader(FakeTransport { HttpResponse(200, kongi) }, log = log).read()
        reader(FakeTransport { HttpResponse(404, """{"error": {"message": "owner-uid-1 not found"}}""") }, log = log).read()
        reader(FakeTransport { HttpResponse(401, "{}") }, log = log).read()
        reader(FakeTransport { throw IOException("owner-uid-1") }, log = log).read()
        reader(FakeTransport(signUp = { HttpResponse(500, "") }) { HttpResponse(200, kongi) }, log = log).read()
        reader(FakeTransport { HttpResponse(200, kongi) }, owner = { null }, log = log).read()
        assertEquals(6, log.size)
        val all = log.joinToString("\n")
        for (secret in listOf("콩이", "말티즈", "슬개골", "닭고기", "owner-uid-1", "car-uid-1", "token-1")) {
            assertFalse(secret, all.contains(secret))
        }
    }
}
