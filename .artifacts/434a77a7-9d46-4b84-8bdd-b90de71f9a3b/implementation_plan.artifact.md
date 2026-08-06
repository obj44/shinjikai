# Fix Room Database Migration Crash (11 to 9)

The app is currently crashing because the local database (`shinjikai.db`) is at version 11, while the code expects version 9. This is likely due to the user having previously installed a version of the app with a higher database version (e.g., from a different branch or a retracted update).

## User Review Required

> [!IMPORTANT]
> The proposed fix involves bumping the database version to 12 and providing a migration path. This ensures that users on version 11 (the crashing state) and users on version 9 (the current state) can both move forward safely.
>
> I am assuming that versions 10 and 11 did not introduce breaking changes to the `bookmarks` table that cannot be handled by a "repair" approach.

## Proposed Changes

### Database Component

#### [MODIFY] [AppDatabase.kt](file:///C:/Users/obj/shinjikai.app/shinjikai.app/app/src/main/java/com/shinjikai/dictionary/data/AppDatabase.kt)

- Bump `version` from 9 to 12.
- Add `MIGRATION_9_10`, `MIGRATION_10_11`, and `MIGRATION_11_12`.
- These migrations will be "defensive":
    - For dictionary tables (`yomitan_terms`, etc.), they will use `repairOfflineDictionarySchema` to ensure the schema is correct.
    - For the `bookmarks` table, they will check for expected columns and add them if they are missing.
- Add `.fallbackToDestructiveMigrationOnDowngrade()` to the `Room.databaseBuilder` to prevent future crashes if the version is ever downgraded again.

## Verification Plan

### Automated Tests
- I will attempt to run the existing database-related tests to ensure no regressions.
- `gradle_build(":app:assembleDebug")` to ensure everything compiles.

### Manual Verification
- The user should verify that the app no longer crashes on startup.
- If possible, verify that bookmarks are preserved (this would require a test device/emulator with the version 11 database).
