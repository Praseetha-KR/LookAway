package `in`.imagineer.lookaway

import java.util.*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Check if current time is within active hours
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)

        val prefs = context.getSharedPreferences("eye_break_prefs", Context.MODE_PRIVATE)
        val startHour = prefs.getInt("start_hour", 10)
        val startMinute = prefs.getInt("start_minute", 0)
        val endHour = prefs.getInt("end_hour", 22)
        val endMinute = prefs.getInt("end_minute", 0)

        val startTimeMinutes = startHour * 60 + startMinute
        val endTimeMinutes = endHour * 60 + endMinute
        val currentTimeMinutes = currentHour * 60 + currentMinute

        if (currentTimeMinutes in startTimeMinutes until endTimeMinutes) {
            createNotificationChannel(context)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Eye Break Reminder")
                .setContentText("Look at object 20 feet away for 20 seconds")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Eye Break Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "Notifications to remind you to look away from screen"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "eye_break_channel"
        const val NOTIFICATION_ID = 1
    }
}
