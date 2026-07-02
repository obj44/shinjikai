package com.shinjikai.dictionary

import com.shinjikai.dictionary.data.WritingPart
import org.junit.Assert.assertEquals
import org.junit.Test

class HeadwordFuriganaTextTest {
    @Test
    fun `kanji iteration mark stays in the kanji ruby token`() {
        val segments = buildHeadwordFuriganaSegments(
            text = "\u6642\u3005",
            reading = "\u3068\u304D\u3069\u304D",
            parts = emptyList()
        )

        assertEquals(
            listOf(HeadwordRubySegment(base = "\u6642\u3005", ruby = "\u3068\u304D\u3069\u304D")),
            segments
        )
    }

    @Test
    fun `adjacent kanji writing parts stay grouped as one ruby segment`() {
        val segments = buildHeadwordFuriganaSegments(
            text = "幸福",
            reading = "こうふく",
            parts = listOf(
                WritingPart(kanji = '幸'.code, reading = "こう"),
                WritingPart(kanji = '福'.code, reading = "ふく")
            )
        )

        assertEquals(
            listOf(HeadwordRubySegment(base = "幸福", ruby = "こうふく")),
            segments
        )
    }

    @Test
    fun `writing parts flush around kana without losing ruby`() {
        val segments = buildHeadwordFuriganaSegments(
            text = "お金を預けた",
            reading = "おかねをあずけた",
            parts = listOf(
                WritingPart(kanji = '金'.code, reading = "かね"),
                WritingPart(kanji = '預'.code, reading = "あず")
            )
        )

        assertEquals(
            listOf(
                HeadwordRubySegment(base = "お", ruby = null),
                HeadwordRubySegment(base = "金", ruby = "かね"),
                HeadwordRubySegment(base = "を", ruby = null),
                HeadwordRubySegment(base = "預", ruby = "あず"),
                HeadwordRubySegment(base = "け", ruby = null),
                HeadwordRubySegment(base = "た", ruby = null)
            ),
            segments
        )
    }

    @Test
    fun `fallback alignment adds ruby only to kanji tokens`() {
        val segments = buildHeadwordFuriganaSegments(
            text = "預ける",
            reading = "あずける",
            parts = emptyList()
        )

        assertEquals(
            listOf(
                HeadwordRubySegment(base = "預", ruby = "あず"),
                HeadwordRubySegment(base = "ける", ruby = null)
            ),
            segments
        )
    }

    @Test
    fun `kana only words render without ruby`() {
        val segments = buildHeadwordFuriganaSegments(
            text = "かわいい",
            reading = "かわいい",
            parts = emptyList()
        )

        assertEquals(
            listOf(HeadwordRubySegment(base = "かわいい", ruby = null)),
            segments
        )
    }
}
