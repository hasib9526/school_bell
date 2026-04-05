package com.example.school_bell.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.school_bell.data.model.AnnouncementResponse
import com.example.school_bell.data.prefs.AppPreferences
import com.example.school_bell.data.repository.DeviceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AnnouncementUiState(
    val announcements: List<AnnouncementResponse> = emptyList(),
    val latestAnnouncement: AnnouncementResponse? = null,
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val error: String? = null
)

class AnnouncementViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AppPreferences(application)
    private val deviceRepository = DeviceRepository(application, preferences)

    private val _uiState = MutableStateFlow(AnnouncementUiState())
    val uiState: StateFlow<AnnouncementUiState> = _uiState.asStateFlow()

    private var player: ExoPlayer? = null

    private val announcementReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val title = intent?.getStringExtra("announcement_title") ?: return
            val message = intent.getStringExtra("announcement_message") ?: ""
            val id = intent.getLongExtra("announcement_id", 0)
            val newAnnouncement = AnnouncementResponse(
                id = id, title = title, message = message,
                createdAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date())
            )
            _uiState.update { state ->
                val updated = (listOf(newAnnouncement) + state.announcements).take(20)
                state.copy(latestAnnouncement = newAnnouncement, announcements = updated)
            }
        }
    }

    init {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(
                announcementReceiver,
                IntentFilter("com.example.school_bell.NEW_ANNOUNCEMENT"),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            getApplication<Application>().registerReceiver(
                announcementReceiver,
                IntentFilter("com.example.school_bell.NEW_ANNOUNCEMENT")
            )
        }
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                fetchLatestAnnouncement()
                delay(30_000L)
            }
        }
    }

    fun fetchLatestAnnouncement() {
        viewModelScope.launch {
            try {
                val token = preferences.authToken.first()
                if (token != null) {
                    val result = deviceRepository.getLatestAnnouncement()
                    if (result.isSuccess && result.data != null) {
                        _uiState.update { state ->
                            val ann = result.data
                            val existing = state.announcements
                            val updated = if (existing.none { it.id == ann.id }) {
                                (listOf(ann) + existing).take(20)
                            } else existing
                            state.copy(
                                latestAnnouncement = ann,
                                announcements = updated,
                                isConnected = true,
                                error = null
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isConnected = false, error = result.error) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isConnected = false) }
            }
        }
    }

    fun testPlay(audioUrl: String?) {
        val url = audioUrl ?: return
        viewModelScope.launch {
            try {
                releasePlayer()
                player = ExoPlayer.Builder(getApplication()).build()
                val mediaItem = MediaItem.fromUri(url)
                player?.apply {
                    setMediaItem(mediaItem)
                    prepare()
                    play()
                    addListener(object : androidx.media3.common.Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == androidx.media3.common.Player.STATE_ENDED ||
                                state == androidx.media3.common.Player.STATE_IDLE) {
                                _uiState.update { it.copy(isPlaying = false) }
                            }
                        }
                    })
                }
                _uiState.update { it.copy(isPlaying = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Playback error: ${e.message}") }
            }
        }
    }

    fun stopPlay() {
        releasePlayer()
        _uiState.update { it.copy(isPlaying = false) }
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
        try { getApplication<Application>().unregisterReceiver(announcementReceiver) } catch (e: Exception) { }
    }
}
