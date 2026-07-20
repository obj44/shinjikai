package com.shinjikai.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailMeaningTextTest {
    @Test
    fun `braced numeric glossary references remain clickable after normalization`() {
        val raw = "Names: {\u745E\u7A42:69440}\u30FB{\u5927\u548C:12174} and [{\u65E5\u672C:8128}]"
        val references = mutableListOf<GlossaryReference>()

        val normalized = normalizeMeaningText(raw)
        val visible = stripGlossaryReferences(
            raw = normalized,
            enableGlossaryLinks = true,
            references = references
        )

        assertEquals("Names: \u745E\u2060\u7A42\u30FB\u5927\u2060\u548C and \u65E5\u2060\u672C", visible)
        assertEquals(
            listOf(
                GlossaryReference(69440, "\u745E\u7A42", 7, 10),
                GlossaryReference(12174, "\u5927\u548C", 11, 14),
                GlossaryReference(8128, "\u65E5\u672C", 19, 22)
            ),
            references
        )
    }

    @Test
    fun `display tags without numeric ids are still flattened`() {
        val visible = normalizeMeaningText("{label:value} {\u7A7A}")

        assertEquals("label: \u7A7A:", visible)
        assertTrue(visible.none { it == '{' || it == '}' })
    }
}
