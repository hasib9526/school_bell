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
import com.example.school_bell.azan.AzanScheduler
import com.example.school_bell.data.db.entities.AzanTime
import com.example.school_bell.ui.theme.*
import com.example.school_bell.viewmodel.AzanViewModel
import com.example.school_bell.viewmodel.CALCULATION_METHODS

@Composable
fun AzanScreen(viewModel: AzanViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showMethodMenu by remember { mutableStateOf(false) }

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
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Prayer Times (Azan)",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // GPS Coordinates card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = TealPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GPS Coordinates", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.latitude == 0.0 && state.longitude == 0.0) {
                            Text(
                                text = "Location not set — tap 'Get Location' below",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusOrange
                            )
                        } else {
                            Row {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Latitude", style = MaterialTheme.typography.labelSmall, color = TextDisabled)
                                    Text(
                                        String.format("%.6f", state.latitude),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Longitude", style = MaterialTheme.typography.labelSmall, color = TextDisabled)
                                    Text(
                                        String.format("%.6f", state.longitude),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Calculation method selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Column {
                            Text(
                                "Calculation Method",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showMethodMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                                border = ButtonDefaults.outlinedButtonBorder.copy()
                            ) {
                                Text(
                                    text = CALCULATION_METHODS.find { it.first == state.calcMethod }?.second
                                        ?: state.calcMethod,
                                    color = TealPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = TealPrimary)
                            }
                        }
                        DropdownMenu(
                            expanded = showMethodMenu,
                            onDismissRequest = { showMethodMenu = false },
                            modifier = Modifier.background(BgElevated)
                        ) {
                            CALCULATION_METHODS.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            label,
                                            color = if (key == state.calcMethod) TealPrimary else TextPrimary,
                                            fontWeight = if (key == state.calcMethod) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.setCalcMethod(key)
                                        showMethodMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.fetchLocation() },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary)
                    ) {
                        Icon(Icons.Filled.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Get Location")
                    }
                    Button(
                        onClick = { viewModel.recalculate() },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DeepNavy)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DeepNavy, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recalculate")
                    }
                }
            }

            // Error
            state.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StatusRed.copy(alpha = 0.15f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Filled.Error, contentDescription = null, tint = StatusRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(error, color = StatusRed, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (!state.isLocationPermissionGranted && state.latitude == 0.0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Location permission required for automatic prayer time calculation",
                                color = GoldPrimary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Prayer times header
            item {
                Text(
                    "Today's Prayer Times",
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Prayer times list
            val prayerNames = AzanScheduler.PRAYER_NAMES
            if (state.prayerTimesDisplay.isNotEmpty()) {
                items(prayerNames) { name ->
                    val displayTime = state.prayerTimesDisplay[name] ?: "--:--"
                    val azanEntry = state.prayerTimes.find { it.prayerName == name }
                    PrayerTimeCard(
                        prayerName = name,
                        displayTime = displayTime,
                        azanTime = azanEntry,
                        onToggle = { azanEntry?.let { viewModel.togglePrayerEnabled(it) } }
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Prayer times not calculated yet.\nSet your location and tap Recalculate.",
                            color = TextDisabled,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerTimeCard(
    prayerName: String,
    displayTime: String,
    azanTime: AzanTime?,
    onToggle: () -> Unit
) {
    val prayerIcon = when (prayerName) {
        "Fajr" -> Icons.Filled.WbTwilight
        "Dhuhr" -> Icons.Filled.WbSunny
        "Asr" -> Icons.Filled.WbCloudy
        "Maghrib" -> Icons.Filled.Nightlight
        "Isha" -> Icons.Filled.NightsStay
        else -> Icons.Filled.Star
    }

    val isEnabled = azanTime?.isEnabled ?: true

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) BgCard else BgCard.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                prayerIcon,
                contentDescription = null,
                tint = if (isEnabled) GoldPrimary else TextDisabled,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = prayerName,
                style = MaterialTheme.typography.titleSmall,
                color = if (isEnabled) TextPrimary else TextDisabled,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = displayTime,
                style = MaterialTheme.typography.titleMedium,
                color = if (isEnabled) GoldPrimary else TextDisabled,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = GoldPrimary,
                    checkedThumbColor = DeepNavy,
                    uncheckedTrackColor = NavyLight
                )
            )
        }
    }
}
