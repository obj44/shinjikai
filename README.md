# Shinjikai Android Dictionary

[English](./README.md) | [العربية](./README.ar.md)

Japanese–Arabic dictionary for Android, built with Kotlin and Jetpack Compose. Dictionary data is indexed locally so search and word lookup work entirely offline.

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.shinjikai.dictionary"><img src="https://img.shields.io/badge/Google%20Play-Available-414141?logo=googleplay&logoColor=white&style=for-the-badge" alt="Get it on Google Play" height="40" /></a>
  <a href="#" aria-label="F-Droid coming soon"><img src="https://img.shields.io/badge/F--Droid-Coming%20soon-1976D2?logo=f-droid&logoColor=white&style=for-the-badge" alt="F-Droid coming soon" height="40" /></a>
</p>

## 📱 Screenshots

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-1.png" alt="Shinjikai search screen" width="260" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-2.png" alt="Shinjikai word details screen" width="260" />
</p>


> ⚠️ **Disclaimer**
>
> This is an independent project and is not officially affiliated with the Shinjikai website. The bundled dictionary data comes from [1Selxo/Shinjikai](https://github.com/1Selxo/Shinjikai), and credit for that data belongs to its original contributors.

## ✨ Features

- 🔎 Fast word search (Japanese and Arabic queries)
- 🧾 Detailed word screen with:
  - kana + kanji
  - JLPT level
  - category chips
  - Arabic definitions
  - related words section (synonyms, antonyms, and related links when available)
- 🔖 Bookmarks (save and manage words)
- 🕘 Recent searches
- 📦 Fully offline search with bundled data and local Yomitan dictionary imports
- 🃏 Anki exporter to send words and definitions from the app to Anki
- 🎨 Material 3 UI with dark/light theme support

## 🛠️ Tech Stack

- Kotlin
- Jetpack Compose (Material 3)
- Gson
- Room with FTS (local dictionary and bookmarks)
- Coroutines

## ▶️ Run the App

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Run the `app` configuration on an emulator or Android device.

## ⚙️ Notes

- Search runs locally against the bundled/imported Room FTS dictionary.
- Some fields, such as related links and categories, depend on bundled data availability per word.
- If no bundled assets are present, import a supported dictionary archive from the local dictionary settings screen.

## 🗂️ Project Structure

- `app/src/main/java/com/shinjikai/dictionary/` -> UI and app flow
- `app/src/main/java/com/shinjikai/dictionary/data/` -> dictionary models, repositories, importers, Room, and local search
- `app/src/main/res/` -> resources (strings, themes, icons, fonts)

## 🙌 Credits

- Dictionary data: **1Selxo/Shinjikai** (`https://github.com/1Selxo/Shinjikai`)
- Japanese deinflection transforms: **Yomitan** (`https://github.com/yomidevs/yomitan/tree/master/ext/js/language/ja`), GPL-3.0-or-later.

## 📦 Bundled Dictionary Assets

The app ships the `1Selxo/Shinjikai` dictionary locally. To refresh the bundled assets, run:

```powershell
.\scripts\fetch-bundled-dictionary.ps1
```

The script downloads `1Selxo/Shinjikai`, writes the data into `app/src/main/assets/bundled_dictionary/`, compresses `data_*.jsonl` as `.jsonl.xz`, recompresses images when it can make them smaller, and stores images as git-safe chunked `tar.xz` assets. On first launch the app imports the bundled JSONL into Room/FTS, extracts the bundled images into app storage, then all lookups run locally against SQLite indexes.
