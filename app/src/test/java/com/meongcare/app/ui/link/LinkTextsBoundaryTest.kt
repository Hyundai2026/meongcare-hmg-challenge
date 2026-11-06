package com.meongcare.app.ui.link

import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.link.remainingMinutesOf
import com.meongcare.app.domain.pet.PetProfileLoad
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/** 연결 화면 값의 경계 (계획 014 설계 5절 "화면") */
class LinkTextsBoundaryTest {

    @Test
    fun `품종이 없어도 이름만으로 요약한다`() {
        val noBreed = SamplePetProfiles.bori.copy(name = "콩이", breed = null)
        assertEquals(LinkedPet("콩이", null), linkedPetOf(PetProfileLoad.Loaded(noBreed)))
    }

    @Test
    fun `만료 전 화면의 분은 1에서 10 사이다`() {
        val created = Instant.parse("2026-11-06T09:00:00Z")
        var at = created
        val end = created.plus(Duration.ofMinutes(10))
        while (at.isBefore(end)) {
            val shown = shownMinutesOf(remainingMinutesOf(created, at))
            assertEquals("$at: $shown", true, shown in 1..10)
            at = at.plusMillis(997)
        }
        // 마지막 1ms도 "1분 안에"
        assertEquals(1, shownMinutesOf(remainingMinutesOf(created, end.minusMillis(1))))
    }
}
