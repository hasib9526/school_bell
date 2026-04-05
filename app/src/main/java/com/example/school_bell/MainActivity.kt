package com.example.school_bell

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.school_bell.data.prefs.AppPreferences
import com.example.school_bell.kiosk.KioskManager
import com.example.school_bell.service.MonitoringService
import com.example.school_bell.ui.navigation.NavGraph
import com.example.school_bell.ui.screen.LoginScreen
import com.example.school_bell.ui.theme.SchoolBellTheme
import com.example.school_bell.worker.SoundSyncWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferences: AppPreferences
    private lateinit var kioskManager: KioskManager

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions result handled; AzanViewModel will check on its own
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Handle notification permission result */ }

    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Refresh kiosk state after admin permission response
        if (result.resultCode == RESULT_OK) {
            lifecycleScope.launch {
                if (preferences.isKioskEnabled.first()) {
                    kioskManager.startKioskMode(this@MainActivity)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferences = AppPreferences(this)
        kioskManager = KioskManager(this)

        requestPermissions()
        startMonitoringService()

        setContent {
            SchoolBellTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-apply kiosk mode if it should be active
        lifecycleScope.launch {
            val kioskEnabled = preferences.isKioskEnabled.first()
            if (kioskEnabled && kioskManager.isAdminActive && !kioskManager.isInLockTaskMode()) {
                kioskManager.startKioskMode(this@MainActivity)
            }
        }
    }

    private fun requestPermissions() {
        val locationPermissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            locationPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (locationPermissions.isNotEmpty()) {
            locationPermissionLauncher.launch(locationPermissions.toTypedArray())
        }

        // Notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startMonitoringService() {
        val intent = Intent(this, MonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        SoundSyncWorker.schedule(this)
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context) }
    val authToken by preferences.authToken.collectAsState(initial = null)

    // TODO: Restore login gate when API is ready
    // when {
    //     authToken.isNullOrEmpty() -> LoginScreen(onLoginSuccess = {})
    //     else -> NavGraph()
    // }
    NavGraph()
}
