package com.shinjikai.dictionary.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryContentTest {
    private val details = Gson().fromJson(FIXTURE, WordDetailsResponse::class.java)

    @Test
    fun `rich bundled fields survive deserialization`() {
        val meaning = details.word.meanings.single()

        assertEquals("物を人に頼んで保管してもらう。", meaning.japanese)
        assertEquals("大辞泉", meaning.source)
        assertEquals(listOf(101), meaning.sentenceIds)
        assertEquals("店を預ける", details.sentenceMap?.get("101")?.text)
        assertEquals(77, details.homophones?.single()?.id)
        assertEquals("預", details.kanjis?.single()?.displayCharacter())
        assertEquals(72, details.sentenceMap?.get("101")?.wordLinks?.single()?.wordId)
    }

    @Test
    fun `examples stay associated with their meaning and are not duplicated`() {
        val direct = details.examplesForMeaning(details.word.meanings.single())
        val additional = details.additionalExamples()
        val additionalFromKnownDirect = details.additionalExamples(direct)

        assertEquals(listOf(101), direct.map { it.id })
        assertEquals(listOf(102), additional.map { it.id })
        assertEquals(additional.map { it.id }, additionalFromKnownDirect.map { it.id })
    }

    @Test
    fun `visible example accepts any language field`() {
        assertTrue(SentenceExample(arabic = "مثال").hasVisibleContent())
    }

    private companion object {
        val FIXTURE =
            """
            {
              "Word": {
                "Id": 72,
                "Kana": "あずける",
                "Writings": [{"Text": "預ける", "Class": 0, "Parts": [{"Kanji": 38928, "Reading": "あず"}]}],
                "Meanings": [{
                  "Arabic": "يُودِع",
                  "Note": "بالكتابة 預ける",
                  "Japanese": "物を人に頼んで保管してもらう。",
                  "Source": "大辞泉",
                  "SentenceIds": [101],
                  "Pictures": [],
                  "Related": []
                }],
                "CategoryIds": [97],
                "Pictures": [],
                "SentenceIds": [101, 102]
              },
              "SimilarWords": [],
              "SentenceMap": {
                "101": {
                  "Id": 101,
                  "Text": "店を預ける",
                  "Kana": "みせをあずける",
                  "Arabic": "يأتمنه على المتجر",
                  "WordLinks": [{"Start": 2, "End": 5, "WordId": 72}]
                }
              },
              "SentenceSearch": [
                {"Id": 101, "Text": "店を預ける", "Kana": "みせをあずける", "Arabic": "يأتمنه على المتجر"},
                {"Id": 102, "Text": "子供を預けた", "Kana": "こどもをあずけた", "Arabic": "ترك الطفل في رعايته"}
              ],
              "Homophones": [{"Id": 77, "Kana": "あずける", "Writings": [{"Text": "預ける"}]}],
              "Kanjis": [{"Character": 38928, "OnYomi": "ヨ", "KunYomi": "あず.ける", "Meaning": "إيداع", "Class": 1}]
            }
            """.trimIndent()
    }
}
