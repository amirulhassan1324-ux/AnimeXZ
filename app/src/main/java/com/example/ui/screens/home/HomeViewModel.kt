package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.HomeData
import com.example.data.local.WatchProgressEntity
import com.example.data.model.Anime
import com.example.data.repository.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val featured: Anime,
        val trending: List<Anime>,
        val popular: List<Anime>,
        val topRated: List<Anime>,
        val serverAnime: List<Anime> = emptyList()
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(private val repository: AnimeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val continueWatching: StateFlow<List<WatchProgressEntity>> = repository.continueWatching
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            repository.getHomeSections()
                .onSuccess { data ->
                    val serverAnime = repository.getServerAnime()
                    val featured = serverAnime.firstOrNull() ?: data.trending.firstOrNull() ?: data.popular.first()
                    _uiState.value = HomeUiState.Success(
                        featured = featured,
                        trending = data.trending,
                        popular = data.popular,
                        topRated = data.topRated,
                        serverAnime = serverAnime
                    )
                }
                .onFailure { error ->
                    _uiState.value = HomeUiState.Error(error.localizedMessage ?: "Failed to load anime")
                }
        }
    }
}
