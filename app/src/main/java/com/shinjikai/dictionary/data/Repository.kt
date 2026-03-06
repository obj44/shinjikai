package com.shinjikai.dictionary.data

import android.util.Base64
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.Locale
import kotlin.random.Random

interface DictionarySource {
    suspend fun searchWords(term: String, page: Int = 0): Result<SearchWordsResponse>
    suspend fun loadWordDetails(id: Int): Result<WordDetailsResponse>
    suspend fun loadCategories(): Result<LoadCategoriesResponse>
    suspend fun loadCategory(id: Int, page: Int = 0): Result<LoadCategoryResponse>
}

class ShinjikaiRepository(
    private val source: DictionarySource = RemoteDictionarySource()
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

class RemoteDictionarySource(
    private val cacheDir: File? = null
) : DictionarySource {
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
    @Volatile
    private var categoriesCache: LoadCategoriesResponse? = null
    private val categoryMembersCacheLock = Any()
    private val categoryMembersCache = object : LinkedHashMap<String, LoadCategoryResponse>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LoadCategoryResponse>?): Boolean {
            return size > CATEGORY_MEMBERS_CACHE_LIMIT
        }
    }

    init {
        val clientId = makeClientId()

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .header("X-Client-Id", clientId)
                    .build()
                chain.proceed(request)
            })
        cacheDir?.let { dir ->
            val httpCacheDir = File(dir, "shinjikai_http_cache")
            clientBuilder.cache(Cache(httpCacheDir, HTTP_CACHE_MAX_BYTES))
        }

        val client = clientBuilder.build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://shinjikai.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        api = retrofit.create(ShinjikaiApi::class.java)
    }

    override suspend fun searchWords(term: String, page: Int): Result<SearchWordsResponse> {
        val normalizedTerm = term.trim().lowercase(Locale.ROOT)
        val cacheKey = "${normalizedTerm}:${page}"
        synchronized(searchCacheLock) {
            searchCache[cacheKey]?.let { return Result.success(it) }
        }

        return runCatching {
            api.searchWords(SearchWordsRequest(term = term, page = page))
        }.onSuccess { response ->
            synchronized(searchCacheLock) {
                searchCache[cacheKey] = response
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

    override suspend fun loadCategories(): Result<LoadCategoriesResponse> {
        categoriesCache?.let { return Result.success(it) }
        return runCatching {
            api.loadCategories()
        }.onSuccess { response ->
            categoriesCache = response
        }
    }

    override suspend fun loadCategory(id: Int, page: Int): Result<LoadCategoryResponse> {
        val cacheKey = "${id}:${page}"
        synchronized(categoryMembersCacheLock) {
            categoryMembersCache[cacheKey]?.let { return Result.success(it) }
        }

        return runCatching {
            api.loadCategory(CategoryRequest(id = id, page = page))
        }.onSuccess { response ->
            synchronized(categoryMembersCacheLock) {
                categoryMembersCache[cacheKey] = response
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
        const val CATEGORY_MEMBERS_CACHE_LIMIT = 120
        const val HTTP_CACHE_MAX_BYTES = 40L * 1024L * 1024L
    }
}




