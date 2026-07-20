package com.shinjikai.dictionary.data

interface DictionarySource {
    suspend fun searchWords(term: String, page: Int = 0): Result<SearchWordsResponse>
    suspend fun loadWordDetails(id: Int): Result<WordDetailsResponse>
    suspend fun loadCategories(): Result<LoadCategoriesResponse>
    suspend fun loadCategory(id: Int, page: Int = 0): Result<LoadCategoryResponse>
}

class ShinjikaiRepository(
    private val source: DictionarySource
) {
    suspend fun searchWords(term: String, page: Int = 0): Result<SearchWordsResponse> {
        return source.searchWords(term = term, page = page)
    }

    suspend fun loadWordDetails(id: Int): Result<WordDetailsResponse> {
        return source.loadWordDetails(id)
    }

    suspend fun loadCategories(): Result<LoadCategoriesResponse> {
        return source.loadCategories()
    }

    suspend fun loadCategory(id: Int, page: Int = 0): Result<LoadCategoryResponse> {
        return source.loadCategory(id = id, page = page)
    }
}
