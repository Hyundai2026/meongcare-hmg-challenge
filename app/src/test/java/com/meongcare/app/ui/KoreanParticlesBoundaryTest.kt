package com.meongcare.app.ui

import com.meongcare.app.ui.hospital.DemoHospitals
import org.junit.Assert.assertEquals
import org.junit.Test

/** `withObjectParticle` 경계: 한글 음절 범위 끝, 한글이 아닌 끝 글자, 시연 병원 이름. */
class KoreanParticlesBoundaryTest {

    @Test
    fun `한글 음절 범위의 처음과 끝도 받침을 맞게 본다`() {
        assertEquals("가를", withObjectParticle("가"))
        assertEquals("각을", withObjectParticle("각"))
        assertEquals("힣을", withObjectParticle("힣"))
        assertEquals("히를", withObjectParticle("히"))
    }

    @Test
    fun `한 글자 이름도 받침에 따라 고른다`() {
        assertEquals("콩을", withObjectParticle("콩"))
        assertEquals("코를", withObjectParticle("코"))
    }

    @Test
    fun `끝 글자가 한글 음절이 아니면 를이다`() {
        assertEquals("병원2를", withObjectParticle("병원2"))
        assertEquals("ㄱ를", withObjectParticle("ㄱ"))
        assertEquals("병원)를", withObjectParticle("병원)"))
    }

    @Test
    fun `시연 병원 이름마다 맞는 조사가 붙는다`() {
        assertEquals(
            listOf("별숲동물의료센터를", "온유동물병원을", "나래동물의료센터를", "달빛24시동물의료센터를", "가람24시동물병원을"),
            DemoHospitals.all.map { withObjectParticle(it.name) },
        )
    }
}
