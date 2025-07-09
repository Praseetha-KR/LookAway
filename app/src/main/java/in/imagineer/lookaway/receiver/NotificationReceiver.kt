package `in`.imagineer.lookaway.receiver

import java.util.*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import `in`.imagineer.lookaway.utils.PreferenceManager

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        var preferenceManager = PreferenceManager(context)
        val startHour = preferenceManager.startHour
        val startMinute = preferenceManager.startMinute
        val endHour = preferenceManager.endHour
        val endMinute = preferenceManager.endMinute

        val startTimeMinutes = startHour * 60 + startMinute
        val endTimeMinutes = endHour * 60 + endMinute
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)
        val currentTimeMinutes = currentHour * 60 + currentMinute

        if (currentTimeMinutes in startTimeMinutes until endTimeMinutes) {
            createNotificationChannel(context)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Eye Break Reminder")
                .setContentText("Look at object 20 feet away for 20 seconds")
                .setSmallIcon(android.R.drawable.ic_media_pause)
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
        channel.description = "Notifications to remind you to look away from screens"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "look_away_eye_break_channel"
        const val NOTIFICATION_ID = 1
    }
}
