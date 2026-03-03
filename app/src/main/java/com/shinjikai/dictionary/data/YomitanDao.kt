package com.shinjikai.dictionary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface YomitanDao {
    @Query(
        """
        SELECT * FROM yomitan_terms
        WHERE expression LIKE '%' || :term || '%'
            OR reading LIKE '%' || :term || '%'
            OR glossary LIKE '%' || :term || '%'
        ORDER BY
            CASE
                WHEN expression = :term THEN 0
                WHEN reading = :term THEN 1
                WHEN expression LIKE :prefix THEN 2
                WHEN reading LIKE :prefix THEN 3
                ELSE 4
            END,
            id ASC
        LIMIT :limit
        """
    )
    suspend fun search(term: String, prefix: String, limit: Int = 80): List<YomitanTermEntity>

    @Query(
        """
        SELECT * FROM yomitan_terms
        WHERE glossary LIKE '%' || :term || '%'
           OR glossary LIKE '%' || :normalizedTerm || '%'
        ORDER BY id ASC
        LIMIT :limit
        """
    )
    suspend fun searchArabic(
        term: String,
        normalizedTerm: String,
        limit: Int = 2500
    ): List<YomitanTermEntity>

    @Query("SELECT * FROM yomitan_terms WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): YomitanTermEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<YomitanTermEntity>)

    @Query("DELETE FROM yomitan_terms")
    suspend fun clearTerms()

    @Query("SELECT COUNT(*) FROM yomitan_terms")
    suspend fun countTerms(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: YomitanMetaEntity)

    @Query("SELECT value FROM yomitan_meta WHERE key = :key LIMIT 1")
    suspend fun getMetaValue(key: String): String?
}
