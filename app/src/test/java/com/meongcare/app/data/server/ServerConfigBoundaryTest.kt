package com.meongcare.app.data.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 서버 주소 경계. 기준: 계획 012 "설계 결정 3"의 `ServerConfig` 규칙
 * (프로젝트 ID 없으면 null, debug+에뮬레이터 주소면 에뮬레이터·키 없으면 demo-key, release는 에뮬레이터 주소 무시·키 없으면 null),
 * ux-guidelines "HTTPS·TLS 1.2+".
 */
class ServerConfigBoundaryTest {

    @Test
    fun `값의 앞뒤 공백은 지우고 주소를 만든다`() {
        val config = serverConfigOf(" demo-meongcare ", " k1 ", " 10.0.2.2 ", isDebugBuild = true)!!
        assertEquals("http://10.0.2.2:8080/v1/projects/demo-meongcare/databases/(default)/documents", config.documentsUrl)
        assertEquals("http://10.0.2.2:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=k1", config.signUpUrl)
        assertEquals("demo-meongcare", config.projectId)
    }

    @Test
    fun `debug 에뮬레이터는 API 키가 있으면 그 키를 쓴다`() {
        val config = serverConfigOf("demo-meongcare", "my-key", "127.0.0.1", isDebugBuild = true)!!
        assertTrue(config.signUpUrl.endsWith("?key=my-key"))
        assertTrue(config.documentsUrl.startsWith("http://127.0.0.1:8080/"))
    }

    @Test
    fun `debug에서 에뮬레이터 주소가 공백뿐이면 진짜 서버 규칙이다`() {
        assertNull(serverConfigOf("meongcare", "", "   ", isDebugBuild = true))
        val config = serverConfigOf("meongcare", "k", "   ", isDebugBuild = true)!!
        assertFalse(config.isEmulator)
        assertTrue(config.documentsUrl.startsWith("https://"))
    }

    @Test
    fun `release 주소는 어떤 조합이어도 https이고 에뮬레이터 주소가 들어가지 않는다`() {
        val hosts = listOf("", "10.0.2.2", "127.0.0.1", "localhost", "::1")
        val keys = listOf("", "k")
        for (host in hosts) for (key in keys) {
            val config = serverConfigOf("meongcare", key, host, isDebugBuild = false) ?: continue
            assertFalse("$host/$key", config.isEmulator)
            assertTrue("$host/$key", config.documentsUrl.startsWith("https://firestore.googleapis.com/"))
            assertTrue("$host/$key", config.signUpUrl.startsWith("https://identitytoolkit.googleapis.com/"))
            if (host.isNotEmpty()) {
                assertFalse("$host/$key", config.documentsUrl.contains(host) || config.signUpUrl.contains(host))
            }
        }
    }

    @Test
    fun `release에서 에뮬레이터 주소만 있고 API 키가 없으면 서버를 쓰지 않는다`() {
        assertNull(serverConfigOf("demo-meongcare", "", "10.0.2.2", isDebugBuild = false))
    }

    @Test
    fun `문서 주소는 commit에 쓰는 문서 이름 앞부분으로 끝난다`() {
        listOf(
            serverConfigOf("p1", "", "10.0.2.2", isDebugBuild = true)!!,
            serverConfigOf("p1", "k", "", isDebugBuild = false)!!,
        ).forEach { config ->
            assertTrue(config.documentsUrl, config.documentsUrl.endsWith("/v1/" + config.documentsName))
            assertFalse(config.documentsUrl.endsWith("/"))
        }
    }
}
