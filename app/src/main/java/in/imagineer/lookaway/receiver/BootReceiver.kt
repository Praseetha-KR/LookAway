package `in`.imagineer.lookaway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `in`.imagineer.lookaway.utils.AlarmUtils
import `in`.imagineer.lookaway.utils.PreferenceManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val preferenceManager = PreferenceManager(context)
            if (preferenceManager.isReminderActive) {
                AlarmUtils.startReminder(context, preferenceManager)
            }
        }
    }
}
