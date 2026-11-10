package com.flame.astronaut.emergency

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.flame.astronaut.MainActivity
import com.flame.astronaut.R
import com.flame.astronaut.domain.AlertPresentation
import com.flame.astronaut.domain.EmergencyRepository
import com.flame.astronaut.domain.FireAlert
import com.flame.astronaut.domain.summaryLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiveAlert(
    val alert: FireAlert,
    val presentation: AlertPresentation,
    val silenced: Boolean = false,
)

/**
 * App-wide owner of the "incoming alert" state + side effects (alarm, vibration, notification).
 * It only depends on [EmergencyRepository], so it works unchanged with a real backend.
 */
class EmergencyCoordinator(
    private val context: Context,
    repository: EmergencyRepository,
    scope: CoroutineScope,
) {
    private val _active = MutableStateFlow<ActiveAlert?>(null)
    val active: StateFlow<ActiveAlert?> = _active.asStateFlow()

    private val alarm = AlarmPlayer(context)
    private val notifier = AlertNotifier(context)

    /** Alerts are held back (no sound, vibration, notification or screen) until the user is signed in. */
    // Persisted so a push that wakes a killed process after a normal session still rings.
    private val prefs = context.getSharedPreferences("flame_session", Context.MODE_PRIVATE)
    private var signedIn = prefs.getBoolean("signed_in", false)
    private var alarmRaised = false

    init {
        scope.launch {
            repository.alerts.collect { event ->
                _active.value = ActiveAlert(event.alert, event.presentation)
                alarmRaised = false
                raiseIfReady()
            }
        }
    }

    fun setSignedIn(value: Boolean) {
        signedIn = value
        prefs.edit().putBoolean("signed_in", value).apply()
        if (!value) {
            // signed out / on splash: make sure nothing keeps ringing
            alarm.stop(); notifier.cancel(); alarmRaised = false
        } else raiseIfReady()
    }

    private fun raiseIfReady() {
        val a = _active.value ?: return
        if (!signedIn || alarmRaised || a.silenced) return
        alarmRaised = true
        alarm.start()
        if (!isForeground()) notifier.show(a.alert)
    }

    fun silence() {
        alarm.stop()
        _active.update { it?.copy(silenced = true) }
    }

    /** User acknowledged → stop alarm and clear the incoming-alert UI. */
    fun acknowledge() {
        alarm.stop()
        notifier.cancel()
        _active.value = null
    }

    private fun isForeground() =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}

private class AlarmPlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun start() {
        stop()
        // SOS: ... --- ... (dot 150, dash 450)
        val pattern = longArrayOf(0, 150, 100, 150, 100, 150, 300, 450, 100, 450, 100, 450, 300, 150, 100, 150, 100, 150, 1200)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        // MediaPlayer loops on every API level (Ringtone#setLooping needs API 28, our minSdk is 26).
        runCatching {
            // Bundled emergency siren (res/raw/alarm_siren.wav) so every phone sounds the same; the system alarm
            // tone is only a fallback.
            val bundled = Uri.parse("android.resource://${context.packageName}/${R.raw.alarm_siren}")
            val systemTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            player = runCatching { newAlarmPlayer(bundled) }.getOrElse { newAlarmPlayer(systemTone) }
        }
    }

    private fun newAlarmPlayer(uri: Uri) = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        setDataSource(context, uri)
        com.flame.astronaut.AppPrefs.alarmVolume.let { setVolume(it, it) }
        isLooping = true
        prepare()
        start()
    }

    fun stop() {
        vibrator?.cancel()
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }
}

private class AlertNotifier(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        val channel = NotificationChannel(CHANNEL, "Fire alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Critical fire emergency alerts"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun show(alert: FireAlert) {
        val open = pendingIntent(MainActivity.ACTION_OPEN, 1)
        val silence = pendingIntent(MainActivity.ACTION_SILENCE, 2)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_flame)
            .setContentTitle("Fire detected — ${alert.module}")
            .setContentText(alert.summaryLine())
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setColor(0xFFFF4538.toInt())
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .addAction(0, "Open steps", open)
            .addAction(0, "Silence alarm", silence)
            .build()
        runCatching { manager.notify(ID, n) }
    }

    fun cancel() = manager.cancel(ID)

    private fun pendingIntent(action: String, code: Int): PendingIntent {
        val i = Intent(context, MainActivity::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val CHANNEL = "fire_alerts"
        const val ID = 4201
    }
}
