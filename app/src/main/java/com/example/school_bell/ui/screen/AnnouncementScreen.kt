package com.example.school_bell.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.school_bell.data.model.AnnouncementResponse
import com.example.school_bell.ui.theme.*
import com.example.school_bell.viewmodel.AnnouncementViewModel

@Composable
fun AnnouncementScreen(viewModel: AnnouncementViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepNavy, BgSurface)))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Campaign, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Announcements",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.fetchLatestAnnouncement() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = TealPrimary)
                    }
                }
            }

            // Connection status card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.isConnected) StatusGreen.copy(0.1f) else StatusRed.copy(0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (state.isConnected) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = if (state.isConnected) StatusGreen else StatusRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (state.isConnected) "Server Connected — Monitoring active" else "Server Disconnected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.isConnected) StatusGreen else StatusRed
                        )
                    }
                }
            }

            // Latest announcement feature card
            state.latestAnnouncement?.let { ann ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(0.12f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Latest Announcement",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (ann.audioUrl != null) {
                                    IconButton(
                                        onClick = {
                                            if (state.isPlaying) viewModel.stopPlay()
                                            else viewModel.testPlay(ann.audioUrl)
                                        },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            if (state.isPlaying) Icons.Filled.StopCircle else Icons.Filled.PlayCircle,
                                            contentDescription = if (state.isPlaying) "Stop" else "Play",
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = ann.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (ann.message.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ann.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Received: ${ann.createdAt}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextDisabled
                            )
                        }
                    }
                }
            }

            // Test play button (if no current announcement has audio)
            item {
                OutlinedButton(
                    onClick = {
                        if (state.isPlaying) viewModel.stopPlay()
                        else viewModel.testPlay(state.latestAnnouncement?.audioUrl)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.latestAnnouncement?.audioUrl != null,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary)
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (state.isPlaying) "Stop Playback" else "Test Play Audio")
                }
            }

            // Error
            state.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StatusRed.copy(alpha = 0.15f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Error, contentDescription = null, tint = StatusRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(error, color = StatusRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.clearError() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = StatusRed)
                            }
                        }
                    }
                }
            }

            // Announcements list header
            item {
                Text(
                    "Recent Announcements",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (state.announcements.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.NotificationsNone, contentDescription = null, tint = TextDisabled, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No announcements yet", color = TextDisabled, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(state.announcements, key = { it.id }) { ann ->
                    AnnouncementListItem(ann)
                }
            }
        }
    }
}

@Composable
private fun AnnouncementListItem(ann: AnnouncementResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                if (ann.priority > 0) Icons.Filled.PriorityHigh else Icons.Filled.Info,
                contentDescription = null,
                tint = if (ann.priority > 0) StatusOrange else TealPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ann.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (ann.message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ann.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ann.createdAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDisabled
                    )
                    if (ann.audioUrl != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.VolumeUp,
                            contentDescription = "Has audio",
                            tint = GoldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
