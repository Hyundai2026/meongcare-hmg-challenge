package com.meongcare.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class KoreanParticlesTest {
    @Test
    fun `받침에 따라 을과 를을 고른다`() {
        assertEquals("온유동물병원을", withObjectParticle("온유동물병원"))
        assertEquals("별숲동물의료센터를", withObjectParticle("별숲동물의료센터"))
        assertEquals("보리를", withObjectParticle("보리"))
        assertEquals("ABC를", withObjectParticle("ABC"))
        assertEquals("", withObjectParticle(""))
    }

    @Test
    fun `받침에 따라 이와 가를 고른다`() {
        assertEquals("보리가", withSubjectParticle("보리"))
        assertEquals("왕건이가", withSubjectParticle("왕건이"))
        assertEquals("콩콩이가", withSubjectParticle("콩콩이"))
        assertEquals("별이", withSubjectParticle("별"))
        assertEquals("Max가", withSubjectParticle("Max"))
        assertEquals("", withSubjectParticle(""))
    }

    @Test
    fun `받침에 따라 으로와 로를 고른다`() {
        assertEquals("온유동물병원으로", withDirectionParticle("온유동물병원"))
        assertEquals("별숲동물의료센터로", withDirectionParticle("별숲동물의료센터"))
        assertEquals("서울로", withDirectionParticle("서울"))
        assertEquals("ABC로", withDirectionParticle("ABC"))
        assertEquals("", withDirectionParticle(""))
    }
}
