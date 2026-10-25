# AniStream

![Build](https://github.com/amirulhassan1324-ux/AnimeXZ/actions/workflows/android.yml/badge.svg)

An anime streaming app for Android built with **Jetpack Compose**, **Material 3** and **Kotlin**. It pulls anime metadata from the AniList GraphQL API, plays video with Media3 ExoPlayer (resume position included), and keeps your list and watch progress in a local Room database.

## ⬇️ Download

**[Download the latest APK](https://github.com/amirulhassan1324-ux/AnimeXZ/releases/latest/download/AniStream.apk)**

1. Open the link on your Android phone and download `AniStream.apk`.
2. Tap the file to install. When Android asks, allow **Install unknown apps** for your browser or file manager.
3. Open **AniStream**.

The APK is rebuilt automatically by GitHub Actions on every push to `main`, so the link always points to the newest build. Every build is signed with the same debug key, so a new build installs over the old one without uninstalling.

> This is a **debug** build for testing, not a Play Store release.

You can also grab the APK from the **Actions** tab: open a run and download the `AniStream-debug-apk` artifact.

## Screens

| Screen | What it does |
| --- | --- |
| **Home** | Hero banner with the #1 trending title, Continue Watching row, Trending now, Most popular and Top rated rows. Loading and error states with retry. |
| **Search** | Debounced (350 ms) search by English or Romaji/Japanese title, shown in a 3-column grid. |
| **Detail** | Banner and poster, format / year / score / episode count / studio, genre chips, expandable synopsis, "Add to my list" toggle, episode list with watched-progress bars. |
| **Player** | ExoPlayer with `.mp4` and `.m3u8` (HLS) support, resume from last position, play/pause, ±10 s seek, seekbar, previous/next episode, playback speed (0.75x - 2.0x), auto-hiding controls. |
| **My List** | Saved anime in a 3-column grid, plus background new-episode alerts. |

### Episode release alerts

- `EpisodeUpdateWorker` (WorkManager) checks AniList every 6 hours when the device is online.
- When a saved anime has a new episode, `NotificationHelper` posts a heads-up notification on the `anime_episode_releases` channel (Android 13+ asks for the `POST_NOTIFICATIONS` permission).
- Tapping the notification opens the player directly on the new episode.
- My List has a manual "check now" button and a "Test Alert" button.

## Video source

All episode lookups go through one function, in two places that should be kept in sync:

- Android: `com.example.config.VideoConfig.getEpisodeSource(animeId, episode)`
- TypeScript: `src/config/video.ts`

**Right now it returns public sample videos** (Big Buck Bunny and similar, plus a Mux test HLS stream) so the player can be tested. It does not stream real anime episodes. To connect your own licensed streaming backend, change the URL logic:

```kotlin
fun getEpisodeSource(animeId: Int, episode: Int): VideoSource {
    return VideoSource(
        url = "https://your-licensed-backend.com/stream/$animeId/$episode.m3u8",
        type = "m3u8",
        quality = "1080p"
    )
}
```

Only stream content you have the rights to.

## Tech stack

- **UI:** Jetpack Compose, Material 3, Navigation Compose
- **Architecture:** MVVM with Kotlin Coroutines and `StateFlow`
- **Network:** OkHttp, Retrofit, Moshi, AniList GraphQL (`https://graphql.anilist.co`)
- **Images:** Coil
- **Video:** AndroidX Media3 (`exoplayer`, `ui`, `exoplayer-hls`)
- **Storage:** Room (`WatchlistDao`, `WatchProgressDao`)
- **Background work:** WorkManager

## Build it yourself

**On GitHub (no setup):** push to `main`. The workflow in `.github/workflows/android.yml` builds the debug APK and publishes it to the **Releases** page. You can also run it by hand from the **Actions** tab with **Run workflow**.

**Locally:**

Requirements: a recent Android Studio, JDK 17+, Android SDK 36.

```bash
# the build expects a debug keystore at the project root
cp ci/ci-debug.keystore debug.keystore

./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Unit tests: `./gradlew :app:testDebugUnitTest`

## Project notes

- `ci/ci-debug.keystore` is the shared **debug** signing key used by CI (the standard `android` debug password). It is only for debug builds. A Play Store release needs your own private keystore, supplied through the `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD` environment variables that the `release` signing config reads.
- `google-services.json` is not included. The build only warns about this because the project sets `googleServices.missing.passthrough=true`.
- `.env` is git-ignored. Defaults come from `.env.example`.
