package com.meongcare.app.ui.card

import com.meongcare.app.data.pet.SamplePetProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 계획 021 "사진"의 경계: 샘플 목록의 모든 반려견에 사진이 있고 서로 다르다.
 * id는 정확히 같아야 한다 (대소문자·공백이 다르면 모르는 id다).
 */
class SamplePetPhotoBoundaryTest {

    @Test
    fun `샘플 목록의 반려견마다 사진이 있다`() {
        SamplePetProfiles.all.forEach { pet ->
            assertNotNull("${pet.profile.name}(${pet.id})의 사진이 없다", samplePetPhotoRes(pet.id))
        }
    }

    @Test
    fun `샘플 목록의 사진은 모두 서로 다르다`() {
        val photos = SamplePetProfiles.all.map { samplePetPhotoRes(it.id) }
        assertEquals(photos.size, photos.toSet().size)
    }

    @Test
    fun `같은 id는 늘 같은 사진이다`() {
        assertEquals(samplePetPhotoRes("P-DEMO-01"), samplePetPhotoRes("P-DEMO-01"))
        assertEquals(samplePetPhotoRes("P-DEMO-02"), samplePetPhotoRes("P-DEMO-02"))
    }

    @Test
    fun `대소문자나 앞뒤 공백이 다른 id는 사진이 없다`() {
        assertNull(samplePetPhotoRes("p-demo-01"))
        assertNull(samplePetPhotoRes(" P-DEMO-01"))
        assertNull(samplePetPhotoRes("P-DEMO-02 "))
        assertNull(samplePetPhotoRes("P-DEMO-1"))
    }

    @Test
    fun `반려견 이름으로는 사진을 찾지 않는다`() {
        assertNull(samplePetPhotoRes("왕건이"))
        assertNull(samplePetPhotoRes("오숑이"))
    }
}
