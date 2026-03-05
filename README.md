# 📘 Shinjikai Android Dictionary

Android app (Kotlin + Jetpack Compose) for Japanese → Arabic dictionary lookup using the Shinjikai API:  
`https://shinjikai.app`

## ✨ Features

- 🔎 Fast word search (Japanese and Arabic queries)
- 🧾 Detailed word screen with:
  - kana + kanji
  - JLPT level
  - category chips
  - Arabic definitions
  - related words section (synonyms/antonyms/related links when available)
- 🔖 Bookmarks (save and manage words)
- 🕘 Recent searches
- 🌐 Online mode (Shinjikai RPC API)
- 📦 Offline mode with local Yomitan dictionary import
- 🎨 Material 3 UI with dark/light theme support

## 🧠 API Endpoints Used

- `POST /rpc/SearchWords`
- `POST /rpc/LoadWordDetails`
- `POST /rpc/LoadCategories`

Request headers include `X-Client-Id` as required by the backend.

## 🛠️ Tech Stack

- Kotlin
- Jetpack Compose (Material 3)
- Retrofit + OkHttp + Gson
- Room (local database for offline dictionary + bookmarks)
- Coroutines

## ▶️ Run the App

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Run the `app` configuration on an emulator or Android device.

## ⚙️ Notes

- Search currently uses default API mode (`Mode = 0`).
- Some fields (for example related links/categories) depend on API data availability per word.
- In offline mode, data comes from local imported dictionary and may differ from online details.

## 🗂️ Project Structure (High Level)

- `app/src/main/java/com/shinjikai/dictionary/` → UI and app flow
- `app/src/main/java/com/shinjikai/dictionary/data/` → API models, repository, Room, offline source
- `app/src/main/res/` → resources (strings, themes, icons, fonts)

## 🙌 Credits

- Dictionary data and API: **Shinjikai** (`https://shinjikai.app`)
- Offline source archive default URL points to the `a-hamdi/japanesearabic` dataset used by the app importer.
