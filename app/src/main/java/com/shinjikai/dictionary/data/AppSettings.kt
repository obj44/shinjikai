package com.shinjikai.dictionary.data

/**
 * Lightweight UI settings persisted by [SettingsStore].
 */
enum class AppThemeMode(val storageKey: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromStorageKey(value: String?): AppThemeMode? {
            return values().firstOrNull { it.storageKey == value }
        }
    }
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.Dark,
    val useDynamicColor: Boolean = false,
    val useOfflineMode: Boolean = true,
    val hasSeenIntroduction: Boolean = false,
    val selectedAnkiDeckName: String = "Shinjikai"
) {
    val darkMode: Boolean
        get() = themeMode == AppThemeMode.Dark
}
