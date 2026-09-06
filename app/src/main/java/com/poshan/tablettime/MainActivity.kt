package com.poshan.tablettime

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.poshan.tablettime.ui.home.HomeScreen
import com.poshan.tablettime.ui.setup.AddReminderScreen
import com.poshan.tablettime.ui.theme.TabletTimeTheme
import com.poshan.tablettime.viewmodel.HomeViewModel
import com.poshan.tablettime.viewmodel.ReminderViewModel

sealed class Screen {
    object Home : Screen()
    data class AddEdit(val reminderId: Long) : Screen()
}

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val reminderViewModel: ReminderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TabletTimeTheme {
                MainApp(
                    homeViewModel = homeViewModel,
                    reminderViewModel = reminderViewModel,
                    onRequestExactAlarm = { openExactAlarmSettings() },
                    onRequestNotification = { requestNotificationPermission() },
                    onRequestBatteryOptimization = { requestIgnoreBatteryOptimization() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.refreshPermissions()
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback to app details
                openAppDetailsSettings()
            }
        }
    }

    private var requestNotificationLauncher: (() -> Unit)? = null

    private fun requestNotificationPermission() {
        requestNotificationLauncher?.invoke()
    }

    private fun openAppDetailsSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                }
            }
        }
    }

    @Composable
    private fun MainApp(
        homeViewModel: HomeViewModel,
        reminderViewModel: ReminderViewModel,
        onRequestExactAlarm: () -> Unit,
        onRequestNotification: () -> Unit,
        onRequestBatteryOptimization: () -> Unit
    ) {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

        // Notification permission launcher for Android 13+
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            homeViewModel.refreshPermissions()
            if (!isGranted) {
                openAppDetailsSettings()
            }
        }

        requestNotificationLauncher = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn().togetherWith(fadeOut())
                },
                label = "screenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Home -> {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onAddReminderClick = {
                                reminderViewModel.initForNew()
                                currentScreen = Screen.AddEdit(0L)
                            },
                            onEditReminderClick = { id ->
                                currentScreen = Screen.AddEdit(id)
                            },
                            onRequestExactAlarmPermission = onRequestExactAlarm,
                            onRequestNotificationPermission = onRequestNotification
                        )
                    }
                    is Screen.AddEdit -> {
                        AddReminderScreen(
                            viewModel = reminderViewModel,
                            reminderId = screen.reminderId,
                            onNavigateBack = {
                                currentScreen = Screen.Home
                                homeViewModel.refreshPermissions()
                            }
                        )
                    }
                }
            }
        }
    }
}
