package com.meongcare.app.data.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 013 익명 토큰 갱신 주소 경계 ([ServerConfig.refreshUrl], 계획 013 설계 결정 8) */
class ServerConfigRefreshBoundaryTest {

    @Test
    fun `debug 에뮬레이터는 API 키가 있으면 갱신 주소에도 그 키를 쓴다`() {
        val config = serverConfigOf("demo-meongcare", "AIzaKey", "127.0.0.1", isDebugBuild = true)!!
        assertEquals("http://127.0.0.1:9099/securetoken.googleapis.com/v1/token?key=AIzaKey", config.refreshUrl)
    }

    @Test
    fun `갱신 주소는 가입 주소와 같은 호스트와 키를 쓴다`() {
        val cases = listOf(
            serverConfigOf("demo-meongcare", "", "10.0.2.2", isDebugBuild = true)!!,
            serverConfigOf("demo-meongcare", "k", "::1", isDebugBuild = true)!!,
            serverConfigOf("meongcare", "AIzaKey", "", isDebugBuild = false)!!,
        )
        for (config in cases) {
            val signUpOrigin = config.signUpUrl.substringBefore("/identitytoolkit")
            val refreshOrigin = config.refreshUrl.substringBefore("/securetoken")
            val emulator = config.isEmulator
            if (emulator) assertEquals(config.signUpUrl, signUpOrigin, refreshOrigin)
            assertEquals(config.signUpUrl, config.signUpUrl.substringAfter("?key="), config.refreshUrl.substringAfter("?key="))
        }
    }

    @Test
    fun `대괄호로 감싼 IPv6 호스트는 다시 감싸지 않는다`() {
        val config = serverConfigOf("demo-meongcare", "", "[::1]", isDebugBuild = true)!!
        assertEquals("http://[::1]:9099/securetoken.googleapis.com/v1/token?key=demo-key", config.refreshUrl)
    }

    @Test
    fun `앞뒤 공백을 지우고 갱신 주소를 만든다`() {
        val emulator = serverConfigOf(" demo-meongcare ", " k ", " 127.0.0.1 ", isDebugBuild = true)!!
        assertEquals("http://127.0.0.1:9099/securetoken.googleapis.com/v1/token?key=k", emulator.refreshUrl)
        val release = serverConfigOf("meongcare", "  AIzaKey\n", "", isDebugBuild = false)!!
        assertEquals("https://securetoken.googleapis.com/v1/token?key=AIzaKey", release.refreshUrl)
    }

    @Test
    fun `debug라도 에뮬레이터 주소가 없으면 https securetoken이다`() {
        for (host in listOf("", "   ")) {
            val config = serverConfigOf("meongcare", "AIzaKey", host, isDebugBuild = true)!!
            assertEquals("https://securetoken.googleapis.com/v1/token?key=AIzaKey", config.refreshUrl)
        }
    }

    @Test
    fun `release는 어떤 에뮬레이터 주소가 있어도 갱신 주소가 https securetoken이다`() {
        for (host in listOf("127.0.0.1", "localhost", "::1", "[::1]", "10.0.2.2")) {
            val config = serverConfigOf("meongcare", "AIzaKey", host, isDebugBuild = false)!!
            assertTrue(host, config.refreshUrl.startsWith("https://securetoken.googleapis.com/"))
            assertFalse(host, config.refreshUrl.contains(":9099"))
        }
    }

    @Test
    fun `release에서 API 키가 없으면 갱신 주소도 없다`() {
        assertNull(serverConfigOf("meongcare", "", "", isDebugBuild = false))
        assertNull(serverConfigOf("meongcare", " ", "127.0.0.1", isDebugBuild = false))
    }
}
