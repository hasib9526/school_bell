package com.example.school_bell.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.school_bell.ui.theme.*
import com.example.school_bell.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepNavy, BgSurface)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // School name header
            Text(
                text = state.schoolName,
                style = MaterialTheme.typography.titleMedium,
                color = GoldPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Live Clock
            LiveClockCard(state.currentTimeMillis)

            Spacer(modifier = Modifier.height(12.dp))

            // Next events row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Next Bell card
                NextEventCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.NotificationsActive,
                    accentColor = TealPrimary,
                    title = "Next Bell",
                    eventLabel = state.nextBell?.label ?: "No bells",
                    minutesAway = state.nextBellMinutesAway
                )
                // Next Azan card
                NextEventCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Star,
                    accentColor = GoldPrimary,
                    title = "Next Azan",
                    eventLabel = state.nextAzanName.ifEmpty { "No prayers" },
                    minutesAway = state.nextAzanMinutesAway
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Battery card
            BatteryCard(
                batteryLevel = state.batteryLevel,
                isCharging = state.isBatteryCharging
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Kiosk mode toggle
            KioskCard(
                isEnabled = state.isKioskEnabled,
                onToggle = { viewModel.toggleKiosk(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Server connection status
            ServerStatusCard(isConnected = state.isServerConnected)

            Spacer(modifier = Modifier.height(12.dp))

            // Live announcement banner
            val announcement = state.liveAnnouncement
            AnimatedVisibility(
                visible = announcement != null && announcement.isActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (announcement != null) {
                    AnnouncementBanner(title = announcement.title, message = announcement.message)
                }
            }
        }
    }
}

@Composable
private fun LiveClockCard(currentTimeMillis: Long) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()) }
    val timeStr = remember(currentTimeMillis) { timeFormat.format(Date(currentTimeMillis)) }
    val dateStr = remember(currentTimeMillis) { dateFormat.format(Date(currentTimeMillis)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeStr,
                fontSize = 56.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TealPrimary,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun NextEventCard(
    modifier: Modifier,
    icon: ImageVector,
    accentColor: Color,
    title: String,
    eventLabel: String,
    minutesAway: Long
) {
    val countdownText = when {
        minutesAway < 0 -> "--"
        minutesAway == 0L -> "Now!"
        minutesAway < 60 -> "${minutesAway}m"
        else -> "${minutesAway / 60}h ${minutesAway % 60}m"
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = eventLabel,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (minutesAway >= 0) "In $countdownText" else "None today",
                style = MaterialTheme.typography.bodySmall,
                color = if (minutesAway in 0..15) StatusOrange else TextSecondary
            )
        }
    }
}

@Composable
private fun BatteryCard(batteryLevel: Int, isCharging: Boolean) {
    val batteryColor = when {
        batteryLevel <= 15 -> StatusRed
        batteryLevel <= 30 -> StatusOrange
        else -> StatusGreen
    }
    val batteryIcon = when {
        isCharging -> Icons.Filled.BatteryChargingFull
        batteryLevel > 80 -> Icons.Filled.BatteryFull
        batteryLevel > 50 -> Icons.Filled.Battery5Bar
        batteryLevel > 20 -> Icons.Filled.Battery3Bar
        else -> Icons.Filled.Battery0Bar
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(batteryIcon, contentDescription = null, tint = batteryColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Battery Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    text = "$batteryLevel% ${if (isCharging) "• Charging" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    color = batteryColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LinearProgressIndicator(
                progress = { batteryLevel / 100f },
                modifier = Modifier
                    .width(80.dp)
                    .height(8.dp),
                color = batteryColor,
                trackColor = NavyLight
            )
        }
    }
}

@Composable
private fun KioskCard(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) TealDark.copy(alpha = 0.3f) else BgCard
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = if (isEnabled) TealPrimary else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Kiosk Mode",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isEnabled) "Device locked to this app" else "Normal operation mode",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DeepNavy,
                    checkedTrackColor = TealPrimary,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = NavyLight
                )
            )
        }
    }
}

@Composable
private fun ServerStatusCard(isConnected: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = if (isConnected) StatusGreen else StatusRed,
                        shape = RoundedCornerShape(50)
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isConnected) "Server Connected" else "Server Offline",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isConnected) StatusGreen else StatusRed
            )
        }
    }
}

@Composable
private fun AnnouncementBanner(title: String, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Filled.Campaign,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Live Announcement",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
