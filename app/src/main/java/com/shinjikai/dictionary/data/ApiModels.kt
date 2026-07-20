package com.shinjikai.dictionary.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class SearchWordsRequest(
    @SerializedName("Term") val term: String,
    @SerializedName("Page") val page: Int = 0,
    @SerializedName("Mode") val mode: Int = 0
)

data class EmptyRequest(
    @SerializedName("_") val ignored: Int = 0
)

data class SearchWordsResponse(
    @SerializedName("Items") val items: List<SearchItem> = emptyList(),
    @SerializedName("Page") val page: Int = 0,
    @SerializedName("PageCount") val pageCount: Int = 0,
    @SerializedName("TotalCount") val totalCount: Int = 0
)

data class SearchItem(
    @SerializedName("Id") val id: Int,
    @SerializedName("Kana") val kana: String = "",
    @SerializedName("Writings") val writings: List<Writing> = emptyList(),
    @SerializedName("MeaningSummary") val meaningSummary: String = "",
    @SerializedName("JLPT") val jlpt: Int = 0,
    @SerializedName("Difficulty") val difficulty: Int = 0
) {
    val primaryWriting: String
        get() = writings.firstOrNull()?.text.orEmpty()
}

data class Writing(
    @SerializedName("Text") val text: String = "",
    @SerializedName("Class") val writingClass: Int = 0,
    @SerializedName("Parts") val parts: List<WritingPart>? = emptyList()
)

data class WritingPart(
    @SerializedName("Kanji") val kanji: Int = 0,
    @SerializedName("Reading") val reading: String = ""
)

data class IdRequest(
    @SerializedName("Id") val id: Int
)

data class WordDetailsResponse(
    @SerializedName("Word") val word: WordDetailsWord,
    @SerializedName("SimilarWords") val similarWords: List<WordRef> = emptyList(),
    @SerializedName("SentenceSearch") val sentenceSearch: List<SentenceExample> = emptyList(),
    @SerializedName("SentenceMap") val sentenceMap: Map<String, SentenceExample>? = emptyMap(),
    @SerializedName("Homophones") val homophones: List<WordRef>? = emptyList(),
    @SerializedName("Kanjis") val kanjis: List<KanjiInfo>? = emptyList()
)

data class WordDetailsWord(
    @SerializedName("Id") val id: Int,
    @SerializedName("Kana") val kana: String = "",
    @SerializedName("Writings") val writings: List<Writing> = emptyList(),
    @SerializedName("Meanings") val meanings: List<Meaning> = emptyList(),
    @SerializedName("JLPT") val jlpt: Int = 0,
    @SerializedName("Difficulty") val difficulty: Int = 0,
    @SerializedName("CategoryIds") val categoryIds: List<Int> = emptyList(),
    @SerializedName("Pictures") val pictures: List<JsonElement>? = emptyList(),
    @SerializedName("SentenceIds") val sentenceIds: List<Int>? = emptyList()
)

data class WordRef(
    @SerializedName("Id") val id: Int = 0,
    @SerializedName("Kana") val kana: String = "",
    @SerializedName("Writings") val writings: List<Writing> = emptyList()
) {
    val primaryWriting: String
        get() = writings.firstOrNull()?.text.orEmpty()
}

data class Meaning(
    @SerializedName("Arabic") val arabic: String = "",
    @SerializedName("Note") val note: String = "",
    @SerializedName("Japanese") val japanese: String? = "",
    @SerializedName("Source") val source: String? = "",
    @SerializedName("SentenceIds") val sentenceIds: List<Int>? = emptyList(),
    @SerializedName("Related") val related: List<RelatedGroup> = emptyList(),
    // The API sometimes returns pictures as strings and sometimes as objects; keep it flexible to avoid breaking parsing.
    // Example crash: "Expected a string but was BEGIN_OBJECT at $.Word.Meanings[0].Pictures[0]".
    @SerializedName(
        value = "Pictures",
        alternate = [
            "Images",
            "ImageUrls",
            "ImageURLs",
            "ImageURLS",
            "PictureUrls",
            "PictureURLs",
            "Media",
            "MediaUrls",
            "MediaURLs",
            "GlossaryImages",
            "GlossaryImageUrls",
            "GlossaryImageURLs"
        ]
    )
    val pictures: List<JsonElement> = emptyList()
)

data class CategoryRef(
    @SerializedName("Id") val id: Int = 0,
    @SerializedName("Name") val name: String = "",
    @SerializedName("ShortName") val shortName: String = ""
)

data class RelatedGroup(
    @SerializedName("Label") val label: String = "",
    @SerializedName("Type") val type: Int = 0,
    @SerializedName("Items") val items: List<RelatedWordItem> = emptyList()
)

data class RelatedWordItem(
    @SerializedName("WordId") val wordId: Int = 0,
    @SerializedName("Text") val text: String = "",
    @SerializedName("Kana") val kana: String = "",
    @SerializedName("MeaningNo") val meaningNo: Int = 0
)

data class SentenceExample(
    @SerializedName("Id") val id: Int = 0,
    @SerializedName("Text") val text: String = "",
    @SerializedName("Kana") val kana: String = "",
    @SerializedName("Arabic") val arabic: String = "",
    @SerializedName("WordLinks") val wordLinks: List<SentenceWordLink>? = emptyList()
)

data class SentenceWordLink(
    @SerializedName("Start") val start: Int = 0,
    @SerializedName("End") val end: Int = 0,
    @SerializedName("WordId") val wordId: Int = 0,
    @SerializedName("Text") val text: String = "",
    @SerializedName("Kana") val kana: String = ""
)

data class KanjiInfo(
    @SerializedName("Character") val character: Int = 0,
    @SerializedName("OnYomi") val onYomi: String = "",
    @SerializedName("KunYomi") val kunYomi: String = "",
    @SerializedName("Meaning") val meaning: String = "",
    @SerializedName("Class") val kanjiClass: Int = 0
)

data class LoadCategoriesResponse(
    @SerializedName("Categories") val categories: List<CategoryRef> = emptyList()
)

data class CategoryRequest(
    @SerializedName("Id") val id: Int,
    @SerializedName("Page") val page: Int = 0
)

data class LoadCategoryResponse(
    @SerializedName("Category") val category: CategoryRef = CategoryRef(),
    @SerializedName("Members") val members: SearchWordsResponse = SearchWordsResponse()
)
