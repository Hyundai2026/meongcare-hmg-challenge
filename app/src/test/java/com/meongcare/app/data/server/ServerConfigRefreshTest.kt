package com.meongcare.app.data.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** 013: 익명 토큰 갱신 주소 [ServerConfig.refreshUrl] */
class ServerConfigRefreshTest {

    @Test
    fun `에뮬레이터면 9099의 securetoken 주소다`() {
        val config = serverConfigOf("demo-meongcare", "", "127.0.0.1", isDebugBuild = true)!!
        assertEquals("http://127.0.0.1:9099/securetoken.googleapis.com/v1/token?key=demo-key", config.refreshUrl)
    }

    @Test
    fun `release는 https securetoken만 쓰고 에뮬레이터 호스트가 없다`() {
        val config = serverConfigOf("meongcare", "AIzaKey", "127.0.0.1", isDebugBuild = false)!!
        assertEquals("https://securetoken.googleapis.com/v1/token?key=AIzaKey", config.refreshUrl)
        assertFalse(config.refreshUrl.contains("127.0.0.1"))
    }

    @Test
    fun `IPv6 에뮬레이터 호스트는 대괄호로 감싼다`() {
        val config = serverConfigOf("demo-meongcare", "k", "::1", isDebugBuild = true)!!
        assertEquals("http://[::1]:9099/securetoken.googleapis.com/v1/token?key=k", config.refreshUrl)
    }
}
