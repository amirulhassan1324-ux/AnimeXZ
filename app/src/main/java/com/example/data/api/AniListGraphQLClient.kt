package com.example.data.api

import android.util.Log
import com.example.data.model.Anime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AniListGraphQLClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val endpoint = "https://graphql.anilist.co"

    suspend fun getHomeSections(): Result<HomeData> = withContext(Dispatchers.IO) {
        val query = """
            query {
              trending: Page(page: 1, perPage: 12) {
                media(sort: TRENDING_DESC, type: ANIME, isAdult: false) {
                  id
                  title { romaji english userPreferred }
                  coverImage { extraLarge large medium color }
                  bannerImage
                  averageScore
                  format
                  episodes
                  seasonYear
                  genres
                  description
                }
              }
              popular: Page(page: 1, perPage: 12) {
                media(sort: POPULARITY_DESC, type: ANIME, isAdult: false) {
                  id
                  title { romaji english userPreferred }
                  coverImage { extraLarge large medium color }
                  bannerImage
                  averageScore
                  format
                  episodes
                  seasonYear
                  genres
                  description
                }
              }
              topRated: Page(page: 1, perPage: 12) {
                media(sort: SCORE_DESC, type: ANIME, isAdult: false) {
                  id
                  title { romaji english userPreferred }
                  coverImage { extraLarge large medium color }
                  bannerImage
                  averageScore
                  format
                  episodes
                  seasonYear
                  genres
                  description
                }
              }
            }
        """.trimIndent()

        try {
            val jsonPayload = JSONObject().apply {
                put("query", query)
            }
            val request = Request.Builder()
                .url(endpoint)
                .post(jsonPayload.toString().toRequestBody(mediaType))
                .header("Accept", "application/json")
                .header("User-Agent", "AniStream-Android")
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string()

            if (response.isSuccessful && !bodyString.isNullOrEmpty()) {
                val json = JSONObject(bodyString)
                val data = json.optJSONObject("data")
                if (data != null) {
                    val trending = parseMediaList(data.optJSONObject("trending")?.optJSONArray("media"))
                    val popular = parseMediaList(data.optJSONObject("popular")?.optJSONArray("media"))
                    val topRated = parseMediaList(data.optJSONObject("topRated")?.optJSONArray("media"))

                    val finalTrending = if (trending.isNotEmpty()) trending else FallbackData.getTrending()
                    val finalPopular = if (popular.isNotEmpty()) popular else FallbackData.getPopular()
                    val finalTopRated = if (topRated.isNotEmpty()) topRated else FallbackData.getTopRated()

                    return@withContext Result.success(HomeData(finalTrending, finalPopular, finalTopRated))
                }
            }
            Log.w("AniListClient", "Response not successful: ${response.code}, using fallback")
            Result.success(HomeData(FallbackData.getTrending(), FallbackData.getPopular(), FallbackData.getTopRated()))
        } catch (e: Exception) {
            Log.e("AniListClient", "Network error in getHomeSections", e)
            Result.success(HomeData(FallbackData.getTrending(), FallbackData.getPopular(), FallbackData.getTopRated()))
        }
    }

    suspend fun searchAnime(queryText: String): Result<List<Anime>> = withContext(Dispatchers.IO) {
        if (queryText.isBlank()) return@withContext Result.success(emptyList())

        val query = """
            query (${'$'}search: String) {
              Page(page: 1, perPage: 24) {
                media(search: ${'$'}search, type: ANIME, isAdult: false, sort: SEARCH_MATCH) {
                  id
                  title { romaji english userPreferred }
                  coverImage { extraLarge large medium color }
                  bannerImage
                  averageScore
                  format
                  episodes
                  seasonYear
                  genres
                  description
                }
              }
            }
        """.trimIndent()

        try {
            val jsonPayload = JSONObject().apply {
                put("query", query)
                put("variables", JSONObject().put("search", queryText))
            }
            val request = Request.Builder()
                .url(endpoint)
                .post(jsonPayload.toString().toRequestBody(mediaType))
                .header("Accept", "application/json")
                .header("User-Agent", "AniStream-Android")
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string()

            if (response.isSuccessful && !bodyString.isNullOrEmpty()) {
                val json = JSONObject(bodyString)
                val mediaArray = json.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("media")
                val results = parseMediaList(mediaArray)
                return@withContext Result.success(results)
            }
            // Fallback search locally
            val fallbackMatches = FallbackData.allFallbackAnime().filter {
                it.title.contains(queryText, ignoreCase = true) ||
                    (it.englishTitle?.contains(queryText, ignoreCase = true) == true) ||
                    it.genres.any { g -> g.contains(queryText, ignoreCase = true) }
            }
            Result.success(fallbackMatches)
        } catch (e: Exception) {
            Log.e("AniListClient", "Search error for '$queryText'", e)
            val fallbackMatches = FallbackData.allFallbackAnime().filter {
                it.title.contains(queryText, ignoreCase = true) ||
                    (it.englishTitle?.contains(queryText, ignoreCase = true) == true)
            }
            Result.success(fallbackMatches)
        }
    }

    suspend fun getAnimeDetail(animeId: Int): Result<Anime> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: ANIME) {
                id
                title { romaji english native userPreferred }
                coverImage { extraLarge large medium color }
                bannerImage
                averageScore
                format
                episodes
                duration
                status
                seasonYear
                genres
                description(asHtml: false)
                studios(isMain: true) {
                  nodes { name }
                }
              }
            }
        """.trimIndent()

        try {
            val jsonPayload = JSONObject().apply {
                put("query", query)
                put("variables", JSONObject().put("id", animeId))
            }
            val request = Request.Builder()
                .url(endpoint)
                .post(jsonPayload.toString().toRequestBody(mediaType))
                .header("Accept", "application/json")
                .header("User-Agent", "AniStream-Android")
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string()

            if (response.isSuccessful && !bodyString.isNullOrEmpty()) {
                val json = JSONObject(bodyString)
                val mediaJson = json.optJSONObject("data")?.optJSONObject("Media")
                if (mediaJson != null) {
                    val anime = parseSingleMedia(mediaJson)
                    return@withContext Result.success(anime)
                }
            }
            val fallback = FallbackData.getAnimeById(animeId)
            Result.success(fallback)
        } catch (e: Exception) {
            Log.e("AniListClient", "Get detail error for $animeId", e)
            Result.success(FallbackData.getAnimeById(animeId))
        }
    }

    private fun parseMediaList(array: JSONArray?): List<Anime> {
        if (array == null) return emptyList()
        val list = mutableListOf<Anime>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            list.add(parseSingleMedia(item))
        }
        return list
    }

    private fun parseSingleMedia(json: JSONObject): Anime {
        val id = json.optInt("id", 0)
        val titleObj = json.optJSONObject("title")
        val userPreferred = titleObj?.optString("userPreferred")
        val romaji = titleObj?.optString("romaji")
        val english = titleObj?.optString("english")
        val native = titleObj?.optString("native")

        val title = when {
            !userPreferred.isNullOrBlank() -> userPreferred
            !english.isNullOrBlank() -> english
            !romaji.isNullOrBlank() -> romaji
            else -> "Anime #$id"
        }

        val coverObj = json.optJSONObject("coverImage")
        val coverImage = coverObj?.optString("extraLarge")
            ?.takeIf { it.isNotBlank() }
            ?: coverObj?.optString("large")
            ?: coverObj?.optString("medium")
            ?: ""

        val banner = json.optString("bannerImage").takeIf { it.isNotBlank() }

        val rawScore = json.optDouble("averageScore", 0.0)
        val score = if (rawScore > 0) rawScore / 10.0 else 8.5

        val format = json.optString("format", "TV")
        val episodes = if (json.has("episodes") && !json.isNull("episodes")) json.optInt("episodes", 12) else 12
        val duration = if (json.has("duration") && !json.isNull("duration")) json.optInt("duration", 24) else 24
        val seasonYear = if (json.has("seasonYear") && !json.isNull("seasonYear")) json.optInt("seasonYear", 2024) else null
        val status = json.optString("status", "RELEASING")

        val genresList = mutableListOf<String>()
        val genresArray = json.optJSONArray("genres")
        if (genresArray != null) {
            for (j in 0 until genresArray.length()) {
                genresList.add(genresArray.optString(j))
            }
        }

        val rawDesc = json.optString("description", "")
        // Clean html tags if present
        val cleanDesc = rawDesc.replace(Regex("<[^>]*>"), "").replace("&quot;", "\"").replace("&#039;", "'")

        val studiosObj = json.optJSONObject("studios")
        val studioNode = studiosObj?.optJSONArray("nodes")?.optJSONObject(0)
        val studioName = studioNode?.optString("name")

        return Anime(
            id = id,
            title = title,
            englishTitle = english,
            nativeTitle = native,
            coverImage = coverImage,
            bannerImage = banner ?: coverImage,
            averageScore = score,
            format = format,
            episodes = episodes,
            durationMinutes = duration,
            seasonYear = seasonYear,
            status = status,
            genres = genresList,
            description = cleanDesc,
            studio = studioName
        )
    }
}

data class HomeData(
    val trending: List<Anime>,
    val popular: List<Anime>,
    val topRated: List<Anime>
)
