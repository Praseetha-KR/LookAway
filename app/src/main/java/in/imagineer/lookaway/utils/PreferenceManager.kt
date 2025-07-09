package `in`.imagineer.lookaway.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferenceManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PreferenceKeys.PREFERENCE_BASE,
        Context.MODE_PRIVATE
    )

    val keys = PreferenceKeys

    var startHour: Int
        get() = prefs.getInt(PreferenceKeys.START_HOUR, 10)
        set(value) = prefs.edit() {
            putInt(
                PreferenceKeys.START_HOUR,
                value
            )
        }

    var startMinute: Int
        get() = prefs.getInt(PreferenceKeys.START_MINUTE, 0)
        set(value) = prefs.edit() {
            putInt(
                PreferenceKeys.START_MINUTE,
                value
            )
        }

    var endHour: Int
        get() = prefs.getInt(PreferenceKeys.END_HOUR, 22)
        set(value) = prefs.edit() {
            putInt(
                PreferenceKeys.END_HOUR,
                value
            )
        }

    var endMinute: Int
        get() = prefs.getInt(PreferenceKeys.END_MINUTE, 0)
        set(value) = prefs.edit() {
            putInt(
                PreferenceKeys.END_MINUTE,
                value
            )
        }

    var intervalMinutes: Int
        get() = prefs.getInt(PreferenceKeys.INTERVAL_MINUTES, 20)
        set(value) = prefs.edit() {
            putInt(
                PreferenceKeys.INTERVAL_MINUTES,
                value
            )
        }

    var nextTriggerTime: Long
        get() = prefs.getLong(PreferenceKeys.NEXT_TRIGGER_TIME, 0L)
        set(value) = prefs.edit() {
            putLong(
                PreferenceKeys.NEXT_TRIGGER_TIME,
                value
            )
        }

    var isReminderActive: Boolean
        get() = prefs.getBoolean(PreferenceKeys.IS_REMINDER_ACTIVE, false)
        set(value) = prefs.edit() {
            putBoolean(
                PreferenceKeys.IS_REMINDER_ACTIVE,
                value
            )
        }

    fun remove(key: String) {
        prefs.edit() {
            remove(key)
        }
    }
}