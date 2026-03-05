package com.shinjikai.dictionary.data

import retrofit2.http.Body
import retrofit2.http.POST

interface ShinjikaiApi {
    @POST("rpc/SearchWords")
    suspend fun searchWords(@Body request: SearchWordsRequest): SearchWordsResponse

    @POST("rpc/LoadWordDetails")
    suspend fun loadWordDetails(@Body request: IdRequest): WordDetailsResponse

    @POST("rpc/LoadCategories")
    suspend fun loadCategories(@Body request: EmptyRequest = EmptyRequest()): LoadCategoriesResponse

    @POST("rpc/LoadCategory")
    suspend fun loadCategory(@Body request: CategoryRequest): LoadCategoryResponse
}
