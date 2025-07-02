package `in`.imagineer.lookaway

import java.util.*
import java.util.Locale
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import `in`.imagineer.lookaway.ui.theme.LookAwayTheme

class MainActivity : ComponentActivity() {
    private var isReminderActive by mutableStateOf(false)
    private var timeUntilNext by mutableLongStateOf(0L)
    private var countdownJob: Job? = null
    private var startHour by mutableIntStateOf(10)
    private var startMinute by mutableIntStateOf(0)
    private var endHour by mutableIntStateOf(22)
    private var endMinute by mutableIntStateOf(0)
    private var intervalMinutes by mutableIntStateOf(20)
    private var hasNotificationPermission by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (!isGranted) {
            // If permission denied, stop reminders if they were active
            if (isReminderActive) {
                stopReminder()
                isReminderActive = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadTimePreferences()

        if (isReminderActive) {
            val prefs = getSharedPreferences("eye_break_prefs", Context.MODE_PRIVATE)
            val savedNextTriggerTime = prefs.getLong("next_trigger_time", 0L)

            if (savedNextTriggerTime > 0L) {
                val timeUntilNextReminder = savedNextTriggerTime - SystemClock.elapsedRealtime()
                if (timeUntilNextReminder > 0) {
                    // Still time left until next reminder
                    startCountdown(timeUntilNextReminder)
                } else {
                    // Time has passed, calculate next occurrence
                    val intervalMillis = intervalMinutes * 60 * 1000L
                    val nextTriggerTime = getNextValidTriggerTime(intervalMillis)
                    startCountdown(nextTriggerTime - SystemClock.elapsedRealtime())
                }
            }
        }

        hasNotificationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        if (!hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
                        onToggle = { toggleReminder() },
                        onTimeChange = { sH, sM, eH, eM ->
                            startHour = sH
                            startMinute = sM
                            endHour = eH
                            endMinute = eM
                            saveTimePreferences()
                        },
                        onIntervalChange = { interval ->
                            intervalMinutes = interval
                            saveTimePreferences()
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun toggleReminder() {
        if (!isReminderActive) {
            // Starting reminders - check permission first
            if (!hasNotificationPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
            startReminder()
        } else {
            stopReminder()
        }
        isReminderActive = !isReminderActive
        saveTimePreferences()
    }

    private fun startReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalMillis = intervalMinutes * 60 * 1000L // Use variable instead of hardcoded value
        val nextTriggerTime = getNextValidTriggerTime(intervalMillis)

        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            nextTriggerTime,
            intervalMillis,
            pendingIntent
        )

        val prefs = getSharedPreferences("eye_break_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putLong("next_trigger_time", nextTriggerTime)
        }

        startCountdown(nextTriggerTime - SystemClock.elapsedRealtime())
    }

    private fun stopReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        countdownJob?.cancel()
        timeUntilNext = 0L

        val prefs = getSharedPreferences("eye_break_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            remove("next_trigger_time")
        }
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

    private fun startCountdown(initialTimeMillis: Long) {
        countdownJob?.cancel()
        timeUntilNext = initialTimeMillis

        countdownJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                delay(1000)
                timeUntilNext -= 1000

                if (timeUntilNext <= 0) {
                    // Calculate next valid trigger time and save it
                    val intervalMillis = intervalMinutes * 60 * 1000L
                    val nextTriggerTime = getNextValidTriggerTime(intervalMillis)
                    timeUntilNext = nextTriggerTime - SystemClock.elapsedRealtime()

                    val prefs = getSharedPreferences("eye_break_prefs", Context.MODE_PRIVATE)
                    prefs.edit {
                        putLong("next_trigger_time", nextTriggerTime)
                    }
                }
            }
        }
    }

    private fun saveTimePreferences() {
        val prefs = getSharedPreferences("eye_break_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putInt("start_hour", startHour)
            putInt("start_minute", startMinute)
            putInt("end_hour", endHour)
            putInt("end_minute", endMinute)
            putInt("interval_minutes", intervalMinutes)
            putBoolean("is_reminder_active", isReminderActive)
        }
    }

    private fun loadTimePreferences() {
        val prefs = getSharedPreferences("eye_break_prefs", Context.MODE_PRIVATE)
        startHour = prefs.getInt("start_hour", 10)
        startMinute = prefs.getInt("start_minute", 0)
        endHour = prefs.getInt("end_hour", 22)
        endMinute = prefs.getInt("end_minute", 0)
        intervalMinutes = prefs.getInt("interval_minutes", 20)
        isReminderActive = prefs.getBoolean("is_reminder_active", false)
    }
}

@Composable
fun EyeBreakScreen(
    isActive: Boolean,
    timeUntilNext: Long,
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    intervalMinutes: Int,
    hasNotificationPermission: Boolean,
    onToggle: () -> Unit,
    onTimeChange: (Int, Int, Int, Int) -> Unit,
    onIntervalChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "🌿 LookAway",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Eye Break Reminder",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Look at an object",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = "20 feet away",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = "for 20 seconds",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = "every $intervalMinutes minutes ",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (!isActive) {
            // Time configuration
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Start:")
                OutlinedTextField(
                    value = String.format(Locale.getDefault(), "%02d", startHour),
                    onValueChange = {
                        if (it.toIntOrNull() in 0..23) onTimeChange(
                            it.toInt(),
                            startMinute,
                            endHour,
                            endMinute
                        )
                    },
                    modifier = Modifier.width(60.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text(":")
                OutlinedTextField(
                    value = String.format(Locale.getDefault(), "%02d", startMinute),
                    onValueChange = {
                        if (it.toIntOrNull() in 0..59) onTimeChange(
                            startHour,
                            it.toInt(),
                            endHour,
                            endMinute
                        )
                    },
                    modifier = Modifier.width(60.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("End:")
                OutlinedTextField(
                    value = String.format(Locale.getDefault(), "%02d", endHour),
                    onValueChange = {
                        if (it.toIntOrNull() in 0..23) onTimeChange(
                            startHour,
                            startMinute,
                            it.toInt(),
                            endMinute
                        )
                    },
                    modifier = Modifier.width(60.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text(":")
                OutlinedTextField(
                    value = String.format(Locale.getDefault(), "%02d", endMinute),
                    onValueChange = {
                        if (it.toIntOrNull() in 0..59) onTimeChange(
                            startHour,
                            startMinute,
                            endHour,
                            it.toInt()
                        )
                    },
                    modifier = Modifier.width(60.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Interval (minutes):")
                OutlinedTextField(
                    value = intervalMinutes.toString(),
                    onValueChange = {
                        val newInterval = it.toIntOrNull()
                        if (newInterval != null && newInterval in 1..120) {
                            onIntervalChange(newInterval)
                        }
                    },
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onToggle,
            enabled = hasNotificationPermission || isActive,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Filled.Close else Icons.Filled.PlayArrow,
                    contentDescription = if (isActive) "Stop" else "Play"
                )
                Text(
                    text = when {
                        isActive -> "Stop Reminders"
                        hasNotificationPermission -> "Start Reminders"
                        else -> "Grant Permission to Start"
                    }
                )
            }
        }

        if (!hasNotificationPermission && !isActive) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Notification permission required for reminders",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (isActive && timeUntilNext > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            val hours = TimeUnit.MILLISECONDS.toHours(timeUntilNext)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeUntilNext) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(timeUntilNext) % 60

            Text(
                text = if (hours > 0) "Next reminder in: ${hours}h ${minutes}m ${seconds}s"
                else "Next reminder in: ${minutes}m ${seconds}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (isActive) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Active hours: ${String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute)} - ${String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}