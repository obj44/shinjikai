package com.shinjikai.dictionary.data

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
    @SerializedName("Text") val text: String = ""
)

data class IdRequest(
    @SerializedName("Id") val id: Int
)

data class WordDetailsResponse(
    @SerializedName("Word") val word: WordDetailsWord
)

data class WordDetailsWord(
    @SerializedName("Id") val id: Int,
    @SerializedName("Kana") val kana: String = "",
    @SerializedName("Writings") val writings: List<Writing> = emptyList(),
    @SerializedName("Meanings") val meanings: List<Meaning> = emptyList(),
    @SerializedName("JLPT") val jlpt: Int = 0,
    @SerializedName("Difficulty") val difficulty: Int = 0,
    @SerializedName("CategoryIds") val categoryIds: List<Int> = emptyList()
)

data class Meaning(
    @SerializedName("Arabic") val arabic: String = "",
    @SerializedName("Note") val note: String = "",
    @SerializedName("Related") val related: List<RelatedGroup> = emptyList()
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
