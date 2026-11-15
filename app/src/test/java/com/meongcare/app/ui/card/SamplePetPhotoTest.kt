package com.meongcare.app.ui.card

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** 계획 021 "사진": 샘플 반려견마다 사진이 다르다 */
class SamplePetPhotoTest {

    @Test
    fun `두 샘플 반려견은 서로 다른 사진이다`() {
        val wanggeon = samplePetPhotoRes("P-DEMO-01")
        val osyong = samplePetPhotoRes("P-DEMO-02")
        assertNotNull(wanggeon)
        assertNotNull(osyong)
        assertNotEquals(wanggeon, osyong)
    }

    @Test
    fun `모르는 id는 사진이 없다`() {
        assertNull(samplePetPhotoRes("P-DEMO-03"))
        assertNull(samplePetPhotoRes(""))
    }
}
