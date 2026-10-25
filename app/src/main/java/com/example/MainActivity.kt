package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.notification.EpisodeUpdateWorker
import com.example.notification.NotificationHelper
import com.example.ui.navigation.AniStreamApp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var initialAnimeId by mutableStateOf<Int?>(null)
    private var initialEpisodeNumber by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize notification channel and schedule periodic background update worker
        NotificationHelper.createNotificationChannel(applicationContext)
        EpisodeUpdateWorker.schedule(applicationContext)

        extractNotificationExtras(intent)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    AniStreamApp(
                        initialAnimeId = initialAnimeId,
                        initialEpisodeNumber = initialEpisodeNumber
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractNotificationExtras(intent)
    }

    private fun extractNotificationExtras(intent: Intent?) {
        if (intent != null && intent.hasExtra(NotificationHelper.EXTRA_ANIME_ID)) {
            val animeId = intent.getIntExtra(NotificationHelper.EXTRA_ANIME_ID, 0)
            val episode = intent.getIntExtra(NotificationHelper.EXTRA_EPISODE_NUMBER, 1)
            if (animeId > 0) {
                initialAnimeId = animeId
                initialEpisodeNumber = episode
            }
        }
    }
}
