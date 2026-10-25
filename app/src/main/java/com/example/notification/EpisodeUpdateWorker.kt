package com.example.notification

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.api.AniListGraphQLClient
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class EpisodeUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting background check for anime episode updates...")

        val context = applicationContext
        val db = AppDatabase.getInstance(context)
        val api = AniListGraphQLClient()

        try {
            val watchlist = db.watchlistDao().getAllWatchlistList()
            if (watchlist.isEmpty()) {
                Log.d(TAG, "Watchlist is empty. No updates to check.")
                return@withContext Result.success()
            }

            for (item in watchlist) {
                try {
                    val anime = if (item.animeId >= com.example.config.ServerConfig.CUSTOM_ID_START) {
                        com.example.data.api.ServerApi.getAnime(item.animeId) ?: continue
                    } else {
                        api.getAnimeDetail(item.animeId).getOrNull() ?: continue
                    }

                    val latestEp = anime.episodes ?: 12
                    val previousNotified = item.lastNotifiedEpisode

                    // If latest episode count is greater than previously notified episode
                    if (latestEp > previousNotified && previousNotified > 0) {
                        Log.d(TAG, "New episode detected for ${item.title}: Ep $latestEp (was $previousNotified)")
                        NotificationHelper.showEpisodeNotification(
                            context = context,
                            animeId = item.animeId,
                            animeTitle = item.title,
                            episodeNumber = latestEp,
                            coverImageUrl = item.coverImage
                        )
                        db.watchlistDao().updateEpisodeReleaseState(
                            animeId = item.animeId,
                            latestEp = latestEp,
                            notifiedEp = latestEp
                        )
                    } else if (previousNotified == 0) {
                        // Initialize baseline on first check
                        db.watchlistDao().updateEpisodeReleaseState(
                            animeId = item.animeId,
                            latestEp = latestEp,
                            notifiedEp = latestEp
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking updates for anime ${item.title}", e)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete EpisodeUpdateWorker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "EpisodeUpdateWorker"
        const val PERIODIC_WORK_TAG = "anime_episode_update_periodic"
        const val ONE_TIME_WORK_TAG = "anime_episode_update_onetime"

        /**
         * Schedules the periodic background worker to check for new episodes.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<EpisodeUpdateWorker>(
                repeatInterval = 6,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag(PERIODIC_WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            Log.d(TAG, "Periodic episode check background task scheduled (every 6 hours).")
        }

        /**
         * Runs an immediate background check for testing and on-demand refresh.
         */
        fun runImmediately(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<EpisodeUpdateWorker>()
                .setConstraints(constraints)
                .addTag(ONE_TIME_WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
            Log.d(TAG, "Immediate episode check background task enqueued.")
        }

        /**
         * Simulates a newly released episode for testing and demo purposes,
         * firing the local notification immediately.
         */
        suspend fun simulateEpisodeRelease(
            context: Context,
            animeId: Int,
            simulatedEpisode: Int? = null
        ) = withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            val item = db.watchlistDao().getWatchlistEntity(animeId) ?: return@withContext

            val nextEp = simulatedEpisode ?: ((item.latestReleasedEpisode.coerceAtLeast(1)) + 1)

            NotificationHelper.showEpisodeNotification(
                context = context,
                animeId = item.animeId,
                animeTitle = item.title,
                episodeNumber = nextEp,
                coverImageUrl = item.coverImage
            )

            db.watchlistDao().updateEpisodeReleaseState(
                animeId = item.animeId,
                latestEp = nextEp,
                notifiedEp = nextEp
            )
        }
    }
}
