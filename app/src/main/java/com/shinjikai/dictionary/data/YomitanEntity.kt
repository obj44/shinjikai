package com.shinjikai.dictionary.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "yomitan_terms",
    indices = [
        Index(value = ["expression"]),
        Index(value = ["reading"])
    ]
)
data class YomitanTermEntity(
    @PrimaryKey val id: Int,
    val expression: String,
    val reading: String,
    val glossary: String,
    val note: String = "",
    val source: String = "yomitan"
)

@Entity(tableName = "yomitan_meta")
data class YomitanMetaEntity(
    @PrimaryKey val key: String,
    val value: String
)
