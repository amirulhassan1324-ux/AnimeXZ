package com.example.ui.screens.watchlist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WatchlistEntity
import com.example.data.repository.AnimeRepository
import com.example.notification.EpisodeUpdateWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WatchlistViewModel(private val repository: AnimeRepository) : ViewModel() {

    val watchlist: StateFlow<List<WatchlistEntity>> = repository.watchlist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isCheckingUpdates = MutableStateFlow(false)
    val isCheckingUpdates: StateFlow<Boolean> = _isCheckingUpdates.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun removeFromWatchlist(animeId: Int) {
        viewModelScope.launch {
            repository.removeFromWatchlist(animeId)
        }
    }

    fun checkForUpdatesNow(context: Context) {
        viewModelScope.launch {
            _isCheckingUpdates.value = true
            _statusMessage.value = "Background worker checking for new episodes..."
            EpisodeUpdateWorker.runImmediately(context)
            delay(1200)
            _isCheckingUpdates.value = false
            _statusMessage.value = "Episode update check completed."
            delay(3000)
            _statusMessage.value = null
        }
    }

    fun simulateNewEpisode(context: Context, animeId: Int) {
        viewModelScope.launch {
            _statusMessage.value = "Simulating episode release notification..."
            EpisodeUpdateWorker.simulateEpisodeRelease(context, animeId)
            delay(2000)
            _statusMessage.value = "Notification sent! Pull down notification drawer to check."
            delay(4000)
            _statusMessage.value = null
        }
    }
}
