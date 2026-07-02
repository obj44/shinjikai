package com.shinjikai.dictionary.data

import java.text.Normalizer
import java.util.Locale

private val ROMAJI_WHITESPACE_REGEX = Regex("""\s+""")

internal object RomajiConverter {
    fun toHiraganaIfRomaji(input: String): String? {
        val normalized = normalizeRomaji(input)
        if (normalized.isBlank()) return null
        if (!normalized.any { it in 'a'..'z' }) return null
        if (normalized.any { it !in ALLOWED_ROMAJI_CHARS }) return null

        val output = StringBuilder(normalized.length)
        var index = 0
        while (index < normalized.length) {
            val ch = normalized[index]
            when {
                ch.isWhitespace() || ch == '-' || ch == '_' -> {
                    output.append(' ')
                    index += 1
                }
                ch == '\'' -> {
                    index += 1
                }
                ch == 'n' && index + 1 < normalized.length && normalized[index + 1] == 'n' -> {
                    output.append('ん')
                    index += 2
                }
                ch == 'n' && (index + 1 == normalized.length || normalized[index + 1] !in VOWELS && normalized[index + 1] != 'y') -> {
                    output.append('ん')
                    index += 1
                }
                isSmallTsu(normalized, index) -> {
                    output.append('っ')
                    index += 1
                }
                else -> {
                    val match = findSyllable(normalized, index)
                    if (match != null) {
                        output.append(match.second)
                        index += match.first.length
                    } else {
                        return null
                    }
                }
            }
        }

        return output.toString()
            .replace(ROMAJI_WHITESPACE_REGEX, " ")
            .trim()
            .takeIf { it.isNotBlank() }
    }

    private fun normalizeRomaji(input: String): String {
        return Normalizer.normalize(input.trim().lowercase(Locale.ROOT), Normalizer.Form.NFKC)
            .replace("ā", "aa")
            .replace("ī", "ii")
            .replace("ū", "uu")
            .replace("ē", "ee")
            .replace("ō", "ou")
    }

    private fun isSmallTsu(text: String, index: Int): Boolean {
        if (index + 1 >= text.length) return false
        val current = text[index]
        val next = text[index + 1]
        return current == next && current !in VOWELS && current != 'n'
    }

    private fun findSyllable(text: String, index: Int): Pair<String, String>? {
        for (size in 3 downTo 1) {
            if (index + size > text.length) continue
            val key = text.substring(index, index + size)
            SYLLABLES[key]?.let { return key to it }
        }
        return null
    }

    private val VOWELS = setOf('a', 'i', 'u', 'e', 'o')

    private val ALLOWED_ROMAJI_CHARS = (
        ('a'..'z').toSet() + setOf(' ', '\t', '\n', '\r', '\'', '-', '_')
    )

    private val SYLLABLES = linkedMapOf(
        "kya" to "きゃ",
        "kyu" to "きゅ",
        "kyo" to "きょ",
        "gya" to "ぎゃ",
        "gyu" to "ぎゅ",
        "gyo" to "ぎょ",
        "sha" to "しゃ",
        "shu" to "しゅ",
        "sho" to "しょ",
        "sya" to "しゃ",
        "syu" to "しゅ",
        "syo" to "しょ",
        "ja" to "じゃ",
        "ju" to "じゅ",
        "jo" to "じょ",
        "jya" to "じゃ",
        "jyu" to "じゅ",
        "jyo" to "じょ",
        "zya" to "じゃ",
        "zyu" to "じゅ",
        "zyo" to "じょ",
        "cha" to "ちゃ",
        "chu" to "ちゅ",
        "cho" to "ちょ",
        "tya" to "ちゃ",
        "tyu" to "ちゅ",
        "tyo" to "ちょ",
        "nya" to "にゃ",
        "nyu" to "にゅ",
        "nyo" to "にょ",
        "hya" to "ひゃ",
        "hyu" to "ひゅ",
        "hyo" to "ひょ",
        "bya" to "びゃ",
        "byu" to "びゅ",
        "byo" to "びょ",
        "pya" to "ぴゃ",
        "pyu" to "ぴゅ",
        "pyo" to "ぴょ",
        "mya" to "みゃ",
        "myu" to "みゅ",
        "myo" to "みょ",
        "rya" to "りゃ",
        "ryu" to "りゅ",
        "ryo" to "りょ",
        "fa" to "ふぁ",
        "fi" to "ふぃ",
        "fe" to "ふぇ",
        "fo" to "ふぉ",
        "tsa" to "つぁ",
        "tsi" to "つぃ",
        "tse" to "つぇ",
        "tso" to "つぉ",
        "shi" to "し",
        "chi" to "ち",
        "tsu" to "つ",
        "fu" to "ふ",
        "ji" to "じ",
        "di" to "ぢ",
        "du" to "づ",
        "va" to "ゔぁ",
        "vi" to "ゔぃ",
        "vu" to "ゔ",
        "ve" to "ゔぇ",
        "vo" to "ゔぉ",
        "ka" to "か",
        "ki" to "き",
        "ku" to "く",
        "ke" to "け",
        "ko" to "こ",
        "ga" to "が",
        "gi" to "ぎ",
        "gu" to "ぐ",
        "ge" to "げ",
        "go" to "ご",
        "sa" to "さ",
        "si" to "し",
        "su" to "す",
        "se" to "せ",
        "so" to "そ",
        "za" to "ざ",
        "zi" to "じ",
        "zu" to "ず",
        "ze" to "ぜ",
        "zo" to "ぞ",
        "ta" to "た",
        "ti" to "ち",
        "tu" to "つ",
        "te" to "て",
        "to" to "と",
        "da" to "だ",
        "de" to "で",
        "do" to "ど",
        "na" to "な",
        "ni" to "に",
        "nu" to "ぬ",
        "ne" to "ね",
        "no" to "の",
        "ha" to "は",
        "hi" to "ひ",
        "hu" to "ふ",
        "he" to "へ",
        "ho" to "ほ",
        "ba" to "ば",
        "bi" to "び",
        "bu" to "ぶ",
        "be" to "べ",
        "bo" to "ぼ",
        "pa" to "ぱ",
        "pi" to "ぴ",
        "pu" to "ぷ",
        "pe" to "ぺ",
        "po" to "ぽ",
        "ma" to "ま",
        "mi" to "み",
        "mu" to "む",
        "me" to "め",
        "mo" to "も",
        "ya" to "や",
        "yu" to "ゆ",
        "yo" to "よ",
        "ra" to "ら",
        "ri" to "り",
        "ru" to "る",
        "re" to "れ",
        "ro" to "ろ",
        "wa" to "わ",
        "wi" to "うぃ",
        "we" to "うぇ",
        "wo" to "を",
        "a" to "あ",
        "i" to "い",
        "u" to "う",
        "e" to "え",
        "o" to "お"
    )
}
