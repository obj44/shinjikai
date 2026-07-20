package com.shinjikai.dictionary.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BookmarkEntity::class,
        YomitanTermEntity::class,
        YomitanTermFtsEntity::class,
        YomitanMetaEntity::class,
        YomitanTermCategoryEntity::class
    ],
    // Keep this >= the highest version that has ever shipped, otherwise existing installs may crash on downgrade.
    version = 10,
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
                        difficulty INTEGER NOT NULL DEFAULT 0,
                        note TEXT NOT NULL,
                        source TEXT NOT NULL,
                        detailsJson TEXT
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS yomitan_terms_fts USING fts4(expression, reading, glossary)"
                )
                // Backfill from existing imported terms (if any).
                db.execSQL(
                    """
                    INSERT INTO yomitan_terms_fts(rowid, expression, reading, glossary)
                    SELECT id, expression, reading, glossary
                    FROM yomitan_terms
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema changes; bump only to avoid crashing users that already have a v4 database.
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // We don't have the exact v4 schema in git history (it likely came from a local build),
                // so be defensive:
                // - Try to preserve bookmarks by copying whatever columns exist into a backup table.
                // - Recreate dictionary tables to match current entities (offline data can be re-imported).
                val nowEpochMsExpr = "(CAST(strftime('%s','now') AS INTEGER) * 1000)"

                val hasBookmarks = tableExists(db, "bookmarks")
                var canRestoreBookmarks = false
                var bookmarkBackupCols: Set<String> = emptySet()

                if (hasBookmarks) {
                    // Backup the entire table as-is, regardless of its schema.
                    db.execSQL("DROP TABLE IF EXISTS bookmarks_backup")
                    db.execSQL("CREATE TABLE bookmarks_backup AS SELECT * FROM bookmarks")
                    bookmarkBackupCols = getTableColumns(db, "bookmarks_backup")
                    canRestoreBookmarks = "id" in bookmarkBackupCols
                }

                // Drop any existing tables we will recreate. Use IF EXISTS to tolerate unknown v4 schemas.
                db.execSQL("DROP TABLE IF EXISTS yomitan_terms_fts")
                db.execSQL("DROP TABLE IF EXISTS yomitan_terms")
                db.execSQL("DROP TABLE IF EXISTS yomitan_meta")
                db.execSQL("DROP TABLE IF EXISTS bookmarks")

                // Recreate schema expected by the current entities.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bookmarks (
                        id INTEGER NOT NULL PRIMARY KEY,
                        primaryWriting TEXT NOT NULL,
                        kana TEXT NOT NULL,
                        meaningSummary TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS yomitan_terms (
                        id INTEGER NOT NULL PRIMARY KEY,
                        expression TEXT NOT NULL,
                        reading TEXT NOT NULL,
                        glossary TEXT NOT NULL,
                        difficulty INTEGER NOT NULL DEFAULT 0,
                        note TEXT NOT NULL,
                        source TEXT NOT NULL,
                        detailsJson TEXT
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

                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS yomitan_terms_fts USING fts4(expression, reading, glossary)"
                )

                // Restore bookmarks if we managed to back them up.
                if (canRestoreBookmarks) {
                    val selectPrimary = if ("primaryWriting" in bookmarkBackupCols) "primaryWriting" else "''"
                    val selectKana = if ("kana" in bookmarkBackupCols) "kana" else "''"
                    val selectMeaning = if ("meaningSummary" in bookmarkBackupCols) "meaningSummary" else "''"
                    val selectCreatedAt = if ("createdAt" in bookmarkBackupCols) "createdAt" else nowEpochMsExpr

                    db.execSQL(
                        """
                        INSERT OR REPLACE INTO bookmarks(id, primaryWriting, kana, meaningSummary, createdAt)
                        SELECT id, $selectPrimary, $selectKana, $selectMeaning, $selectCreatedAt
                        FROM bookmarks_backup
                        """.trimIndent()
                    )
                }

                db.execSQL("DROP TABLE IF EXISTS bookmarks_backup")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableExists(db, "bookmarks")) return
                val cols = getTableColumns(db, "bookmarks")
                if ("detailsJson" !in cols) {
                    db.execSQL("ALTER TABLE bookmarks ADD COLUMN detailsJson TEXT")
                }
                if ("detailsSavedAt" !in cols) {
                    db.execSQL("ALTER TABLE bookmarks ADD COLUMN detailsSavedAt INTEGER")
                }
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableExists(db, "yomitan_terms")) return
                val cols = getTableColumns(db, "yomitan_terms")
                if ("detailsJson" !in cols) {
                    db.execSQL("ALTER TABLE yomitan_terms ADD COLUMN detailsJson TEXT")
                }
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS yomitan_term_categories (
                        termId INTEGER NOT NULL,
                        categoryId INTEGER NOT NULL,
                        PRIMARY KEY(termId, categoryId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_yomitan_term_categories_categoryId ON yomitan_term_categories(categoryId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_yomitan_term_categories_termId ON yomitan_term_categories(termId)"
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!tableExists(db, "yomitan_terms")) return
                val cols = getTableColumns(db, "yomitan_terms")
                if ("difficulty" !in cols) {
                    db.execSQL("ALTER TABLE yomitan_terms ADD COLUMN difficulty INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_yomitan_terms_browse ON yomitan_terms(reading, expression, id)"
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
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun repairOfflineDictionarySchema(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS yomitan_terms (
                    id INTEGER NOT NULL PRIMARY KEY,
                    expression TEXT NOT NULL,
                    reading TEXT NOT NULL,
                    glossary TEXT NOT NULL,
                    difficulty INTEGER NOT NULL DEFAULT 0,
                    note TEXT NOT NULL,
                    source TEXT NOT NULL,
                    detailsJson TEXT
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS yomitan_meta (
                    key TEXT NOT NULL PRIMARY KEY,
                    value TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS yomitan_term_categories (
                    termId INTEGER NOT NULL,
                    categoryId INTEGER NOT NULL,
                    PRIMARY KEY(termId, categoryId)
                )
                """.trimIndent()
            )

            val termColumns = getTableColumns(db, "yomitan_terms")
            if ("difficulty" !in termColumns) {
                db.execSQL("ALTER TABLE yomitan_terms ADD COLUMN difficulty INTEGER NOT NULL DEFAULT 0")
            }
            if ("detailsJson" !in termColumns) {
                db.execSQL("ALTER TABLE yomitan_terms ADD COLUMN detailsJson TEXT")
            }

            db.execSQL("CREATE INDEX IF NOT EXISTS index_yomitan_terms_expression ON yomitan_terms(expression)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_yomitan_terms_reading ON yomitan_terms(reading)")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_yomitan_terms_browse ON yomitan_terms(reading, expression, id)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_yomitan_term_categories_categoryId ON yomitan_term_categories(categoryId)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_yomitan_term_categories_termId ON yomitan_term_categories(termId)"
            )

            db.execSQL(
                "CREATE VIRTUAL TABLE IF NOT EXISTS yomitan_terms_fts USING fts4(expression, reading, glossary)"
            )
        }

        private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean {
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$table'").use { cursor ->
                return cursor.moveToFirst()
            }
        }

        private fun getTableColumns(db: SupportSQLiteDatabase, table: String): Set<String> {
            val result = LinkedHashSet<String>()
            db.query("PRAGMA table_info(`$table`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0) {
                        cursor.getString(nameIndex)?.let(result::add)
                    }
                }
            }
            return result
        }
    }
}
