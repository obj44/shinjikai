package com.shinjikai.dictionary.data

import android.util.Base64
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import kotlin.random.Random

interface DictionarySource {
    suspend fun searchWords(term: String): Result<SearchWordsResponse>
    suspend fun loadWordDetails(id: Int): Result<WordDetailsResponse>
}

class ShinjikaiRepository(
    private val source: DictionarySource = RemoteDictionarySource()
) {
    suspend fun searchWords(term: String): Result<SearchWordsResponse> {
        return source.searchWords(term)
    }

    suspend fun loadWordDetails(id: Int): Result<WordDetailsResponse> {
        return source.loadWordDetails(id)
    }
}

class RemoteDictionarySource : DictionarySource {
    private val api: ShinjikaiApi
    private val searchCacheLock = Any()
    private val detailsCacheLock = Any()
    private val searchCache = object : LinkedHashMap<String, SearchWordsResponse>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SearchWordsResponse>?): Boolean {
            return size > SEARCH_CACHE_LIMIT
        }
    }
    private val detailsCache = object : LinkedHashMap<Int, WordDetailsResponse>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, WordDetailsResponse>?): Boolean {
            return size > DETAILS_CACHE_LIMIT
        }
    }

    init {
        val clientId = makeClientId()

        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .header("X-Client-Id", clientId)
                    .build()
                chain.proceed(request)
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://shinjikai.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        api = retrofit.create(ShinjikaiApi::class.java)
    }

    override suspend fun searchWords(term: String): Result<SearchWordsResponse> {
        val normalizedTerm = term.trim().lowercase(Locale.ROOT)
        synchronized(searchCacheLock) {
            searchCache[normalizedTerm]?.let { return Result.success(it) }
        }

        return runCatching {
            api.searchWords(SearchWordsRequest(term = term))
        }.onSuccess { response ->
            synchronized(searchCacheLock) {
                searchCache[normalizedTerm] = response
            }
        }
    }

    override suspend fun loadWordDetails(id: Int): Result<WordDetailsResponse> {
        synchronized(detailsCacheLock) {
            detailsCache[id]?.let { return Result.success(it) }
        }

        return runCatching {
            api.loadWordDetails(IdRequest(id = id))
        }.onSuccess { response ->
            synchronized(detailsCacheLock) {
                detailsCache[id] = response
            }
        }
    }

    private fun makeClientId(): String {
        val bytes = ByteArray(8)
        Random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private companion object {
        const val SEARCH_CACHE_LIMIT = 120
        const val DETAILS_CACHE_LIMIT = 400
    }
}
