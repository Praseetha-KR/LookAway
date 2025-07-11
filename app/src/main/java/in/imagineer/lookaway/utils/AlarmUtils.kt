package `in`.imagineer.lookaway.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import java.util.*
import `in`.imagineer.lookaway.receiver.NotificationReceiver

object AlarmUtils {
    fun startReminder(context: Context, preferenceManager: PreferenceManager) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("RESCHEDULE", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalMillis = preferenceManager.intervalMinutes * 60 * 1000L
        val nextTriggerTime = getNextValidTriggerTime(context, preferenceManager, intervalMillis)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            nextTriggerTime,
            pendingIntent
        )

        preferenceManager.nextTriggerTime = nextTriggerTime
    }

    fun stopReminder(context: Context, preferenceManager: PreferenceManager) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        preferenceManager.remove(preferenceManager.keys.NEXT_TRIGGER_TIME)
    }

    private fun getNextValidTriggerTime(
        context: Context,
        preferenceManager: PreferenceManager,
        intervalMillis: Long
    ): Long {
        val startHour = preferenceManager.startHour
        val startMinute = preferenceManager.startMinute
        val endHour = preferenceManager.endHour
        val endMinute = preferenceManager.endMinute

        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)

        val startTimeMinutes = startHour * 60 + startMinute
        val endTimeMinutes = endHour * 60 + endMinute
        val currentTimeMinutes = currentHour * 60 + currentMinute

        return if (currentTimeMinutes in startTimeMinutes until endTimeMinutes) {
            SystemClock.elapsedRealtime() + intervalMillis
        } else if (currentTimeMinutes < startTimeMinutes) {
            val nextStartTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            SystemClock.elapsedRealtime() + (nextStartTime.timeInMillis - System.currentTimeMillis())
        } else {
            val nextStartTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1)
            }
            SystemClock.elapsedRealtime() + (nextStartTime.timeInMillis - System.currentTimeMillis())
        }
    }
}