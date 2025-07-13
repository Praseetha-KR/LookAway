package `in`.imagineer.lookaway

import kotlinx.coroutines.*
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import `in`.imagineer.lookaway.ui.screens.EyeBreakScreen
import `in`.imagineer.lookaway.ui.theme.LookAwayTheme
import `in`.imagineer.lookaway.utils.AlarmUtils
import `in`.imagineer.lookaway.utils.PreferenceManager


class MainActivity : ComponentActivity() {
    private var hasNotificationPermission by mutableStateOf(false)
    private lateinit var preferenceManager: PreferenceManager

    private var startHour by mutableIntStateOf(10)
    private var startMinute by mutableIntStateOf(0)
    private var endHour by mutableIntStateOf(22)
    private var endMinute by mutableIntStateOf(0)
    private var intervalMinutes by mutableIntStateOf(20)

    private var timeUntilNext by mutableLongStateOf(0L)
    private var countdownJob: Job? = null
    private var isReminderActive by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (!isGranted) {
            // If permission denied, stop reminders if they were active
            if (isReminderActive) {
                stopReminder(preferenceManager)
                isReminderActive = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager = PreferenceManager(this)

        enableEdgeToEdge()
        loadTimePreferences(preferenceManager)

        // Show notification permission launcher if not already granted
        hasNotificationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Resume countdown on app restart
        if (isReminderActive) {
            val savedNextTriggerTime = preferenceManager.nextTriggerTime
            val currentTime = SystemClock.elapsedRealtime()
    
            if (currentTime > savedNextTriggerTime) {
                // Time has passed or device was rebooted, restart
                AlarmUtils.stopReminder(this, preferenceManager)
                AlarmUtils.startReminder(this, preferenceManager)
            }

            countdownJob?.cancel()
            countdownJob = AlarmUtils.startCountdown(
                preferenceManager,
                onTick = { timeUntilNext ->
                    this.timeUntilNext = timeUntilNext
                },
            )
        }

        setContent {
            LookAwayTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    EyeBreakScreen(
                        isActive = isReminderActive,
                        timeUntilNext = timeUntilNext,
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute,
                        intervalMinutes = intervalMinutes,
                        hasNotificationPermission = hasNotificationPermission,
                        onToggle = { toggleReminder(preferenceManager) },
                        onTimeChange = { sH, sM, eH, eM ->
                            startHour = sH
                            startMinute = sM
                            endHour = eH
                            endMinute = eM
                            saveTimePreferences(preferenceManager)

                            if (isReminderActive) {
                                stopReminder(preferenceManager)
                                startReminder(preferenceManager)
                            }
                        },
                        onIntervalChange = { interval ->
                            intervalMinutes = interval
                            saveTimePreferences(preferenceManager)

                            if (isReminderActive) {
                                stopReminder(preferenceManager)
                                startReminder(preferenceManager)
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun loadTimePreferences(preferenceManager: PreferenceManager) {
        startHour = preferenceManager.startHour
        startMinute = preferenceManager.startMinute
        endHour = preferenceManager.endHour
        endMinute = preferenceManager.endMinute
        intervalMinutes = preferenceManager.intervalMinutes
        isReminderActive = preferenceManager.isReminderActive
    }

    private fun saveTimePreferences(preferenceManager: PreferenceManager) {
        preferenceManager.startHour = startHour
        preferenceManager.startMinute = startMinute
        preferenceManager.endHour = endHour
        preferenceManager.endMinute = endMinute
        preferenceManager.intervalMinutes = intervalMinutes
        preferenceManager.isReminderActive = isReminderActive
    }

    private fun toggleReminder(preferenceManager: PreferenceManager) {
        if (!isReminderActive) {
            if (!hasNotificationPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }

            startReminder(preferenceManager)
            isReminderActive = true
        } else {
            stopReminder(preferenceManager)
            isReminderActive = false
        }
        saveTimePreferences(preferenceManager)
    }

    private fun startReminder(preferenceManager: PreferenceManager) {
        countdownJob?.cancel()
        AlarmUtils.startReminder(this, preferenceManager)
        countdownJob = AlarmUtils.startCountdown(
            preferenceManager,
            onTick = { remaining ->
                timeUntilNext = remaining
            }
        )
    }

    private fun stopReminder(preferenceManager: PreferenceManager) {
        AlarmUtils.stopReminder(this, preferenceManager)
        countdownJob?.cancel()
        timeUntilNext = 0L
    }
}
