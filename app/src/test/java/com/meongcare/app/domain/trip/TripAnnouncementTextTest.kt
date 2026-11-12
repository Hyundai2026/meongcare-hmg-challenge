package com.meongcare.app.domain.trip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripAnnouncementTextTest {

    private val name = "시연용 바른 동물병원"

    private fun all(hospital: String) = listOf(
        TripAnnouncement.Departed(hospital, cardSent = true),
        TripAnnouncement.Departed(hospital, cardSent = false),
        TripAnnouncement.CardSent(hospital),
        TripAnnouncement.CardFailed,
        TripAnnouncement.Acknowledged(hospital),
        TripAnnouncement.Arrived(hospital),
    )

    @Test
    fun `출발 안내는 병원까지 안내를 시작한다고 말하고 카드를 보냈을 때만 덧붙인다`() {
        assertEquals(
            "시연용 바른 동물병원까지 안내를 시작해요. 진료 카드를 보냈어요.",
            TripAnnouncement.Departed(name, cardSent = true).spokenText(),
        )
        assertEquals(
            "시연용 바른 동물병원까지 안내를 시작해요.",
            TripAnnouncement.Departed(name, cardSent = false).spokenText(),
        )
    }

    @Test
    fun `사건마다 표의 문장을 그대로 말한다`() {
        assertEquals("진료 카드를 시연용 바른 동물병원에 보냈어요.", TripAnnouncement.CardSent(name).spokenText())
        assertEquals("진료 카드를 보내지 못했어요. 정차하면 다시 보낼 수 있어요.", TripAnnouncement.CardFailed.spokenText())
        assertEquals("시연용 바른 동물병원에서 진료 카드를 확인했어요.", TripAnnouncement.Acknowledged(name).spokenText())
        assertEquals(
            "시연용 바른 동물병원에 도착했어요. 진료 카드를 접수처에 보여 주세요.",
            TripAnnouncement.Arrived(name).spokenText(),
        )
    }

    @Test
    fun `병원 이름 뒤에 받침에 따라 바뀌는 말을 붙이지 않는다`() {
        val names = listOf("행복동물병원", "튼튼 동물의료센터 24시", "ABC Vet", "바른 펫 클리닉")
        val changing = listOf("이 ", "가 ", "을 ", "를 ", "은 ", "는 ", "과 ", "와 ", "으로", "이에요", "예요")
        for (hospital in names) {
            for (announcement in all(hospital)) {
                val text = announcement.spokenText()
                if (!text.contains(hospital)) continue
                val after = text.substringAfter(hospital)
                assertTrue(
                    "\"$hospital\" 뒤가 받침과 상관없는 말이어야 한다: $text",
                    listOf("까지", "에서", "에").any { after.startsWith(it) },
                )
                assertFalse("받침에 따라 바뀌는 말: $text", changing.any { after.startsWith(it) })
            }
        }
    }

    @Test
    fun `모든 문장은 두 문장 이하이고 병원 이름을 빼면 30자 안팎이다`() {
        for (announcement in all(name)) {
            val text = announcement.spokenText()
            val sentences = text.split('.').count { it.isNotBlank() }
            assertTrue("두 문장 이하: $text", sentences <= 2)
            val withoutName = text.replace(name, "")
            assertTrue("병원 이름을 빼고 35자 이하: $text (${withoutName.length}자)", withoutName.length <= 35)
        }
    }

    @Test
    fun `로그 이름에는 병원 이름이 들어가지 않는다`() {
        for (hospital in listOf(name, "행복동물병원")) {
            for (announcement in all(hospital)) assertFalse(announcement.logName.contains(hospital))
        }
        assertEquals(
            listOf("출발(카드 보냄)", "출발", "카드 보냄", "카드 못 보냄", "병원 확인", "도착"),
            all(name).map { it.logName },
        )
    }
}
