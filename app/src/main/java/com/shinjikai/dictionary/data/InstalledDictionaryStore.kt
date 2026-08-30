package com.shinjikai.dictionary.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class InstalledDictionaryRecord(
    val id: String,
    val name: String,
    val source: String,
    val fileName: String? = null,
    val sourceKey: String? = null,
    val enabled: Boolean = true
)

/** Persists the dictionary inventory independently from the active search index. */
class InstalledDictionaryStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val type = object : TypeToken<List<InstalledDictionaryRecord>>() {}.type

    fun read(): List<InstalledDictionaryRecord> = runCatching {
        gson.fromJson<List<InstalledDictionaryRecord>>(prefs.getString(KEY, null), type).orEmpty()
    }.getOrDefault(emptyList())

    fun add(record: InstalledDictionaryRecord) {
        val updated = (read().filterNot { it.id == record.id } + record)
        prefs.edit().putString(KEY, gson.toJson(updated)).apply()
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val updated = read().map { record ->
            if (record.id == id) record.copy(enabled = enabled) else record
        }
        prefs.edit().putString(KEY, gson.toJson(updated)).apply()
    }

    fun remove(id: String) {
        val updated = read().filterNot { record -> record.id == id }
        prefs.edit().putString(KEY, gson.toJson(updated)).apply()
    }

    private companion object {
        const val PREFS_NAME = "installed_dictionaries"
        const val KEY = "records"
    }
}
