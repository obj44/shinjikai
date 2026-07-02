package com.shinjikai.dictionary.data

internal fun WordDetailsResponse.examplesForMeaning(meaning: Meaning): List<SentenceExample> {
    val examplesById = sentenceMap.orEmpty()
    return meaning.sentenceIds.orEmpty()
        .mapNotNull { sentenceId -> examplesById[sentenceId.toString()] }
        .filter(SentenceExample::hasVisibleContent)
        .distinctBy(SentenceExample::stableContentKey)
}

internal fun WordDetailsResponse.directMeaningExamples(): List<SentenceExample> {
    return word.meanings
        .flatMap(::examplesForMeaning)
        .distinctBy(SentenceExample::stableContentKey)
}

internal fun WordDetailsResponse.additionalExamples(): List<SentenceExample> {
    val directExamples = directMeaningExamples()
    return additionalExamples(directExamples)
}

internal fun WordDetailsResponse.additionalExamples(
    directExamples: List<SentenceExample>
): List<SentenceExample> {
    val directIds = directExamples.mapNotNullTo(hashSetOf()) { it.id.takeIf { id -> id > 0 } }
    val directContentKeys = directExamples.mapTo(hashSetOf(), SentenceExample::stableContentKey)
    val unassignedMappedExamples = sentenceMap.orEmpty().values
        .filterNot { it.id > 0 && it.id in directIds }
    return (unassignedMappedExamples + sentenceSearch)
        .filter(SentenceExample::hasVisibleContent)
        .filterNot { example ->
            (example.id > 0 && example.id in directIds) ||
                example.stableContentKey() in directContentKeys
        }
        .distinctBy(SentenceExample::stableContentKey)
}

internal fun SentenceExample.hasVisibleContent(): Boolean {
    return text.isNotBlank() || kana.isNotBlank() || arabic.isNotBlank()
}

internal fun SentenceExample.stableContentKey(): String {
    return "$id|${text.trim()}|${kana.trim()}|${arabic.trim()}"
}

internal fun KanjiInfo.displayCharacter(): String {
    if (!Character.isValidCodePoint(character)) return ""
    return String(Character.toChars(character))
}
