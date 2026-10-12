# YTLiveStreaming for Android

An example of using the [YouTube Live Streaming API v3](https://developers.google.com/youtube/v3/live/docs) on Android (Kotlin).

> **Maintenance status:** the project was brought back to a buildable and runnable state in September 2026 and moved to the current Android toolchain in October 2026 (see [What's new](#whats-new-october-2026)). It is provided as a sample and is no longer actively developed.

## Technologies & Libraries Used

- [Google APIs Client Library for Java](https://github.com/googleapis/google-api-java-client-services) — YouTube Data API v3
- [Google Sign-In](https://developers.google.com/identity/sign-in/android/sign-in)
- MVVM Architecture
- Kotlin Coroutines (asynchronous operations)
- [YouTube Android Player API](https://developers.google.com/youtube/android/player)

## Requirements

| | Version |
|---|---|
| Android Studio | 2026.1 or newer |
| JDK for Gradle | 17 or newer (the JetBrains Runtime bundled with Android Studio works) |
| Gradle | 9.8.1 |
| Android Gradle Plugin | 9.4.1 (built-in Kotlin support) |
| Kotlin | 2.4.20 |
| compileSdk / minSdk / targetSdk | 37 / 26 / 36 |

Dependency versions are kept in the version catalog [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Getting Started

1. [Enable YouTube Live Streaming for your channel](https://support.google.com/youtube/answer/2474026?hl=en).
2. In the [Google Cloud Console](https://console.cloud.google.com):
   1. Enable the **YouTube Data API v3** (APIs & Services → Library).

      ![Screen Shot 2020-10-06 at 8 48 27 AM](https://user-images.githubusercontent.com/2775621/95163961-b6a7fb00-07b1-11eb-9b06-42fef871cb2f.png)

   2. Configure the **OAuth consent screen**. While the app is in *Testing* mode, add your Google account to **Test users** — the app requests the `youtube` scope, which is a sensitive scope.
   3. Create an **OAuth client ID** of type **Android**:
      - Package name: `com.skdev.ytlivevideo`
      - SHA-1: the fingerprint of your signing key. For the debug key run `./gradlew signingReport` in the project folder and copy the `SHA1` of the `debug` variant.

      The client ID does not need to be added to the code — Google Play services match the app by package name and SHA-1.

      ![Screen Shot 2020-10-06 at 8 45 37 AM](https://user-images.githubusercontent.com/2775621/95163944-abed6600-07b1-11eb-8e4e-c9cd1693e4a6.png)
      ![Screen Shot 2020-10-06 at 8 52 25 AM](https://user-images.githubusercontent.com/2775621/95163976-bc9ddc00-07b1-11eb-96ee-5540d0ab3d34.png)

   4. Create an **API key** (APIs & Services → Credentials). It is used by the YouTube player.
3. Add the API key to the project:
   1. Copy `app/src/main/java/com/skdev/ytlivevideo/util/Credentilals_.kt` to `Credentials.kt` in the same folder.
   2. In `Credentials.kt` rename `object Credentials_` to `object Credentials` and replace `"API Key"` with your key.

   `Credentials.kt` is listed in `.gitignore`, so your key is not committed.
4. Build and run. On the first sign-in Google asks for permission to manage your YouTube account. If the app is not verified by Google, choose **Advanced → Go to … (unsafe)** to continue.

## What's new (October 2026)

**Build**
- Gradle **9.8.1**, Android Gradle Plugin **9.4.1**, Kotlin **2.4.20**. The project now syncs with the JDK bundled with current Android Studio (Gradle 8.13 could not run on it).
- Build scripts converted to the Kotlin DSL (`*.gradle.kts`) with a version catalog; repositories are declared in `settings.gradle.kts`.
- AGP's built-in Kotlin replaces the `kotlin-android` plugin; `kapt`, Jetifier, MultiDex (not needed with minSdk 26) and the unused RxJava, Gson and `lifecycle-compiler` dependencies were removed.
- All AndroidX, Material, Coroutines and Google API client libraries updated; the abandoned `com.mcxiaoke.volley` fork replaced with the official `com.android.volley`.
- `play-services-auth` stays on 21.x: version 22 removed the Google Sign-In API this app uses.
- Gradle build cache and configuration cache are enabled.

**Android 15/16 (targetSdk 36)**
- Edge-to-edge: activity content is padded with the system bar insets, status bar icons are dark on light screens.
- The Back gesture on the main screen is handled with `OnBackPressedDispatcher` (`onBackPressed()` is no longer called with predictive back).
- The streaming notification uses a notification channel and an immutable `PendingIntent` (a mutable one crashes on Android 12+); `POST_NOTIFICATIONS` is declared.

**Code**
- `startActivityForResult`/`onActivityResult` replaced with the Activity Result API (sign-in, account picker, authorization, Google Play services dialog, streaming screen).
- `AsyncTask` replaced with a coroutine; `Handler()` in the splash screen replaced with `lifecycleScope`; other deprecated calls (`onAttach(Activity)`, `capitalize`, `FLAG_FULLSCREEN`, `overridePendingTransition`) updated.

## What's new (September 2026)

The 2020 version no longer built with current tools and crashed at runtime. The following was changed:

**Build**
- Gradle wrapper updated to **8.13**, Android Gradle Plugin to **8.13.2**; the project builds with **JDK 17**.
- Added `META-INF/INDEX.LIST` to the packaging excludes (duplicate file in `google-auth-library` jars).

**YouTube Data API**
- `google-api-services-youtube` updated from `v3-rev120-1.19.0` (2015) to **`v3-rev20260902-2.0.0`**. The old library crashed on start with `ExceptionInInitializerError` because it is incompatible with `google-api-client` 2.x.
- Code adapted to the new client API: the `part` and `id` parameters are now `List<String>` (`list(listOf("id", "snippet", …))`, `id = listOf(broadcastId)`).
- New live streams are created with `cdn.resolution = "variable"` and `cdn.frameRate = "variable"` instead of the deprecated `cdn.format`.
- Fixed: deleting a broadcast built the request but never executed it.

**Sign-in and authorization**
- Google Sign-In now requests the YouTube scope at sign-in, so the consent screen is shown right away.
- Fixed the `NeedRemoteConsent` error: `UserRecoverableAuthIOException` was re-wrapped into a plain `IOException`, which lost the consent intent. It is now passed through and the app opens the Google consent screen.

**Removed**
- The Google+ **+1 button** (`PlusOneButton`) from the broadcast list and the `play-services-plus` dependency — Google+ was shut down, and the button crashed the list on modern devices.
- The unused `youtubeapidemo` sample package (demo activities and layouts from the YouTube Android Player API samples). `YouTubeFailureRecoveryActivity`, which the player screen still needs, moved to `ui/youtubePlayer` and now uses `Credentials.API_KEY`.

## Known Limitations

- **FFmpeg streaming from the device camera does not work.** The native library in `app/src/main/jni` is not built by Gradle, and its JNI function names belong to the original Google sample (`com.google.android.apps.watchme`). Creating and managing broadcasts works; to actually stream, use an external encoder (for example OBS) with the stream URL and key from YouTube Studio, or replace the native code with a maintained RTMP library.
- **Deprecated Google SDKs.** The [YouTube Android Player API](https://developers.google.com/youtube/android/player) is no longer supported by Google, and the Google Sign-In API used here is deprecated in favor of Credential Manager and `AuthorizationClient` (it was removed in `play-services-auth` 22, so the library is kept on 21.x).
- The main screen still uses `ViewPager` with the deprecated `FragmentPagerAdapter`; the camera code uses the deprecated `android.hardware.Camera` API.
- The *Scheduled* label in the broadcast list shows the publish date, not the scheduled start time.

## Demo Video

1. Create a new stream on your YouTube account:

   ![v2](https://user-images.githubusercontent.com/2775621/95176102-0f34c380-07c5-11eb-99bf-84e38c6fe781.gif)

2. View broadcasts across different lifecycle states (All, Upcoming, Active, Completed) and watch active streams:

   ![v5](https://user-images.githubusercontent.com/2775621/95176316-62a71180-07c5-11eb-8565-71baae59234f.gif)

## iOS Implementation

An iOS implementation using the same API is available at [YTLiveStreaming for iOS](https://github.com/SKrotkih/YTLiveStreaming).
