package com.flame.astronaut.ui.nav

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flame.astronaut.assistant.AssistantChatScreen
import com.flame.astronaut.assistant.AssistantChatViewModel
import com.flame.astronaut.assistant.AssistantListScreen
import com.flame.astronaut.assistant.AssistantListViewModel
import com.flame.astronaut.di.LocalContainer
import com.flame.astronaut.domain.AlertPresentation
import com.flame.astronaut.ui.auth.*
import com.flame.astronaut.ui.chat.ChatScreen
import com.flame.astronaut.ui.chat.ChatViewModel
import com.flame.astronaut.ui.components.NavTab
import com.flame.astronaut.ui.emergency.EmergencyAlertScreen
import com.flame.astronaut.ui.emergency.EmergencyViewModel
import com.flame.astronaut.ui.emergency.InAppAlertOverlay
import com.flame.astronaut.ui.guide.GuideScreen
import com.flame.astronaut.ui.guide.GuideViewModel
import com.flame.astronaut.ui.home.HomeScreen
import com.flame.astronaut.ui.home.HomeViewModel
import com.flame.astronaut.ui.settings.SettingsScreen
import com.flame.astronaut.ui.settings.SettingsViewModel
import com.flame.astronaut.ui.theme.C
import com.flame.astronaut.ui.splash.SplashScreen
import com.flame.astronaut.ui.splash.SplashViewModel

object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val SignUp = "signup"
    const val Otp = "otp/{verificationId}/{email}"
    const val Forgot = "forgot"
    const val Home = "home"
    const val Steps = "steps"
    const val Guide = "guide"
    const val Settings = "settings"
    const val Alert = "alert"
    const val AssistantList = "assistant"
    const val AssistantChat = "assistant/chat/{conversationId}?q={q}"

    fun otp(verificationId: String, email: String) = "otp/${Uri.encode(verificationId)}/${Uri.encode(email)}"

    /** conversationId 0 = a new chat; [question] (optional) is asked right away. */
    fun assistantChat(conversationId: Long, question: String? = null) =
        "assistant/chat/$conversationId" + (question?.takeIf { it.isNotBlank() }?.let { "?q=${Uri.encode(it)}" } ?: "")
}

@Composable
fun AppNavHost() {
    val container = LocalContainer.current
    val factory = container.viewModelFactory
    val nav = rememberNavController()

    // Activity-scoped: owns the incoming-alert state across all screens.
    val emergencyVm: EmergencyViewModel = viewModel(factory = factory)
    val active by emergencyVm.active.collectAsStateWithLifecycle()

    // A FULL_SCREEN alert takes over whatever is currently showing (like an incoming call).
    // Not before the user is signed in: re-evaluated when the route changes, so a pending alert shows right after login.
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val signedIn = route != null && route != Routes.Splash && route != Routes.Login &&
        route != Routes.SignUp && route != Routes.Otp && route != Routes.Forgot
    LaunchedEffect(signedIn) { emergencyVm.setSignedIn(signedIn) }

    // The server rejected our token (401): the session is already cleared — go back to the login screen.
    LaunchedEffect(Unit) {
        container.session.loggedOut.collect {
            nav.navigate(Routes.Login) { popUpTo(nav.graph.id) { inclusive = true } }
        }
    }
    LaunchedEffect(active?.alert?.id, active?.presentation, signedIn) {
        if (signedIn && active?.presentation == AlertPresentation.FULL_SCREEN) {
            nav.navigate(Routes.Alert) { launchSingleTop = true }
        }
    }

    fun openSteps() {
        emergencyVm.acknowledge()
        nav.navigate(Routes.Steps) {
            popUpTo(Routes.Alert) { inclusive = true }
            launchSingleTop = true
        }
    }

    val onTab: (NavTab) -> Unit = { tab ->
        when (tab) {
            NavTab.Home -> nav.navigateTab(Routes.Home)
            NavTab.Ai -> nav.navigate(Routes.AssistantList) { launchSingleTop = true }
            NavTab.Guide -> nav.navigateTab(Routes.Guide)
            NavTab.Settings -> nav.navigateTab(Routes.Settings)
        }
    }

    // The dark app background is painted behind everything, including under the (transparent) system bars.
    Box(Modifier.fillMaxSize().background(C.Bg)) {
        NavHost(nav, startDestination = Routes.Splash, modifier = Modifier.fillMaxSize()) {
            composable(Routes.Splash) {
                SplashScreen(viewModel<SplashViewModel>(factory = factory)) {
                    // stay signed in across launches
                    val next = if (container.authRepository.isLoggedIn()) Routes.Home else Routes.Login
                    nav.navigate(next) { popUpTo(Routes.Splash) { inclusive = true } }
                }
            }
            composable(Routes.Login) {
                LoginScreen(
                    viewModel<LoginViewModel>(factory = factory),
                    onLoggedIn = { nav.navigate(Routes.Home) { popUpTo(Routes.Login) { inclusive = true } } },
                    onSignUp = { nav.navigate(Routes.SignUp) },
                    onForgot = { nav.navigate(Routes.Forgot) },
                )
            }
            composable(Routes.SignUp) {
                SignUpScreen(
                    viewModel<SignUpViewModel>(factory = factory),
                    onBack = { nav.popBackStack() },
                    onVerify = { verificationId, email -> nav.navigate(Routes.otp(verificationId, email)) },
                    onLogin = { nav.popBackStack() },
                )
            }
            composable(
                Routes.Otp,
                arguments = listOf(
                    navArgument("verificationId") { type = NavType.StringType },
                    navArgument("email") { type = NavType.StringType },
                ),
            ) {
                VerifyOtpScreen(
                    viewModel<VerifyOtpViewModel>(factory = factory),
                    onBack = { nav.popBackStack() },
                    onVerified = { nav.navigate(Routes.Home) { popUpTo(Routes.Login) { inclusive = true } } },
                )
            }
            composable(Routes.Forgot) {
                ForgotPasswordScreen(viewModel<ForgotPasswordViewModel>(factory = factory), onBack = { nav.popBackStack() })
            }
            composable(Routes.Home) {
                HomeScreen(
                    viewModel<HomeViewModel>(factory = factory),
                    onOpenChat = { nav.navigate(Routes.AssistantList) { launchSingleTop = true } },
                    onNavigate = onTab,
                )
            }
            composable(Routes.Guide) { GuideScreen(viewModel<GuideViewModel>(factory = factory), onTab) }
            composable(Routes.Settings) {
                SettingsScreen(
                    viewModel<SettingsViewModel>(factory = factory),
                    onSignOut = { nav.navigate(Routes.Login) { popUpTo(nav.graph.id) { inclusive = true } } },
                    onNavigate = onTab,
                )
            }
            composable(Routes.Steps) {
                ChatScreen(
                    viewModel<ChatViewModel>(factory = factory),
                    onBack = {
                        if (!nav.popBackStack(Routes.Home, inclusive = false)) {
                            nav.navigate(Routes.Home) { popUpTo(nav.graph.id) { inclusive = true } }
                        }
                    },
                    onProtocolComplete = {
                        nav.navigate(Routes.Home) { popUpTo(nav.graph.id) { inclusive = true } }
                    },
                    onOpenAssistant = { nav.navigate(Routes.assistantChat(0)) { launchSingleTop = true } },
                    onAsk = { question -> nav.navigate(Routes.assistantChat(0, question)) },
                )
            }
            composable(Routes.AssistantList) {
                AssistantListScreen(
                    viewModel<AssistantListViewModel>(factory = factory),
                    onBack = { nav.popBackStack() },
                    onNewChat = { nav.navigate(Routes.assistantChat(0)) },
                    onOpen = { id -> nav.navigate(Routes.assistantChat(id)) },
                )
            }
            composable(
                Routes.AssistantChat,
                arguments = listOf(
                    navArgument("conversationId") { type = NavType.LongType },
                    navArgument("q") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) {
                AssistantChatScreen(viewModel<AssistantChatViewModel>(factory = factory), onBack = { nav.popBackStack() })
            }
            composable(Routes.Alert) {
                EmergencyAlertScreen(emergencyVm, onAcknowledge = ::openSteps)
            }
        }

        // In-app critical banner (design 06) over whatever screen is showing.
        val a = active
        if (a != null && a.presentation == AlertPresentation.BANNER) {
            InAppAlertOverlay(a, onOpenSteps = ::openSteps, onSilence = emergencyVm::silence)
        }
    }
}

private fun NavController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(Routes.Home) { inclusive = route == Routes.Home && false }
        launchSingleTop = true
    }
}
