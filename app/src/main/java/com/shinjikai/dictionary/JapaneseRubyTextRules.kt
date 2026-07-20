package com.shinjikai.dictionary

internal fun isJapaneseRubyKanjiLike(char: Char): Boolean {
    return isJapaneseRubyKanjiLikeCodePoint(char.code)
}

internal fun isJapaneseRubyKanjiLikeCodePoint(codePoint: Int): Boolean {
    val block = Character.UnicodeBlock.of(codePoint)
    return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
        block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
        block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
        codePoint == 0x3005 ||
        codePoint == 0x3006 ||
        codePoint == 0x30F6
}

internal fun japaneseRubyLiteralEquals(textChar: Char, readingChar: Char): Boolean {
    return textChar == readingChar ||
        normalizeJapaneseRubyLiteral(textChar) == normalizeJapaneseRubyLiteral(readingChar)
}

internal fun normalizeJapaneseRubyLiteral(char: Char): Char {
    return when (char) {
        '.', '\u3002' -> '\u3002'
        ',', '\u3001' -> '\u3001'
        '!', '\uFF01' -> '\uFF01'
        '?', '\uFF1F' -> '\uFF1F'
        ':', '\uFF1A' -> '\uFF1A'
        ';', '\uFF1B' -> '\uFF1B'
        '(', '\uFF08' -> '\uFF08'
        ')', '\uFF09' -> '\uFF09'
        '\uFF70', '\u30FC' -> '\u30FC'
        else -> char
    }
}

internal fun canDropJapaneseRubyLiteral(char: Char): Boolean {
    return char.isWhitespace() || char == '"' || char == '\'' || char == '\u201C' || char == '\u201D'
}
