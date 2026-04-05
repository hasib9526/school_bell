package com.example.school_bell.ui.screen

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.example.school_bell.ui.theme.*
import com.example.school_bell.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val editServerUrl by viewModel.editServerUrl.collectAsState()
    val editSchoolName by viewModel.editSchoolName.collectAsState()
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSyncIntervalMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            delay(2000)
            viewModel.clearSaved()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepNavy, BgSurface)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            // Server section
            SettingsSectionHeader("Server Configuration", Icons.Filled.Cloud)

            OutlinedTextField(
                value = editServerUrl,
                onValueChange = { viewModel.updateEditServerUrl(it) },
                label = { Text("Server URL") },
                leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null, tint = TealPrimary) },
                modifier = Modifier.fillMaxWidth(),
                colors = settingsTextFieldColors(),
                singleLine = true
            )

            OutlinedTextField(
                value = editSchoolName,
                onValueChange = { viewModel.updateEditSchoolName(it) },
                label = { Text("School Name") },
                leadingIcon = { Icon(Icons.Filled.School, contentDescription = null, tint = TealPrimary) },
                modifier = Modifier.fillMaxWidth(),
                colors = settingsTextFieldColors(),
                singleLine = true
            )

            Button(
                onClick = { viewModel.saveSettings() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = DeepNavy)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (state.isSaved) "Saved!" else "Save Settings", fontWeight = FontWeight.Bold)
            }

            // Device info section
            SettingsSectionHeader("Device Information", Icons.Filled.PhoneAndroid)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoRow(label = "Device ID", value = state.deviceId, valueColor = GoldPrimary)
                    HorizontalDivider(color = NavyLight)
                    InfoRow(label = "App Version", value = "1.0.0")
                    HorizontalDivider(color = NavyLight)
                    InfoRow(label = "Admin Active", value = if (state.isAdminActive) "Yes" else "No",
                        valueColor = if (state.isAdminActive) StatusGreen else StatusRed)
                    HorizontalDivider(color = NavyLight)
                    InfoRow(label = "Device Owner", value = if (state.isDeviceOwner) "Yes" else "No",
                        valueColor = if (state.isDeviceOwner) StatusGreen else TextSecondary)
                }
            }

            // Kiosk section
            SettingsSectionHeader("Kiosk & Security", Icons.Filled.Security)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    // Kiosk mode toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Kiosk Mode", color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                            Text("Lock device to this app", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = state.isKioskEnabled,
                            onCheckedChange = { enabled ->
                                val activity = context as? androidx.activity.ComponentActivity
                                activity?.let { viewModel.toggleKiosk(enabled, it) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = TealPrimary,
                                checkedThumbColor = DeepNavy
                            )
                        )
                    }

                    if (!state.isAdminActive) {
                        HorizontalDivider(color = NavyLight)
                        TextButton(
                            onClick = {
                                val activity = context as? androidx.activity.ComponentActivity
                                activity?.let { viewModel.requestDeviceAdmin(it) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Request Device Admin", color = GoldPrimary)
                        }
                    }
                }
            }

            // Azan section
            SettingsSectionHeader("Azan Settings", Icons.Filled.Star)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Azan Notifications", color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                        Text("Play azan at prayer times", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = state.azanEnabled,
                        onCheckedChange = { viewModel.toggleAzan(it) },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = GoldPrimary,
                            checkedThumbColor = DeepNavy
                        )
                    )
                }
            }

            // Sound sync section
            SettingsSectionHeader("Sound Files", Icons.Filled.MusicNote)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    // Sync interval
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sync Interval", color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                            Box {
                                TextButton(onClick = { showSyncIntervalMenu = true }) {
                                    Text("Every ${state.syncIntervalHours}h", color = TealPrimary)
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = TealPrimary)
                                }
                                DropdownMenu(
                                    expanded = showSyncIntervalMenu,
                                    onDismissRequest = { showSyncIntervalMenu = false },
                                    modifier = Modifier.background(BgElevated)
                                ) {
                                    listOf(1, 2, 4, 6, 12, 24).forEach { hours ->
                                        DropdownMenuItem(
                                            text = { Text("Every ${hours}h", color = TextPrimary) },
                                            onClick = {
                                                viewModel.setSyncInterval(hours)
                                                showSyncIntervalMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = NavyLight)
                    TextButton(
                        onClick = { viewModel.triggerSoundSync() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync Now", color = TealPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logout button
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                border = ButtonDefaults.outlinedButtonBorder.copy()
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout / Disconnect", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Logout confirm dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                containerColor = BgCard,
                title = { Text("Logout?", color = TextPrimary) },
                text = { Text("This will disconnect from the server and clear your session.", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.logout()
                            showLogoutDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                    ) {
                        Text("Logout")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = TealPrimary)
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = TealPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun settingsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TealPrimary,
    unfocusedBorderColor = TextDisabled,
    focusedLabelColor = TealPrimary,
    cursorColor = TealPrimary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
