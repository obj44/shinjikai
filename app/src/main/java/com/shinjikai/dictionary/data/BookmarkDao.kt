package com.shinjikai.dictionary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    suspend fun getAll(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BookmarkEntity)

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun pagingSource(): PagingSource<Int, BookmarkEntity>

    @Query("SELECT id FROM bookmarks")
    fun observeIds(): Flow<List<Int>>

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Int)
}
