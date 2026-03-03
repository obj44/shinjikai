package com.shinjikai.dictionary.data

import androidx.room.withTransaction
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream

class YomitanImporter(
    private val database: AppDatabase
) {
    private val yomitanDao = database.yomitanDao()

    suspend fun importFromZip(zipStream: InputStream, sourceLabel: String = "yomitan-v2"): Result<Int> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val allEntries = parseYomitanZip(zipStream, sourceLabel)
                database.withTransaction {
                    yomitanDao.clearTerms()
                    allEntries.chunked(800).forEach { chunk ->
                        yomitanDao.upsertAll(chunk)
                    }
                    yomitanDao.upsertMeta(
                        YomitanMetaEntity(
                            key = "last_import_source",
                            value = sourceLabel
                        )
                    )
                    yomitanDao.upsertMeta(
                        YomitanMetaEntity(
                            key = "last_import_count",
                            value = allEntries.size.toString()
                        )
                    )
                    yomitanDao.upsertMeta(
                        YomitanMetaEntity(
                            key = "last_import_epoch_ms",
                            value = System.currentTimeMillis().toString()
                        )
                    )
                }
                allEntries.size
            }
        }
    }

    private fun parseYomitanZip(zipStream: InputStream, sourceLabel: String): List<YomitanTermEntity> {
        val terms = ArrayList<YomitanTermEntity>(40_000)
        var idSeed = 1
        ZipInputStream(zipStream).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                val isTermBank = entry.name.lowercase(Locale.ROOT).startsWith("term_bank_")
                if (!isTermBank || !entry.name.endsWith(".json")) {
                    zis.closeEntry()
                    continue
                }

                val entryJson = zis.readBytes().toString(Charsets.UTF_8)
                val root = JsonParser.parseString(entryJson)
                if (!root.isJsonArray) {
                    zis.closeEntry()
                    continue
                }
                val rows = root.asJsonArray
                for (raw in rows) {
                    val parsed = parseTermRow(raw, idSeed, sourceLabel)
                    if (parsed != null) {
                        terms.add(parsed)
                        idSeed += 1
                    }
                }
                zis.closeEntry()
            }
        }
        return terms
    }

    private fun parseTermRow(
        raw: JsonElement,
        id: Int,
        sourceLabel: String
    ): YomitanTermEntity? {
        if (!raw.isJsonArray) return null
        val row = raw.asJsonArray
        if (row.size() < 6) return null

        val expression = row.getOrNull(0).asSafeString().trim()
        val reading = row.getOrNull(1).asSafeString().trim()
        val definitionElement = row.getOrNull(5)

        if (expression.isBlank() && reading.isBlank()) return null
        val glossary = extractText(definitionElement)
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
            .ifBlank { "-" }

        return YomitanTermEntity(
            id = id,
            expression = expression.ifBlank { reading },
            reading = reading.ifBlank { expression },
            glossary = glossary,
            note = "",
            source = sourceLabel
        )
    }

    private fun extractText(element: JsonElement?): String {
        if (element == null || element.isJsonNull) return ""
        return when {
            element.isJsonPrimitive -> element.asString
            element.isJsonArray -> {
                element.asJsonArray.mapNotNull { child ->
                    extractText(child).takeIf { it.isNotBlank() }
                }.joinToString(separator = "\n")
            }
            element.isJsonObject -> {
                val obj: JsonObject = element.asJsonObject
                if (obj.has("content")) {
                    extractText(obj.get("content"))
                } else {
                    obj.entrySet().mapNotNull { (_, value) ->
                        extractText(value).takeIf { it.isNotBlank() }
                    }.joinToString(separator = "\n")
                }
            }
            else -> ""
        }
    }

    private fun JsonElement?.asSafeString(): String {
        if (this == null || this.isJsonNull) return ""
        return runCatching { this.asString }.getOrDefault("")
    }

    private fun JsonArray.getOrNull(index: Int): JsonElement? {
        return if (index in 0 until size()) get(index) else null
    }
}
