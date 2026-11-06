package com.meongcare.app.data.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerConfigTest {

    @Test
    fun `프로젝트 ID가 없으면 서버를 쓰지 않는다`() {
        assertNull(serverConfigOf("", "key", "10.0.2.2", isDebugBuild = true))
        assertNull(serverConfigOf("  ", "key", "", isDebugBuild = false))
    }

    @Test
    fun `debug에서 에뮬레이터 주소가 있으면 에뮬레이터로 간다`() {
        val config = serverConfigOf("demo-meongcare", "", "10.0.2.2", isDebugBuild = true)!!
        assertTrue(config.isEmulator)
        assertEquals("http://10.0.2.2:8080/v1/projects/demo-meongcare/databases/(default)/documents", config.documentsUrl)
        assertEquals("http://10.0.2.2:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=demo-key", config.signUpUrl)
        assertEquals("projects/demo-meongcare/databases/(default)/documents", config.documentsName)
    }

    @Test
    fun `IPv6 에뮬레이터 주소는 대괄호로 감싼다`() {
        val config = serverConfigOf("demo-meongcare", "", "::1", isDebugBuild = true)!!
        assertTrue(config.documentsUrl.startsWith("http://[::1]:8080/"))
    }

    @Test
    fun `release는 에뮬레이터 주소를 무시한다`() {
        val config = serverConfigOf("meongcare", "AIza-key", "10.0.2.2", isDebugBuild = false)!!
        assertFalse(config.isEmulator)
        assertEquals("https://firestore.googleapis.com/v1/projects/meongcare/databases/(default)/documents", config.documentsUrl)
        assertEquals("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=AIza-key", config.signUpUrl)
    }

    @Test
    fun `release에서 API 키가 없으면 서버를 쓰지 않는다`() {
        assertNull(serverConfigOf("meongcare", "", "", isDebugBuild = false))
        assertNull(serverConfigOf("meongcare", " ", "10.0.2.2", isDebugBuild = false))
    }

    @Test
    fun `debug라도 에뮬레이터 주소가 없으면 진짜 서버 규칙이다`() {
        assertNull(serverConfigOf("meongcare", "", "", isDebugBuild = true))
        assertFalse(serverConfigOf("meongcare", "k", "", isDebugBuild = true)!!.isEmulator)
    }
}
