package com.example.data.api

import android.util.Log
import com.example.config.ServerConfig
import com.example.config.VideoSource
import com.example.data.model.Anime
import com.example.data.model.AnimeEpisode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Koyeb server (MongoDB wala) se anime, episodes aur stream URLs laata hai. */
object ServerApi {
    private const val TAG = "ServerApi"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS) // Koyeb free instance sleep se jaagne me time leta hai
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private suspend fun get(path: String): String? = withContext(Dispatchers.IO) {
        if (!ServerConfig.isConfigured) return@withContext null
        try {
            val req = Request.Builder().url(ServerConfig.BASE_URL + path).build()
            client.newCall(req).execute().use { r ->
                if (r.isSuccessful) r.body?.string() else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "GET $path failed", e)
            null
        }
    }

    suspend fun getAnimeList(query: String? = null): List<Anime> {
        val path = if (query.isNullOrBlank()) "/api/anime" else "/api/anime?q=" + URLEncoder.encode(query, "UTF-8")
        val body = get(path) ?: return emptyList()
        return try {
            val arr = JSONArray(body)
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(::parseAnime) }
        } catch (e: Exception) {
            Log.e(TAG, "parse list", e); emptyList()
        }
    }

    /** Episode count = max(server anime.episodes, sabse bada uploaded episode number). */
    suspend fun getAnime(id: Int): Anime? {
        val body = get("/api/anime/$id") ?: return null
        return try {
            val anime = parseAnime(JSONObject(body))
            val maxEp = getEpisodes(id).maxOfOrNull { it.episodeNumber } ?: 0
            anime.copy(episodes = maxOf(anime.episodes ?: 0, maxEp).takeIf { it > 0 })
        } catch (e: Exception) {
            Log.e(TAG, "parse anime", e); null
        }
    }

    suspend fun getEpisodes(animeId: Int): List<AnimeEpisode> {
        val body = get("/api/anime/$animeId/episodes") ?: return emptyList()
        return try {
            val arr = JSONArray(body)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val n = o.optInt("number", 0)
                if (n <= 0) return@mapNotNull null
                AnimeEpisode(
                    animeId = animeId,
                    episodeNumber = n,
                    title = o.optString("title").ifBlank { "Episode $n" },
                    durationMinutes = o.optInt("durationMinutes", 24),
                    thumbnail = o.optString("thumbnail").takeIf { it.isNotBlank() }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "parse episodes", e); emptyList()
        }
    }

    /** Is episode ka stream URL server pe hai to VideoSource, warna null. */
    suspend fun getStream(animeId: Int, episode: Int): VideoSource? {
        val body = get("/api/stream/$animeId/$episode") ?: return null
        return try {
            val o = JSONObject(body)
            val url = o.optString("videoUrl")
            if (url.isBlank()) null
            else VideoSource(
                url = url,
                type = o.optString("type", if (url.contains(".m3u8")) "m3u8" else "mp4"),
                quality = "Server"
            )
        } catch (e: Exception) {
            Log.e(TAG, "parse stream", e); null
        }
    }

    private fun parseAnime(o: JSONObject): Anime {
        val genres = mutableListOf<String>()
        o.optJSONArray("genres")?.let { g -> for (i in 0 until g.length()) genres.add(g.optString(i)) }
        val cover = o.optString("coverImage")
        return Anime(
            id = o.optInt("id"),
            title = o.optString("title"),
            englishTitle = o.optString("englishTitle").takeIf { it.isNotBlank() && it != "null" },
            coverImage = cover,
            bannerImage = o.optString("bannerImage").takeIf { it.isNotBlank() } ?: cover,
            averageScore = o.optDouble("averageScore", 0.0),
            format = o.optString("format", "TV"),
            episodes = o.optInt("episodes", 0).takeIf { it > 0 },
            durationMinutes = o.optInt("durationMinutes", 24),
            seasonYear = o.optInt("seasonYear", 0).takeIf { it > 0 },
            status = o.optString("status", "RELEASING"),
            genres = genres,
            description = o.optString("description"),
            studio = o.optString("studio").takeIf { it.isNotBlank() && it != "null" }
        )
    }
}
