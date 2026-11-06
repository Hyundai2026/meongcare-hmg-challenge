package com.meongcare.app.ui.link

import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.pet.PetProfileLoad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 연결 화면 글자에 들어가는 값 (strings.xml `link_*`) */
class LinkTextsTest {

    @Test
    fun `남은 시간은 N분 안에다`() {
        assertEquals(10, shownMinutesOf(10))
        assertEquals(3, shownMinutesOf(3))
    }

    @Test
    fun `1분 미만은 1분 안에다`() {
        assertEquals(1, shownMinutesOf(1))
        assertEquals(1, shownMinutesOf(0))
    }

    @Test
    fun `연결됨 문구에 반려견 이름이 들어간다`() {
        val kongi = SamplePetProfiles.bori.copy(name = "콩이", breed = "말티즈")
        assertEquals(LinkedPet("콩이", "말티즈"), linkedPetOf(PetProfileLoad.Loaded(kongi)))
    }

    @Test
    fun `반려견 정보를 받지 못했으면 이름 없이 연결됨이다`() {
        assertNull(linkedPetOf(null))
        assertNull(linkedPetOf(PetProfileLoad.Unavailable))
        assertNull(linkedPetOf(PetProfileLoad.NotRegistered))
    }
}
