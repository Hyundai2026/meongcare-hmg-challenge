package com.meongcare.app.data.server

import com.meongcare.app.data.pet.LinkedProfileRead
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 계획 014 단계 8-1. 가짜 HttpTransport로 JVM에서 돈다 */
class FirestorePetProfileReaderTest {

    private val config = serverConfigOf("demo-meongcare", "", "10.0.2.2", isDebugBuild = true)!!

    private class FakeTransport(private val profile: () -> HttpResponse) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        override suspend fun send(request: HttpRequest): HttpResponse {
            requests += request
            return if (request.url.contains("accounts:signUp")) {
                HttpResponse(200, """{"idToken": "token-1", "localId": "car-uid-1"}""")
            } else {
                profile()
            }
        }
    }

    private val kongi = """{"name": "projects/p/databases/(default)/documents/petProfiles/owner-uid-1", "fields": {
        "schemaVersion": {"integerValue": "1"}, "name": {"stringValue": "콩이"}, "breed": {"stringValue": "말티즈"},
        "sex": {"stringValue": "female"}, "birthDate": {"stringValue": "2020-03-01"}, "birthDateIsEstimated": {"booleanValue": false},
        "photoDataUrl": {"nullValue": null},
        "medical": {"mapValue": {"fields": {"conditions": {"arrayValue": {"values": [{"stringValue": "슬개골 탈구"}]}},
            "allergies": {"arrayValue": {"values": [{"stringValue": "닭고기"}]}}, "medications": {"arrayValue": {}}}}},
        "updatedAt": {"stringValue": "2026-11-06T09:00:00.000Z"}}}"""

    private fun reader(fake: FakeTransport, owner: String? = "owner-uid-1", log: MutableList<String> = mutableListOf()) =
        FirestorePetProfileReader(config, AnonymousAuth(config, fake), fake, ownerUid = { owner }, log = { log += it })

    @Test
    fun `연결된 보호자의 프로필 문서를 Bearer 토큰으로 읽는다`() = runTest {
        val fake = FakeTransport { HttpResponse(200, kongi) }
        val read = reader(fake).read() as LinkedProfileRead.Loaded
        assertEquals("콩이", read.profile.name)
        assertEquals(listOf("닭고기"), read.profile.medical.allergies)
        val get = fake.requests.single { it.method == "GET" }
        assertEquals("http://10.0.2.2:8080/v1/projects/demo-meongcare/databases/(default)/documents/petProfiles/owner-uid-1", get.url)
        assertEquals("Bearer token-1", get.headers["Authorization"])
    }

    @Test
    fun `403은 연결 끊김이다`() = runTest {
        assertEquals(LinkedProfileRead.Revoked, reader(FakeTransport { HttpResponse(403, "{}") }).read())
    }

    @Test
    fun `404는 등록 안 됨이다`() = runTest {
        assertEquals(LinkedProfileRead.NotRegistered, reader(FakeTransport { HttpResponse(404, "{}") }).read())
    }

    @Test
    fun `네트워크 오류와 5xx와 형식 오류는 읽지 못함이다`() = runTest {
        assertEquals(LinkedProfileRead.Unavailable, reader(FakeTransport { throw IOException("끊김") }).read())
        assertEquals(LinkedProfileRead.Unavailable, reader(FakeTransport { HttpResponse(503, "") }).read())
        assertEquals(LinkedProfileRead.Unavailable, reader(FakeTransport { HttpResponse(200, """{"fields": {}}""") }).read())
    }

    @Test
    fun `연결된 보호자가 없으면 요청하지 않는다`() = runTest {
        val fake = FakeTransport { HttpResponse(200, kongi) }
        assertEquals(LinkedProfileRead.Unavailable, reader(fake, owner = null).read())
        assertTrue(fake.requests.isEmpty())
    }

    @Test
    fun `로그에 반려견 이름과 uid와 토큰이 없다`() = runTest {
        val log = mutableListOf<String>()
        reader(FakeTransport { HttpResponse(200, kongi) }, log = log).read()
        reader(FakeTransport { HttpResponse(403, "{}") }, log = log).read()
        assertEquals(2, log.size)
        val all = log.joinToString("\n")
        for (secret in listOf("콩이", "owner-uid-1", "car-uid-1", "token-1", "닭고기")) assertFalse(secret, all.contains(secret))
        assertTrue(log[0].startsWith("반려견 정보: 서버"))
        assertTrue(log[1].startsWith("반려견 정보: 연결 끊김"))
    }
}
