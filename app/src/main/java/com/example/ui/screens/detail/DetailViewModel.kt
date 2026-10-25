package com.example.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WatchProgressEntity
import com.example.data.model.Anime
import com.example.data.model.AnimeEpisode
import com.example.data.repository.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(
        val anime: Anime,
        val episodes: List<AnimeEpisode>,
        val isWatchlisted: Boolean,
        val progress: WatchProgressEntity?
    ) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

class DetailViewModel(
    private val animeId: Int,
    private val repository: AnimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
        observeWatchlistStatus()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            repository.getAnimeDetail(animeId)
                .onSuccess { anime ->
                    val episodes = repository.getEpisodesForAnime(anime)
                    val progress = repository.getProgress(animeId)
                    val isSaved = repository.isWatchlisted(animeId).stateIn(viewModelScope).value
                    _uiState.value = DetailUiState.Success(
                        anime = anime,
                        episodes = episodes,
                        isWatchlisted = isSaved,
                        progress = progress
                    )
                }
                .onFailure { error ->
                    _uiState.value = DetailUiState.Error(error.localizedMessage ?: "Failed to load details")
                }
        }
    }

    private fun observeWatchlistStatus() {
        viewModelScope.launch {
            repository.isWatchlisted(animeId).collect { isSaved ->
                val current = _uiState.value
                if (current is DetailUiState.Success) {
                    _uiState.value = current.copy(isWatchlisted = isSaved)
                }
            }
        }
    }

    fun toggleWatchlist() {
        val current = _uiState.value
        if (current is DetailUiState.Success) {
            viewModelScope.launch {
                repository.toggleWatchlist(current.anime)
            }
        }
    }
}
