package com.shinjikai.dictionary.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTextNormalizationTest {
    @Test
    fun `offline search preview removes bullet prefixes and compacts whitespace`() {
        val raw = "\u2022  first line\n\nsecond   line"

        assertEquals("first line second line", buildOfflineSearchPreview(raw))
    }
}
