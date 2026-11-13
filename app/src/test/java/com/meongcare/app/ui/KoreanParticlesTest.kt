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
}
