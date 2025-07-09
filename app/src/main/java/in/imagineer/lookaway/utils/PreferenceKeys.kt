package `in`.imagineer.lookaway.utils

object PreferenceKeys {
    const val PREFERENCE_BASE = "in.imagineer.lookaway.prefs"
    const val EYE_BREAK = "${PREFERENCE_BASE}.eye_break"
    const val START_HOUR = "${EYE_BREAK}_start_hour"
    const val START_MINUTE = "${EYE_BREAK}_start_minute"
    const val END_HOUR = "${EYE_BREAK}_end_hour"
    const val END_MINUTE = "${EYE_BREAK}_end_minute"
    const val INTERVAL_MINUTES = "${EYE_BREAK}_interval_minutes"
    const val NEXT_TRIGGER_TIME = "${EYE_BREAK}_next_trigger_time"
    const val IS_REMINDER_ACTIVE = "${EYE_BREAK}_is_reminder_active"
}
