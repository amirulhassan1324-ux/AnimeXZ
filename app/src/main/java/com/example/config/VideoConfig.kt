package com.example.config

data class VideoSource(
    val url: String,
    val type: String, // "mp4" or "m3u8"
    val quality: String = "1080p"
)

object VideoConfig {
    // Custom backend URL template: can be adjusted to point to any licensed streaming backend
    private const val BACKEND_STREAM_BASE = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample"
    private const val HLS_STREAM_URL = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

    private val sampleSources = listOf(
        "$BACKEND_STREAM_BASE/BigBuckBunny.mp4",
        "$BACKEND_STREAM_BASE/ElephantsDream.mp4",
        "$BACKEND_STREAM_BASE/TearsOfSteel.mp4",
        "$BACKEND_STREAM_BASE/Sintel.mp4",
        "$BACKEND_STREAM_BASE/SubaruOutbackSeeTheWorld.mp4"
    )

    /**
     * Primary lookup function returning an .mp4 or .m3u8 URL from a licensed backend.
     */
    fun getEpisodeSource(animeId: Int, episode: Int): VideoSource {
        // Episode 1 on even anime IDs can test HLS adaptive streaming (.m3u8), while others use high-res .mp4
        val isHls = (animeId + episode) % 5 == 0
        return if (isHls) {
            VideoSource(
                url = HLS_STREAM_URL,
                type = "m3u8",
                quality = "Auto (Adaptive)"
            )
        } else {
            val index = (Math.abs(animeId * 31 + episode)) % sampleSources.size
            VideoSource(
                url = sampleSources[index],
                type = "mp4",
                quality = "1080p"
            )
        }
    }
}
