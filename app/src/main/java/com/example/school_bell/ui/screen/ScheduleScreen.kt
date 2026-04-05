package com.example.school_bell.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.school_bell.data.db.entities.BellSchedule
import com.example.school_bell.data.db.entities.RoutineType
import com.example.school_bell.ui.theme.*
import com.example.school_bell.viewmodel.ScheduleViewModel

@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<BellSchedule?>(null) }
    var deleteConfirmSchedule by remember { mutableStateOf<BellSchedule?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepNavy, BgSurface)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bell Schedules",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.refreshFromServer() }) {
                    Icon(Icons.Filled.Sync, contentDescription = "Sync", tint = TealPrimary)
                }
            }

            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = TealPrimary,
                    trackColor = NavyLight
                )
            }

            state.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusRed.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = StatusRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = error, color = StatusRed, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearError() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = StatusRed)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (state.schedules.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = TextDisabled,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No schedules yet", color = TextSecondary, style = MaterialTheme.typography.titleMedium)
                        Text("Tap + to add a bell schedule", color = TextDisabled, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.schedules, key = { it.id }) { schedule ->
                        ScheduleCard(
                            schedule = schedule,
                            dayNames = viewModel.dayNames,
                            onToggle = { viewModel.toggleScheduleEnabled(schedule) },
                            onEdit = { editingSchedule = schedule },
                            onDelete = { deleteConfirmSchedule = schedule }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = TealPrimary,
            contentColor = DeepNavy
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add schedule")
        }

        // Add/Edit dialog
        if (showAddDialog || editingSchedule != null) {
            ScheduleDialog(
                schedule = editingSchedule,
                availableSounds = viewModel.availableSounds,
                dayNames = viewModel.dayNames,
                onDismiss = {
                    showAddDialog = false
                    editingSchedule = null
                },
                onSave = { label, hour, minute, days, soundFile, routineType ->
                    if (editingSchedule != null) {
                        viewModel.updateSchedule(
                            editingSchedule!!.copy(
                                label = label, hour = hour, minute = minute,
                                days = days, soundFile = soundFile, routineType = routineType
                            )
                        )
                    } else {
                        viewModel.addSchedule(label, hour, minute, days, soundFile, routineType)
                    }
                    showAddDialog = false
                    editingSchedule = null
                }
            )
        }

        // Delete confirmation
        deleteConfirmSchedule?.let { schedule ->
            AlertDialog(
                onDismissRequest = { deleteConfirmSchedule = null },
                containerColor = BgCard,
                title = { Text("Delete Schedule", color = TextPrimary) },
                text = { Text("Delete '${schedule.label}'?", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteSchedule(schedule)
                            deleteConfirmSchedule = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteConfirmSchedule = null }) {
                        Text("Cancel", color = TealPrimary)
                    }
                }
            )
        }
    }
}

@Composable
private fun ScheduleCard(
    schedule: BellSchedule,
    dayNames: List<String>,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.isEnabled) BgCard else BgCard.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type indicator dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when (schedule.routineType) {
                                RoutineType.AZAN -> GoldPrimary
                                RoutineType.CUSTOM -> TealLight
                                else -> TealPrimary
                            }
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = schedule.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (schedule.isEnabled) TextPrimary else TextDisabled,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = String.format("%02d:%02d", schedule.hour, schedule.minute),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (schedule.isEnabled) TealPrimary else TextDisabled,
                        fontWeight = FontWeight.Bold
                    )
                }
                Switch(
                    checked = schedule.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = TealPrimary,
                        checkedThumbColor = DeepNavy
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                dayNames.forEachIndexed { index, day ->
                    val active = schedule.days and (1 shl index) != 0
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (active) TealDark else NavyLight,
                        modifier = Modifier.size(width = 36.dp, height = 24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = day.take(2),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) TealLight else TextDisabled
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sound file and actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = TextDisabled, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = schedule.soundFile,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDisabled,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = TealPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = StatusRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ScheduleDialog(
    schedule: BellSchedule?,
    availableSounds: List<String>,
    dayNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Int, String, RoutineType) -> Unit
) {
    var label by remember { mutableStateOf(schedule?.label ?: "") }
    var hour by remember { mutableStateOf(schedule?.hour ?: 8) }
    var minute by remember { mutableStateOf(schedule?.minute ?: 0) }
    var selectedDays by remember {
        mutableStateOf(
            (0..6).filter { (schedule?.days ?: 0b0111110) and (1 shl it) != 0 }.toMutableSet()
        )
    }
    var selectedSound by remember { mutableStateOf(schedule?.soundFile ?: availableSounds.first()) }
    var selectedType by remember { mutableStateOf(schedule?.routineType ?: RoutineType.SCHOOL) }
    var showSoundMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        title = {
            Text(
                if (schedule == null) "Add Bell Schedule" else "Edit Schedule",
                color = TextPrimary, fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary, unfocusedBorderColor = TextDisabled,
                        focusedLabelColor = TealPrimary, focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Time picker row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Time:", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    NumberPicker(label = "Hour", value = hour, range = 0..23, onValueChange = { hour = it })
                    Text(":", color = TextPrimary, fontWeight = FontWeight.Bold)
                    NumberPicker(label = "Min", value = minute, range = 0..59, onValueChange = { minute = it })
                }

                // Days selector
                Text("Days:", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayNames.forEachIndexed { index, day ->
                        val active = index in selectedDays
                        FilterChip(
                            selected = active,
                            onClick = {
                                selectedDays = selectedDays.toMutableSet().apply {
                                    if (active) remove(index) else add(index)
                                }
                            },
                            label = { Text(day.take(2), style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealDark,
                                selectedLabelColor = TealLight,
                                containerColor = NavyLight,
                                labelColor = TextDisabled
                            )
                        )
                    }
                }

                // Sound file dropdown
                Box {
                    OutlinedTextField(
                        value = selectedSound,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sound File") },
                        trailingIcon = {
                            IconButton(onClick = { showSoundMenu = true }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary, unfocusedBorderColor = TextDisabled,
                            focusedLabelColor = TealPrimary, focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    DropdownMenu(
                        expanded = showSoundMenu,
                        onDismissRequest = { showSoundMenu = false },
                        modifier = Modifier.background(BgElevated)
                    ) {
                        availableSounds.forEach { sound ->
                            DropdownMenuItem(
                                text = { Text(sound, color = TextPrimary) },
                                onClick = { selectedSound = sound; showSoundMenu = false }
                            )
                        }
                    }
                }

                // Type dropdown
                Box {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = {
                            IconButton(onClick = { showTypeMenu = true }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary, unfocusedBorderColor = TextDisabled,
                            focusedLabelColor = TealPrimary, focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false },
                        modifier = Modifier.background(BgElevated)
                    ) {
                        RoutineType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name, color = TextPrimary) },
                                onClick = { selectedType = type; showTypeMenu = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val daysMask = selectedDays.fold(0) { acc, i -> acc or (1 shl i) }
                    onSave(label, hour, minute, daysMask, selectedSound, selectedType)
                },
                enabled = label.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = DeepNavy)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun NumberPicker(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { if (value > range.first) onValueChange(value - 1) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = TealPrimary)
        }
        Text(
            text = String.format("%02d", value),
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        IconButton(
            onClick = { if (value < range.last) onValueChange(value + 1) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Increase", tint = TealPrimary)
        }
    }
}
