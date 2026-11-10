package com.flame.astronaut

import android.content.Context
import android.content.SharedPreferences

/** The two user settings that really change behaviour: alarm loudness and read-aloud speed. */
object AppPrefs {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) prefs = context.applicationContext.getSharedPreferences("flame_prefs", Context.MODE_PRIVATE)
    }

    var alarmVolume: Float
        get() = if (::prefs.isInitialized) prefs.getFloat("alarm_volume", 1f) else 1f
        set(v) { if (::prefs.isInitialized) prefs.edit().putFloat("alarm_volume", v.coerceIn(0f, 1f)).apply() }

    var speechRate: Float
        get() = if (::prefs.isInitialized) prefs.getFloat("speech_rate", 1.2f) else 1.2f
        set(v) { if (::prefs.isInitialized) prefs.edit().putFloat("speech_rate", v.coerceIn(0.5f, 2f)).apply() }
}
