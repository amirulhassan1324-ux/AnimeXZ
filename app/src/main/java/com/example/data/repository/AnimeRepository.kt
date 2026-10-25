package com.example.data.repository

import com.example.config.ServerConfig
import com.example.config.VideoConfig
import com.example.config.VideoSource
import com.example.data.api.AniListGraphQLClient
import com.example.data.api.ServerApi
import com.example.data.api.HomeData
import com.example.data.local.AppDatabase
import com.example.data.local.WatchProgressEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.Anime
import com.example.data.model.AnimeEpisode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnimeRepository(
    private val database: AppDatabase,
    private val api: AniListGraphQLClient = AniListGraphQLClient()
) {
    val watchlist: Flow<List<WatchlistEntity>> = database.watchlistDao().getAllWatchlist()
    val continueWatching: Flow<List<WatchProgressEntity>> = database.watchProgressDao().getAllProgress()

    suspend fun getHomeSections(): Result<HomeData> {
        return api.getHomeSections()
    }

    suspend fun search(query: String): Result<List<Anime>> {
        val serverResults = ServerApi.getAnimeList(query)
        return api.searchAnime(query).map { serverResults + it }
    }

    suspend fun getServerAnime(): List<Anime> = ServerApi.getAnimeList()


    suspend fun getAnimeDetail(animeId: Int): Result<Anime> {
        if (animeId >= ServerConfig.CUSTOM_ID_START) {
            val anime = ServerApi.getAnime(animeId)
                ?: return Result.failure(Exception("Server se anime load nahi hua. FQDN check karo."))
            return Result.success(anime)
        }
        return api.getAnimeDetail(animeId)
    }

    fun isWatchlisted(animeId: Int): Flow<Boolean> {
        return database.watchlistDao().isInWatchlist(animeId)
    }

    suspend fun toggleWatchlist(anime: Anime) {
        val exists = database.watchlistDao().isInWatchlistSync(anime.id)
        if (exists) {
            database.watchlistDao().delete(anime.id)
        } else {
            val entity = WatchlistEntity(
                animeId = anime.id,
                title = anime.title,
                englishTitle = anime.englishTitle,
                coverImage = anime.coverImage,
                bannerImage = anime.bannerImage,
                averageScore = anime.averageScore,
                format = anime.format,
                episodes = anime.episodes,
                seasonYear = anime.seasonYear,
                genres = anime.genres.joinToString(", "),
                latestReleasedEpisode = anime.episodes ?: 12,
                lastNotifiedEpisode = anime.episodes ?: 12
            )
            database.watchlistDao().insert(entity)
        }
    }

    suspend fun getWatchlistSync(): List<WatchlistEntity> {
        return database.watchlistDao().getAllWatchlistList()
    }

    suspend fun updateEpisodeReleaseState(animeId: Int, latestEp: Int, notifiedEp: Int) {
        database.watchlistDao().updateEpisodeReleaseState(animeId, latestEp, notifiedEp)
    }

    fun observeProgress(animeId: Int): Flow<WatchProgressEntity?> {
        return database.watchProgressDao().observeProgress(animeId)
    }

    suspend fun getProgress(animeId: Int): WatchProgressEntity? {
        return database.watchProgressDao().getProgress(animeId)
    }

    suspend fun saveProgress(
        animeId: Int,
        episodeNumber: Int,
        animeTitle: String,
        coverImage: String,
        bannerImage: String?,
        currentPositionMs: Long,
        durationMs: Long
    ) {
        // Only save meaningful progress (> 2 seconds)
        if (currentPositionMs > 2000L || currentPositionMs == 0L) {
            val entity = WatchProgressEntity(
                animeId = animeId,
                episodeNumber = episodeNumber,
                animeTitle = animeTitle,
                coverImage = coverImage,
                bannerImage = bannerImage,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs
            )
            database.watchProgressDao().saveProgress(entity)
        }
    }

    suspend fun removeFromWatchlist(animeId: Int) {
        database.watchlistDao().delete(animeId)
    }

    suspend fun removeProgress(animeId: Int) {
        database.watchProgressDao().deleteProgress(animeId)
    }

    suspend fun getEpisodesForAnime(anime: Anime): List<AnimeEpisode> {
        val progress = database.watchProgressDao().getProgress(anime.id)
        val durationMs = (anime.durationMinutes ?: 24) * 60 * 1000L
        val serverEpisodes = ServerApi.getEpisodes(anime.id).associateBy { it.episodeNumber }
        val isCustom = anime.id >= ServerConfig.CUSTOM_ID_START

        val numbers: List<Int> = if (isCustom) {
            serverEpisodes.keys.sorted()
        } else {
            val count = (anime.episodes ?: 12).coerceIn(1, 48)
            (1..maxOf(count, serverEpisodes.keys.maxOrNull() ?: 0)).toList()
        }

        return numbers.map { epNum ->
            val isCurrentEp = progress?.episodeNumber == epNum
            val progressMs = if (isCurrentEp) (progress?.currentPositionMs ?: 0L) else 0L
            val server = serverEpisodes[epNum]

            AnimeEpisode(
                animeId = anime.id,
                episodeNumber = epNum,
                title = server?.title ?: "Episode $epNum",
                durationMinutes = server?.durationMinutes ?: (anime.durationMinutes ?: 24),
                thumbnail = server?.thumbnail ?: anime.bannerImage ?: anime.coverImage,
                progressMs = progressMs,
                durationMs = durationMs
            )
        }
    }

    /** Pehle Koyeb server ka stream URL, na mile to purane sample videos. */
    suspend fun getEpisodeSource(animeId: Int, episode: Int): VideoSource {
        return ServerApi.getStream(animeId, episode) ?: VideoConfig.getEpisodeSource(animeId, episode)
    }
}
