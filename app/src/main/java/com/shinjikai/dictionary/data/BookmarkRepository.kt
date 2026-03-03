package com.shinjikai.dictionary.data

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {
    suspend fun getAll(): List<SearchItem> {
        return bookmarkDao.getAll().map { entity ->
            SearchItem(
                id = entity.id,
                kana = entity.kana,
                writings = listOf(Writing(entity.primaryWriting)),
                meaningSummary = entity.meaningSummary
            )
        }
    }

    suspend fun upsert(item: SearchItem) {
        bookmarkDao.upsert(
            BookmarkEntity(
                id = item.id,
                primaryWriting = item.primaryWriting,
                kana = item.kana,
                meaningSummary = item.meaningSummary
            )
        )
    }

    suspend fun deleteById(id: Int) {
        bookmarkDao.deleteById(id)
    }
}
