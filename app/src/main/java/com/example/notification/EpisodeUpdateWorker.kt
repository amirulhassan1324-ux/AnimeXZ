package com.example.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.VideoSource
import com.example.data.model.Anime
import com.example.data.repository.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerState(
    val anime: Anime? = null,
    val currentEpisode: Int = 1,
    val totalEpisodes: Int = 12,
    val videoSource: VideoSource? = null,
    val initialPositionMs: Long = 0L,
    val isPlaying: Boolean = true,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class PlayerViewModel(
    private val animeId: Int,
    initialEpisode: Int,
    private val repository: AnimeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        PlayerState(currentEpisode = initialEpisode)
    )
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    init {
        loadAnimeAndEpisode(initialEpisode)
    }

    fun loadAnimeAndEpisode(episode: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, currentEpisode = episode)
            repository.getAnimeDetail(animeId)
                .onSuccess { anime ->
                    val totalEps = anime.episodes ?: 12
                    val safeEp = episode.coerceIn(1, totalEps)
                    val savedProgress = repository.getProgress(animeId)
                    val initialPos = if (savedProgress?.episodeNumber == safeEp) {
                        savedProgress.currentPositionMs
                    } else 0L

                    val source = repository.getEpisodeSource(animeId, safeEp)

                    _state.value = _state.value.copy(
                        anime = anime,
                        currentEpisode = safeEp,
                        totalEpisodes = totalEps,
                        videoSource = source,
                        initialPositionMs = initialPos,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    val fallback = com.example.data.api.FallbackData.getAnimeById(animeId)
                    val source = repository.getEpisodeSource(animeId, episode)
                    _state.value = _state.value.copy(
                        anime = fallback,
                        currentEpisode = episode,
                        totalEpisodes = fallback.episodes ?: 12,
                        videoSource = source,
                        initialPositionMs = 0L,
                        isLoading = false
                    )
                }
        }
    }

    fun onProgressUpdate(positionMs: Long, durationMs: Long) {
        _state.value = _state.value.copy(
            currentPositionMs = positionMs,
            durationMs = durationMs
        )
    }

    fun saveWatchProgress(positionMs: Long, durationMs: Long) {
        val anime = _state.value.anime ?: return
        viewModelScope.launch {
            repository.saveProgress(
                animeId = anime.id,
                episodeNumber = _state.value.currentEpisode,
                animeTitle = anime.title,
                coverImage = anime.coverImage,
                bannerImage = anime.bannerImage,
                currentPositionMs = positionMs,
                durationMs = durationMs
            )
        }
    }

    fun playNextEpisode(): Boolean {
        val current = _state.value.currentEpisode
        val total = _state.value.totalEpisodes
        if (current < total) {
            // Save progress of current episode
            saveWatchProgress(_state.value.currentPositionMs, _state.value.durationMs)
            loadAnimeAndEpisode(current + 1)
            return true
        }
        return false
    }

    fun playPreviousEpisode(): Boolean {
        val current = _state.value.currentEpisode
        if (current > 1) {
            saveWatchProgress(_state.value.currentPositionMs, _state.value.durationMs)
            loadAnimeAndEpisode(current - 1)
            return true
        }
        return false
    }

    fun setPlaybackSpeed(speed: Float) {
        _state.value = _state.value.copy(playbackSpeed = speed)
    }
}
