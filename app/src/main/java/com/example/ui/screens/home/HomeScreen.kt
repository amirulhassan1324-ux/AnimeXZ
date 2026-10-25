package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AnimePosterCard
import com.example.ui.components.BrandHeader
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.HeroBanner
import com.example.ui.components.SectionHeader
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TextMuted

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToPlayer: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val continueWatchingList by viewModel.continueWatching.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PurplePrimary,
                        modifier = Modifier.testTag("home_loading_indicator")
                    )
                }
            }

            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Unable to connect to AniList",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadHomeData() },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        modifier = Modifier.testTag("home_retry_button")
                    ) {
                        Text("Retry")
                    }
                }
            }

            is HomeUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("home_scroll_container"),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    // Brand Header
                    item {
                        BrandHeader()
                    }

                    // Hero Banner
                    item {
                        HeroBanner(
                            anime = state.featured,
                            onPlayClick = { onNavigateToPlayer(state.featured.id, 1) },
                            onDetailClick = { onNavigateToDetail(state.featured.id) }
                        )
                    }

                    // Continue Watching (if user has watch progress)
                    if (continueWatchingList.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(title = "Continue watching")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("continue_watching_row")
                            ) {
                                items(continueWatchingList, key = { it.animeId }) { progress ->
                                    ContinueWatchingCard(
                                        progress = progress,
                                        onClick = {
                                            onNavigateToPlayer(progress.animeId, progress.episodeNumber)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Trending now row
                    item {
                        Spacer(modifier = Modifier.height(18.dp))
                        SectionHeader(title = "Trending now")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("trending_row")
                        ) {
                            items(state.trending, key = { it.id }) { anime ->
                                AnimePosterCard(
                                    anime = anime,
                                    onClick = { onNavigateToDetail(anime.id) }
                                )
                            }
                        }
                    }

                    // Most popular row
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        SectionHeader(title = "Most popular")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("popular_row")
                        ) {
                            items(state.popular, key = { it.id }) { anime ->
                                AnimePosterCard(
                                    anime = anime,
                                    onClick = { onNavigateToDetail(anime.id) }
                                )
                            }
                        }
                    }

                    // Top rated row
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        SectionHeader(title = "Top rated")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("top_rated_row")
                        ) {
                            items(state.topRated, key = { it.id }) { anime ->
                                AnimePosterCard(
                                    anime = anime,
                                    onClick = { onNavigateToDetail(anime.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
