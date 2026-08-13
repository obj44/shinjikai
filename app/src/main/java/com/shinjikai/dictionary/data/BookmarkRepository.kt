package com.shinjikai.dictionary.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookmarkRepository(
    private val bookmarkDao: BookmarkDao,
    private val yomitanDao: YomitanDao,
    private val gson: Gson = Gson()
) {
    fun pagedFlow(pageSize: Int = 30): Flow<PagingData<BookmarkItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = pageSize / 2,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { bookmarkDao.pagingSource() }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toBookmarkItem() }
        }
    }

    fun observeBookmarkedIds(): Flow<Set<Int>> {
        return bookmarkDao.observeIds().map { it.toSet() }
    }

    suspend fun getAll(): List<BookmarkItem> {
        return bookmarkDao.getAll().map { entity -> entity.toBookmarkItem() }
    }

    suspend fun upsert(item: SearchItem) {
        val createdAt = bookmarkDao.getCreatedAt(item.id) ?: java.util.Date().time
        bookmarkDao.upsert(
            BookmarkEntity(
                id = item.id,
                primaryWriting = item.primaryWriting,
                kana = item.kana,
                meaningSummary = item.meaningSummary,
                createdAt = createdAt
            )
        )
    }

    suspend fun upsertWithDetails(item: SearchItem, details: WordDetailsResponse) {
        val createdAt = bookmarkDao.getCreatedAt(item.id) ?: java.util.Date().time
        val detailsJson = withContext(Dispatchers.Default) {
            gson.toJson(details.withCanonicalPictureElements())
        }
        bookmarkDao.upsert(
            BookmarkEntity(
                id = item.id,
                primaryWriting = item.primaryWriting,
                kana = item.kana,
                meaningSummary = item.meaningSummary,
                detailsJson = detailsJson,
                detailsSavedAt = java.util.Date().time,
                createdAt = createdAt
            )
        )
    }
    suspend fun getSavedDetails(id: Int): WordDetailsResponse? {
        val json = bookmarkDao.getDetailsJsonById(id)?.trim().orEmpty()
        if (json.isBlank()) return null
        val imageDirectory = yomitanDao.getMetaValue(OFFLINE_IMAGE_DIR_META_KEY)
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
        return runCatching {
            gson.fromJson(json, WordDetailsResponse::class.java)
                .withResolvedOfflineImages(imageDirectory)
        }.getOrNull()
    }

    suspend fun deleteById(id: Int) {
        bookmarkDao.deleteById(id)
    }

    suspend fun deleteByIds(ids: List<Int>) {
        if (ids.isEmpty()) return
        bookmarkDao.deleteByIds(ids)
    }

    private fun BookmarkEntity.toSearchItem(): SearchItem {
        return SearchItem(
            id = id,
            kana = kana,
            writings = listOf(Writing(primaryWriting)),
            meaningSummary = meaningSummary
        )
    }

    private fun BookmarkEntity.toBookmarkItem(): BookmarkItem {
        return BookmarkItem(
            item = toSearchItem(),
            createdAt = createdAt
        )
    }
}
