package com.meongcare.app.ui.debug

import com.meongcare.app.domain.location.CurrentLocation
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 011 계획 설계 결정 5의 문구 경계. 좌표는 소수 5자리, `Locale.ROOT`라서 기기 언어와 상관없이 같은 글자다.
 */
class DebugLocationTextBoundaryTest {

    private val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")
    private val southWest = CurrentLocation(-33.86882, -70.6693, null)

    /** 소수점이 쉼표이거나, 음수 기호가 다르거나(U+2212), 숫자 글자가 다른 언어들. */
    private val trickyLocales = listOf(
        Locale.GERMANY,
        Locale.FRANCE,
        Locale.forLanguageTag("sv-SE"),
        Locale.forLanguageTag("fi-FI"),
        Locale.forLanguageTag("ar-EG"),
        Locale.forLanguageTag("fa-IR"),
        Locale.forLanguageTag("th-TH-u-nu-thai"),
        Locale.forLanguageTag("hi-IN-u-nu-deva"),
        Locale.KOREA,
    )

    private fun <T> withDefaultLocale(locale: Locale, block: () -> T): T {
        val saved = Locale.getDefault()
        return try {
            Locale.setDefault(locale)
            block()
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun `소수점이나 음수 기호나 숫자 글자가 다른 언어에서도 같은 글자다`() {
        for (locale in trickyLocales) {
            withDefaultLocale(locale) {
                assertEquals(
                    "$locale",
                    "위치(내비): 37.41272, 127.09468 · 수정구 시흥동",
                    debugLocationText(DebugLocationUiState.Found(here), "내비"),
                )
                assertEquals(
                    "$locale",
                    "위치(내비): -33.86882, -70.66930 · 주소 모름",
                    debugLocationText(DebugLocationUiState.Found(southWest), "내비"),
                )
            }
        }
    }

    @Test
    fun `좌표는 항상 소수 다섯 자리다`() {
        assertEquals(
            "위치(Fake): 37.50000, 127.00000 · 주소 모름",
            debugLocationText(DebugLocationUiState.Found(CurrentLocation(37.5, 127.0, null)), "Fake"),
        )
    }

    @Test
    fun `경계 좌표도 그대로 보인다`() {
        assertEquals(
            "위치(내비): -90.00000, -180.00000 · 주소 모름",
            debugLocationText(DebugLocationUiState.Found(CurrentLocation(-90.0, -180.0, null)), "내비"),
        )
        assertEquals(
            "위치(내비): 90.00000, 180.00000 · 주소 모름",
            debugLocationText(DebugLocationUiState.Found(CurrentLocation(90.0, 180.0, null)), "내비"),
        )
    }

    @Test
    fun `Fake 출처의 위치도 같은 모양이다`() {
        assertEquals(
            "위치(Fake): 37.41272, 127.09468 · 수정구 시흥동",
            debugLocationText(DebugLocationUiState.Found(here), "Fake"),
        )
    }

    @Test
    fun `주소는 받은 그대로 붙인다`() {
        val withLongAddress = here.copy(address = "경기도 성남시 수정구 시흥동 (테스트)")
        assertEquals(
            "위치(내비): 37.41272, 127.09468 · 경기도 성남시 수정구 시흥동 (테스트)",
            debugLocationText(DebugLocationUiState.Found(withLongAddress), "내비"),
        )
    }

    @Test
    fun `좌표가 없는 상태 문구도 언어와 상관없이 같다`() {
        for (locale in trickyLocales) {
            withDefaultLocale(locale) {
                assertEquals("$locale", "위치(내비): 아직 묻지 않음", debugLocationText(DebugLocationUiState.NotAsked, "내비"))
                assertEquals("$locale", "위치(내비): 묻는 중…", debugLocationText(DebugLocationUiState.Asking, "내비"))
                assertEquals("$locale", "위치(Fake): 모름", debugLocationText(DebugLocationUiState.Unknown, "Fake"))
            }
        }
    }
}
