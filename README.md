# Shinjikai Android Dictionary

Android app (Kotlin + Jetpack Compose) that uses `https://shinjikai.app` RPC API for online Japanese -> Arabic dictionary lookup.

## API methods used

- `POST /rpc/SearchWords`
- `POST /rpc/LoadWordDetails`

## Build

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Run the `app` configuration on an emulator/device.

## Notes

- The app sends `X-Client-Id` as required by the API.
- Search mode is set to default (`Mode = 0`).
