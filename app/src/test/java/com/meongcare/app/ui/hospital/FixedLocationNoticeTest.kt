package com.meongcare.app.ui.hospital

import com.meongcare.app.di.DataMode
import com.meongcare.app.di.SourceKind
import com.meongcare.app.di.currentLocationKindOf
import com.meongcare.app.di.fixedLocationOf
import com.meongcare.app.di.resolveDataMode
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.RankBasis
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 계획 016 "설계 4": 목록 위 "시연용 위치(…) 기준이에요." 안내 (tester).
 * `isLocationSample`은 AppContainer와 같은 규칙(`currentLocationKindOf == FAKE`)으로 만든다.
 * 글자는 Compose 없이 strings.xml 원문을 읽어 형식만 확인한다.
 */
class FixedLocationNoticeTest {

    private fun isLocationSample(mode: DataMode, auto: Boolean) = currentLocationKindOf(mode, auto) == SourceKind.FAKE

    @Test
    fun `일반 폰의 시연·Real에서 위치를 알면 시연용 위치 안내가 나온다`() {
        listOf(DataMode.DEMO, DataMode.REAL).forEach { mode ->
            val notices = finderNoticesOf(HospitalOrigin.BUILT_IN, RankBasis.TRAVEL_PLUS_WAIT, isLocationSample(mode, auto = false))
            assertTrue(mode.name, FinderNotice.SAMPLE_LOCATION in notices)
        }
    }

    @Test
    fun `차량 기기의 시연·Real(내비 위치)에서는 시연용 위치 안내가 없다`() {
        listOf(DataMode.DEMO, DataMode.REAL).forEach { mode ->
            val notices = finderNoticesOf(HospitalOrigin.SERVER, RankBasis.TRAVEL_PLUS_WAIT, isLocationSample(mode, auto = true))
            assertFalse(mode.name, FinderNotice.SAMPLE_LOCATION in notices)
        }
    }

    @Test
    fun `release 일반 폰은 내장 병원 안내, 직선 어림 안내, 시연용 위치 안내 순이다`() {
        val mode = resolveDataMode(isDebugBuild = false, savedName = null)
        val notices = finderNoticesOf(HospitalOrigin.BUILT_IN, RankBasis.TRAVEL_PLUS_WAIT, isLocationSample(mode, auto = false))
        assertEquals(listOf(FinderNotice.BUILT_IN, FinderNotice.ESTIMATED_TRAVEL, FinderNotice.SAMPLE_LOCATION), notices)
    }

    @Test
    fun `서버 병원을 받은 폰 시연에서도 시연용 위치 안내가 나온다`() {
        val notices = finderNoticesOf(HospitalOrigin.SERVER, RankBasis.TRAVEL_PLUS_WAIT, isLocationSample(DataMode.DEMO, auto = false))
        assertEquals(listOf(FinderNotice.SERVER, FinderNotice.ESTIMATED_TRAVEL, FinderNotice.SAMPLE_LOCATION), notices)
    }

    @Test
    fun `위치를 몰라 대기 시간만으로 정렬하면 고정 위치 칸이어도 위치 안내가 없다`() {
        listOf(true, false).forEach { auto ->
            val notices = finderNoticesOf(HospitalOrigin.BUILT_IN, RankBasis.WAIT_ONLY, isLocationSample(DataMode.FAKE, auto))
            assertEquals("auto=$auto", listOf(FinderNotice.BUILT_IN), notices)
        }
    }

    @Test
    fun `시연용 위치 안내가 나오는 칸에는 언제나 채울 위치 이름이 있다`() {
        DataMode.entries.forEach { mode ->
            listOf(true, false).forEach { auto ->
                val shows = FinderNotice.SAMPLE_LOCATION in
                    finderNoticesOf(HospitalOrigin.BUILT_IN, RankBasis.TRAVEL_PLUS_WAIT, isLocationSample(mode, auto))
                val name = fixedLocationOf(mode, auto)?.address
                assertEquals("$mode/$auto", shows, name != null)
            }
        }
    }

    @Test
    fun `안내 글자는 폰 시연에서 '시연용 위치(서초동) 기준이에요'다`() {
        val name = fixedLocationOf(DataMode.DEMO, isAutomotive = false)!!.address
        assertEquals("시연용 위치(서초동) 기준이에요.", String.format(sampleLocationTemplate(), name))
    }

    @Test
    fun `안내 글자는 Fake 모드에서 '시연용 위치(수정구 시흥동) 기준이에요'다`() {
        val name = fixedLocationOf(DataMode.FAKE, isAutomotive = false)!!.address
        assertEquals("시연용 위치(수정구 시흥동) 기준이에요.", String.format(sampleLocationTemplate(), name))
    }

    /** strings.xml의 `finder_notice_sample_location` 원문. 단위 테스트는 app 모듈 폴더에서 돈다. */
    private fun sampleLocationTemplate(): String {
        val xml = File("src/main/res/values/strings.xml").readText()
        val match = Regex("""<string name="finder_notice_sample_location">([^<]*)</string>""").find(xml)
            ?: error("finder_notice_sample_location이 strings.xml에 없다")
        return match.groupValues[1]
    }
}
