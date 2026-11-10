package com.flame.astronaut.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Holds the device's FCM token. Works without Firebase configured: [available] is false and
 * [token] stays null until `app/google-services.json` is added (see app/build.gradle.kts).
 */
class PushTokens(private val context: Context) {
    private val prefs = context.getSharedPreferences("flame_push", Context.MODE_PRIVATE)

    private val _token = MutableStateFlow(prefs.getString(KEY, null))
    val token: StateFlow<String?> = _token.asStateFlow()

    val available: Boolean get() = FirebaseApp.getApps(context).isNotEmpty()

    fun refresh() {
        if (!available) return
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { set(it) }
        }
    }

    /** Current token from Firebase (falls back to the stored one). Null when Firebase isn't configured. */
    suspend fun fetch(): String? {
        if (!available) return token.value
        return suspendCancellableCoroutine { cont ->
            runCatching {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { t ->
                    val value = if (t.isSuccessful) t.result?.also { set(it) } else token.value
                    if (cont.isActive) cont.resume(value)
                }
            }.onFailure { if (cont.isActive) cont.resume(token.value) }
        }
    }

    fun set(token: String) {
        prefs.edit().putString(KEY, token).apply()
        _token.value = token
    }

    private companion object { const val KEY = "fcm_token" }
}
