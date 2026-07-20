package com.shinjikai.dictionary.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RawShinjikaiImporterTest {
    @Test
    fun `headword-less records are rejected`() {
        assertFalse(hasDictionaryHeadword(expression = "", reading = "  "))
    }

    @Test
    fun `a writing or reading is enough to retain a record`() {
        assertTrue(hasDictionaryHeadword(expression = "\u611B", reading = ""))
        assertTrue(hasDictionaryHeadword(expression = "", reading = "\u3042\u3044"))
    }
}
