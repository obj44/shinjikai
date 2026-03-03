package com.shinjikai.dictionary.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BookmarkEntity::class, YomitanTermEntity::class, YomitanMetaEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun yomitanDao(): YomitanDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS yomitan_terms (
                        id INTEGER NOT NULL PRIMARY KEY,
                        expression TEXT NOT NULL,
                        reading TEXT NOT NULL,
                        glossary TEXT NOT NULL,
                        note TEXT NOT NULL,
                        source TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_yomitan_terms_expression ON yomitan_terms(expression)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_yomitan_terms_reading ON yomitan_terms(reading)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS yomitan_meta (
                        key TEXT NOT NULL PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shinjikai.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
