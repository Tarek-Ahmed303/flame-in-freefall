package com.flame.astronaut.push

import android.util.Log
import com.flame.astronaut.BuildConfig
import com.flame.astronaut.FlameApp
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives the FCM message that POST /simulate (or a real detection) triggers and raises the same full-screen
 * alarm flow as every other alert source — also when the app is in the background.
 *
 * NOTE for the backend: send a *data* message (high priority). A message with a `notification` block is shown by
 * the system tray when the app is in the background and this method is NOT called; the alert then opens when the
 * user taps the notification (MainActivity handles that case).
 */
class FlameMessagingService : FirebaseMessagingService() {

    private val container get() = (application as FlameApp).container

    override fun onNewToken(token: String) {
        container.pushTokens.set(token)
        container.appScope.launch(Dispatchers.IO) { container.authRepository.syncFcmToken() }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Payload shape isn't documented yet: log the keys (and values in debug builds) to see it in Logcat, tag "FlamePush".
        Log.d(TAG, "FCM received: dataKeys=${message.data.keys} hasNotification=${message.notification != null}")
        if (BuildConfig.DEBUG) Log.d(TAG, "FCM data=${message.data} title=${message.notification?.title} body=${message.notification?.body}")

        val alert = parsePushAlert(
            data = message.data,
            title = message.notification?.title,
            body = message.notification?.body,
            messageId = message.messageId,
            fallbackModule = container.authRepository.user.value?.module,
        )
        container.emergencyRepository.onPushAlert(alert)
    }

    private companion object { const val TAG = "FlamePush" }
}
