package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object NotificationHelper {

    const val CHANNEL_ID = "anime_episode_releases"
    const val CHANNEL_NAME = "New Episode Releases"
    const val CHANNEL_DESCRIPTION = "Alerts when a new episode of an anime in My List is released"

    const val EXTRA_ANIME_ID = "EXTRA_ANIME_ID"
    const val EXTRA_EPISODE_NUMBER = "EXTRA_EPISODE_NUMBER"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = android.graphics.Color.parseColor("#A855F7")
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    suspend fun showEpisodeNotification(
        context: Context,
        animeId: Int,
        animeTitle: String,
        episodeNumber: Int,
        coverImageUrl: String?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                Log.w("NotificationHelper", "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        createNotificationChannel(context)

        // Target intent to open MainActivity directly into the player or anime detail
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "OPEN_ANIME_EPISODE"
            putExtra(EXTRA_ANIME_ID, animeId)
            putExtra(EXTRA_EPISODE_NUMBER, episodeNumber)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            animeId * 1000 + episodeNumber,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Try downloading cover art bitmap for large icon
        val largeIconBitmap: Bitmap? = withContext(Dispatchers.IO) {
            if (!coverImageUrl.isNullOrBlank()) {
                try {
                    val stream = URL(coverImageUrl).openStream()
                    BitmapFactory.decodeStream(stream)
                } catch (e: Exception) {
                    null
                }
            } else null
        }

        val title = "New Episode Available! ⚡"
        val contentText = "Episode $episodeNumber of $animeTitle is out now!"
        val bigText = "Episode $episodeNumber of $animeTitle is now streaming on AniStream! Tap to start watching right away."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_purple))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap)
        }

        val notificationId = animeId * 100 + episodeNumber

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            Log.d("NotificationHelper", "Successfully notified for $animeTitle Ep $episodeNumber")
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "SecurityException showing notification", e)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error showing notification", e)
        }
    }
}
