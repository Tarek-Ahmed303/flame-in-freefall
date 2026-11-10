package com.flame.astronaut

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import android.util.Log
import com.flame.astronaut.di.LocalContainer
import com.flame.astronaut.push.parsePushAlert
import com.flame.astronaut.ui.nav.AppNavHost
import com.flame.astronaut.ui.theme.FlameTheme

class MainActivity : ComponentActivity() {

    private val container get() = (application as FlameApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        // Samsung One UI draws a contrast scrim behind the 3-button bar unless this is switched off.
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightNavigationBars = false // light icons on the dark background
            isAppearanceLightStatusBars = false
        }
        // Behave like an incoming call: show over the lock screen and wake the display.
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        handleIntent(intent)
        setContent {
            val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            CompositionLocalProvider(LocalContainer provides container) {
                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                    FlameTheme { AppNavHost() }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_SILENCE) container.emergencyCoordinator.silence()

        // Tapped an FCM notification while the app was in the background (message had a `notification` block):
        // the data keys arrive as intent extras. Log them and open the alert. The message id de-duplicates re-deliveries.
        val extras = intent?.extras
        val messageId = extras?.getString("google.message_id")
        if (extras != null && messageId != null) {
            val data = extras.keySet()
                .filter { !it.startsWith("google.") && !it.startsWith("gcm.") && it != "from" && it != "collapse_key" }
                .associateWith { extras.get(it)?.toString().orEmpty() }
            Log.d("FlamePush", "Opened from notification: dataKeys=${data.keys}")
            if (BuildConfig.DEBUG) Log.d("FlamePush", "Opened from notification data=$data")
            container.emergencyRepository.onPushAlert(
                parsePushAlert(data, title = null, body = null, messageId = messageId, fallbackModule = container.authRepository.user.value?.module),
            )
        }
    }

    companion object {
        const val ACTION_OPEN = "com.flame.astronaut.OPEN_ALERT"
        const val ACTION_SILENCE = "com.flame.astronaut.SILENCE_ALERT"
    }
}
