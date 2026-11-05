package com.meongcare.app.ui.debug

import com.meongcare.app.domain.location.CurrentLocation
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugLocationTextTest {

    private val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")

    @Test
    fun `좌표는 소수 다섯째 자리까지 점으로 보여 준다`() {
        val saved = Locale.getDefault()
        try {
            // 소수점이 쉼표인 언어여도 점이어야 한다
            Locale.setDefault(Locale.GERMANY)
            assertEquals(
                "위치(내비): 37.41272, 127.09468 · 수정구 시흥동",
                debugLocationText(DebugLocationUiState.Found(here), "내비"),
            )
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun `주소가 없으면 주소 모름이라고 쓴다`() {
        assertEquals(
            "위치(내비): 37.41272, 127.09468 · 주소 모름",
            debugLocationText(DebugLocationUiState.Found(here.copy(address = null)), "내비"),
        )
    }

    @Test
    fun `상태마다 출처 이름을 붙인다`() {
        assertEquals("위치(Fake): 아직 묻지 않음", debugLocationText(DebugLocationUiState.NotAsked, "Fake"))
        assertEquals("위치(Fake): 묻는 중…", debugLocationText(DebugLocationUiState.Asking, "Fake"))
        assertEquals("위치(내비): 모름", debugLocationText(DebugLocationUiState.Unknown, "내비"))
    }
}
