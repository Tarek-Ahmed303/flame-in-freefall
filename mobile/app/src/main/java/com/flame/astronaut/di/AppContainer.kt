package com.flame.astronaut.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flame.astronaut.BuildConfig
import com.flame.astronaut.assistant.AssistantChatViewModel
import com.flame.astronaut.assistant.AssistantListViewModel
import com.flame.astronaut.data.fake.*
import com.flame.astronaut.data.real.*
import com.flame.astronaut.domain.*
import com.flame.astronaut.emergency.EmergencyCoordinator
import com.flame.astronaut.net.Network
import com.flame.astronaut.net.SessionManager
import com.flame.astronaut.net.TokenStore
import com.flame.astronaut.push.PushTokens
import com.flame.astronaut.ui.auth.ForgotPasswordViewModel
import com.flame.astronaut.ui.auth.LoginViewModel
import com.flame.astronaut.ui.auth.SignUpViewModel
import com.flame.astronaut.ui.auth.VerifyOtpViewModel
import com.flame.astronaut.ui.chat.ChatViewModel
import com.flame.astronaut.ui.emergency.EmergencyViewModel
import com.flame.astronaut.ui.guide.GuideViewModel
import com.flame.astronaut.ui.home.HomeViewModel
import com.flame.astronaut.ui.settings.SettingsViewModel
import com.flame.astronaut.ui.splash.SplashViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Composition root. The ONLY place that picks real (Retrofit) vs fake repositories, by BuildConfig.USE_FAKE.
 * Backend URL: BuildConfig.BASE_URL (see app/build.gradle.kts).
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val useFake = BuildConfig.USE_FAKE

    val pushTokens = PushTokens(appContext).also { it.refresh() }
    val tokenStore = TokenStore(appContext)
    val session = SessionManager(tokenStore)
    private val network by lazy { Network(tokenStore, session) }

    private val keepAwake: (Long) -> Unit = { ms ->
        // keep the CPU awake so a scheduled demo alert still fires with the screen off
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "flame:demo").acquire(ms)
    }

    val authRepository: AuthRepository =
        if (useFake) FakeAuthRepository() else RealAuthRepository(network.api, tokenStore, pushTokens, appScope)
    val emergencyRepository: EmergencyRepository =
        if (useFake) FakeEmergencyRepository(appScope, keepAwake) else RealEmergencyRepository(network.api, appScope, keepAwake)
    val guideRepository: GuideRepository =
        if (useFake) FakeGuideRepository() else RealGuideRepository(appContext, network.api)
    val chatRepository: ChatRepository =
        if (useFake) FakeChatRepository(appContext) else RealChatRepository(network.api)
    val riskRepository: RiskRepository = if (useFake) FakeRiskRepository() else RealRiskRepository()
    val healthRepository: HealthRepository = if (useFake) FakeHealthRepository() else RealHealthRepository()

    val emergencyCoordinator = EmergencyCoordinator(appContext, emergencyRepository, appScope)

    val viewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer { SplashViewModel(healthRepository) }
        initializer { LoginViewModel(authRepository) }
        initializer { SignUpViewModel(authRepository) }
        initializer { VerifyOtpViewModel(authRepository, createSavedStateHandle()) }
        initializer { ForgotPasswordViewModel(authRepository) }
        initializer { HomeViewModel(riskRepository, emergencyRepository, authRepository) }
        initializer { ChatViewModel(guideRepository, emergencyRepository) }
        initializer { AssistantListViewModel(chatRepository) }
        initializer {
            val handle: SavedStateHandle = createSavedStateHandle()
            AssistantChatViewModel(chatRepository, handle)
        }
        initializer { EmergencyViewModel(emergencyCoordinator) }
        initializer { GuideViewModel(guideRepository) }
        initializer { SettingsViewModel(authRepository, pushTokens) }
    }
}

val LocalContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
