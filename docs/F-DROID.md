## F-Droid submission guide for Shinjikai Dictionary

Package ID: `com.shinjikai.dictionary`

This project is close to F-Droid-compatible, but it is not submission-ready yet. The remaining work is mostly around repository metadata and review evidence, not large code changes.

### Current status

What already looks good:

- The app is built from Gradle source in this repository.
- Dependencies are fetched from `google()` and `mavenCentral()`.
- No Firebase, Google Play Services, ads SDKs, or analytics SDKs are present in the app module.
- The app can function in online mode using the public `https://shinjikai.app/` API.
- The application source is now licensed under MIT.
- The optional offline dictionary dataset is MIT-licensed.

What still blocks or may delay acceptance:

- F-Droid listing screenshots still need to be supplied.
- The offline dictionary import downloads a third-party ZIP from GitHub at runtime, so reviewers may still ask for a clear note about the dataset source and license.

### Hard requirements before submitting

1. Add screenshots.
   Put at least two phone screenshots under:

   - `fastlane/metadata/android/en-US/images/phoneScreenshots/`

2. Tag a public release in GitHub.
   F-Droid usually packages from a tagged source release. The tag should match the app version in `app/build.gradle.kts`.

### Suggested review notes for F-Droid

When you open the F-Droid inclusion request, include notes like these:

- The app has no nonfree SDK dependencies.
- Network access is used for the public Shinjikai dictionary API.
- Offline import is user-initiated from the Settings screen.
- The offline import downloads a third-party dictionary archive only when the user requests it.
- The optional offline dictionary dataset is MIT-licensed.
- The offline dataset currently referenced by the app is sourced from:
  `https://github.com/a-hamdi/japanesearabic`

### Basic submission flow

1. Push the repository to a public GitHub repo.
2. Add screenshots.
3. Create a signed Git tag for the release, for example `v1.1`.
4. Verify the project builds cleanly from source with Gradle.
5. Open an inclusion request in the F-Droid Data repository.
6. In the request, provide:
   - source repo URL
   - package ID `com.shinjikai.dictionary`
   - app license: MIT
   - build instructions if reviewers need anything unusual
   - explanation of the optional offline dictionary import
   - note that the referenced offline dataset is MIT-licensed

### Recommended next improvement

If you want the smoothest F-Droid review, replace the hardcoded third-party dictionary download URL with a document picker import flow. That removes the biggest remaining review ambiguity in the current app.