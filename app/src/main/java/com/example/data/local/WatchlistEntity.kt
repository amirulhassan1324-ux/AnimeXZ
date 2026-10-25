package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val animeId: Int,
    val title: String,
    val englishTitle: String?,
    val coverImage: String,
    val bannerImage: String?,
    val averageScore: Double,
    val format: String,
    val episodes: Int?,
    val seasonYear: Int?,
    val genres: String,
    val latestReleasedEpisode: Int = 0,
    val lastNotifiedEpisode: Int = 0,
    val savedAt: Long = System.currentTimeMillis()
)
