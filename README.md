# AniStream - Anime Streaming App

AniStream is a high-performance anime streaming application built with **Jetpack Compose**, **Material 3**, and **Kotlin** for Android, featuring AniList GraphQL integration, custom ExoPlayer video streaming with resume position, and local Room database persistence.

---

## 📱 Features & Screens

1. **Home Screen**
   - **Hero Banner:** Featured anime with #1 Trending badge, rating, Play button, and Details button.
   - **Continue Watching:** Horizontal row displaying in-progress anime with episode indicators, visual progress bars, and single-tap resume.
   - **Trending Now:** Popular and trending anime titles updated from AniList GraphQL API.
   - **Most Popular & Top Rated:** Categorized rows with ratings, cover art, and titles.
   - **Loading & Error Handling:** Skeleton states and retry mechanics.

2. **Search Screen**
   - **Debounced Search:** Reactive 350ms debounced queries querying AniList titles in English and Romaji/Japanese.
   - **3-Column Results Grid:** Clean layout matching the design specifications.
   - **Empty & Idle States:** Polished purple iconography and helpful search cues.

3. **Detail Screen**
   - **Cinematic Banner & Poster:** High-resolution backdrops with smooth gradient fades.
   - **Metadata:** Format (TV/Movie/ONA), Release year, average community score, episode count, and animation studio.
   - **Watchlist Toggle:** One-tap "Add to my list" with local Room database persistence.
   - **Expandable Synopsis:** Clean typography with "Read more / Show less" toggle.
   - **Genre Chips:** Styled purple pill chips.
   - **Episode List:** Episode thumbnails, episode numbers, runtime, watched progress bars, and direct playback buttons.

4. **Video Player Screen**
   - **ExoPlayer (Media3):** Support for both `.mp4` and `.m3u8` (HLS adaptive bitrate) video streams.
   - **Watch Progress & Resume:** Automatically saves playback timestamp and resumes from the last position.
   - **Playback Controls:** Play/Pause, 10s Rewind, 10s Fast-Forward, interactive seekbar with current and total duration timestamps.
   - **Episode Navigation:** Next Episode and Previous Episode buttons.
   - **Playback Speed Selector:** 0.75x, 1.0x, 1.25x, 1.5x, 2.0x speeds.
   - **Auto-Hiding Controls:** Immersive viewing experience with tap-to-reveal controls.

5. **My List (Watchlist) Screen & Episode Release Alerts**
   - **Saved Anime Grid:** 3-column responsive grid showing user's saved titles with item counter ("N saved").
   - **Background Episode Monitor:** Background task (WorkManager) checks for new episode airings every 6 hours with network connectivity constraints.
   - **Local Notifications:** Dispatches high-priority heads-up notifications when a saved anime has a new episode released.
   - **Direct Play Launch:** Tapping on a release notification opens the app directly into the video player for that episode.
   - **Manual & Test Controls:** Includes an on-demand update check button and a "Test Alert" simulation trigger in My List.
   - **Empty State:** Matching custom empty state with "Browse trending" shortcut.

---

## 🔔 Local Notification & Background Task Details

- **WorkManager Task:** `com.example.notification.EpisodeUpdateWorker` runs periodically in the background (configured for every 6 hours) with `NetworkType.CONNECTED` constraint.
- **Notification Channel:** `NotificationHelper.CHANNEL_ID = "anime_episode_releases"` (High importance, vibration, purple accent light).
- **Permissions:** Android 13+ `POST_NOTIFICATIONS` runtime permission with in-app permission requester.
- **Deep Linking:** Launching from a notification passes `EXTRA_ANIME_ID` and `EXTRA_EPISODE_NUMBER`, routing the player immediately to the newly released episode.


## 🎬 Video Source Configuration

Video lookup is centralized in:
- **Kotlin (Android):** `com.example.config.VideoConfig.getEpisodeSource(animeId, episode)`
- **TypeScript:** `src/config/video.ts` (`getEpisodeSource(animeId, episode)`)

To connect your licensed streaming backend, adjust the stream URL logic in `src/config/video.ts` and `VideoConfig.kt`:
```kotlin
fun getEpisodeSource(animeId: Int, episode: Int): VideoSource {
    return VideoSource(
        url = "https://your-licensed-backend.com/stream/$animeId/$episode.m3u8",
        type = "m3u8",
        quality = "1080p"
    )
}
```

---

## 🛠️ Architecture & Tech Stack

- **UI:** Jetpack Compose + Material Design 3
- **Network & API:** OkHttp, AniList GraphQL API (`https://graphql.anilist.co`)
- **Image Loading:** Coil Compose
- **Video Player:** AndroidX Media3 ExoPlayer (`media3-exoplayer`, `media3-ui`, `media3-exoplayer-hls`)
- **Local Persistence:** AndroidX Room Database (`WatchlistDao`, `WatchProgressDao`)
- **Navigation:** Navigation Compose (`NavHost`)
- **State Management:** MVVM with Kotlin Coroutines & `StateFlow`

---

## 🚀 Setup & Build Steps

1. **Prerequisites:**
   - Android Studio Ladybug (2024.2+) or higher
   - JDK 17 / 21
   - Android SDK 35/36

2. **Run on Android Device or Emulator:**
   ```bash
   gradle assembleDebug
   ```
   Or click **Run** in Android Studio.

3. **Run Unit Tests:**
   ```bash
   gradle :app:testDebugUnitTest
   ```
