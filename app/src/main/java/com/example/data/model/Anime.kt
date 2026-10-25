package com.example.data.model

data class Anime(
    val id: Int,
    val title: String,
    val englishTitle: String? = null,
    val nativeTitle: String? = null,
    val coverImage: String,
    val bannerImage: String? = null,
    val averageScore: Double = 0.0,
    val format: String = "TV",
    val episodes: Int? = null,
    val durationMinutes: Int? = 24,
    val seasonYear: Int? = null,
    val status: String = "RELEASING",
    val genres: List<String> = emptyList(),
    val description: String = "",
    val studio: String? = null
) {
    val displayScore: String
        get() = if (averageScore > 0) String.format("%.1f", averageScore) else "N/A"

    val displayYear: String
        get() = seasonYear?.toString() ?: "2026"
}

data class AnimeEpisode(
    val animeId: Int,
    val episodeNumber: Int,
    val title: String,
    val durationMinutes: Int = 24,
    val thumbnail: String? = null,
    val progressMs: Long = 0L,
    val durationMs: Long = 24 * 60 * 1000L
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
