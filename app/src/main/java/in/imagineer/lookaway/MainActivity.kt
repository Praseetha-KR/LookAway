package `in`.imagineer.lookaway

import java.util.*
import kotlinx.coroutines.*
import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
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
import `in`.imagineer.lookaway.receiver.NotificationReceiver
import `in`.imagineer.lookaway.ui.screens.EyeBreakScreen
import `in`.imagineer.lookaway.ui.theme.LookAwayTheme
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

            if (savedNextTriggerTime > 0L) {
                val timeUntilNextReminder = savedNextTriggerTime - SystemClock.elapsedRealtime()
                if (timeUntilNextReminder > 0) {
                    // Still time left until next reminder
                    startCountdown(timeUntilNextReminder, preferenceManager)
                } else {
                    // Time has passed, calculate next occurrence
                    val intervalMillis = intervalMinutes * 60 * 1000L
                    val nextTriggerTime = getNextValidTriggerTime(intervalMillis)
                    startCountdown(nextTriggerTime - SystemClock.elapsedRealtime(), preferenceManager)
                }
            }
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
                        },
                        onIntervalChange = { interval ->
                            intervalMinutes = interval
                            saveTimePreferences(preferenceManager)
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
        } else {
            stopReminder(preferenceManager)
        }
        isReminderActive = !isReminderActive
        saveTimePreferences(preferenceManager)
    }

    private fun startReminder(preferenceManager: PreferenceManager) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val intervalMillis = intervalMinutes * 60 * 1000L
        val nextTriggerTime = getNextValidTriggerTime(intervalMillis)

        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            nextTriggerTime,
            intervalMillis,
            pendingIntent
        )

        preferenceManager.nextTriggerTime = nextTriggerTime
        startCountdown(nextTriggerTime - SystemClock.elapsedRealtime(), preferenceManager)
    }

    private fun stopReminder(preferenceManager: PreferenceManager) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        countdownJob?.cancel()
        timeUntilNext = 0L

        preferenceManager.remove(preferenceManager.keys.NEXT_TRIGGER_TIME)
    }

    private fun getNextValidTriggerTime(intervalMillis: Long): Long {
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)

        val startTimeMinutes = startHour * 60 + startMinute
        val endTimeMinutes = endHour * 60 + endMinute
        val currentTimeMinutes = currentHour * 60 + currentMinute

        return if (currentTimeMinutes in startTimeMinutes until endTimeMinutes) {
            // We're in active hours, next trigger is in the specified interval
            SystemClock.elapsedRealtime() + intervalMillis
        } else if (currentTimeMinutes < startTimeMinutes) {
            // Before start time today - trigger at start time
            val nextStartTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val timeDifference = nextStartTime.timeInMillis - System.currentTimeMillis()
            SystemClock.elapsedRealtime() + timeDifference
        } else {
            // After end time today - trigger at start time tomorrow
            val nextStartTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val timeDifference = nextStartTime.timeInMillis - System.currentTimeMillis()
            SystemClock.elapsedRealtime() + timeDifference
        }
    }

    private fun startCountdown(initialTimeMillis: Long, preferenceManager: PreferenceManager) {
        countdownJob?.cancel()
        timeUntilNext = initialTimeMillis

        countdownJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                delay(1000)
                timeUntilNext -= 1000

                if (timeUntilNext <= 0) {
                    // Reset the next trigger time
                    val intervalMillis = intervalMinutes * 60 * 1000L
                    val nextTriggerTime = getNextValidTriggerTime(intervalMillis)
                    timeUntilNext = nextTriggerTime - SystemClock.elapsedRealtime()

                    preferenceManager.nextTriggerTime = nextTriggerTime
                }
            }
        }
    }
}
