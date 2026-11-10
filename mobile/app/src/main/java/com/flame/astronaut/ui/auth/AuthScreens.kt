package com.flame.astronaut.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.flame.astronaut.domain.AssignedModule
import com.flame.astronaut.domain.AuthRepository
import com.flame.astronaut.net.userMessage
import com.flame.astronaut.ui.components.*
import com.flame.astronaut.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ ViewModels

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val trustDevice: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmail(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun toggleShow() = _state.update { it.copy(showPassword = !it.showPassword) }
    fun toggleTrust() = _state.update { it.copy(trustDevice = !it.trustDevice) }

    fun login(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.loading) return
        if (s.email.isBlank() || s.password.isEmpty()) {
            _state.update { it.copy(error = "Enter your email and password.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            auth.login(s.email, s.password)
                .onSuccess { _state.update { it.copy(loading = false, password = "") }; onSuccess() }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.userMessage()) } }
        }
    }
}

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val showPassword: Boolean = false,
    val module: AssignedModule = AssignedModule.entries.first(),
    val allowCritical: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
)

class SignUpViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    fun onName(v: String) = _state.update { it.copy(name = v, error = null) }
    fun onEmail(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onConfirm(v: String) = _state.update { it.copy(confirm = v, error = null) }
    fun toggleShow() = _state.update { it.copy(showPassword = !it.showPassword) }
    fun onModule(v: AssignedModule) = _state.update { it.copy(module = v) }
    fun toggleCritical() = _state.update { it.copy(allowCritical = !it.allowCritical) }

    /** On success [onVerify] receives the verification id and the email the code was sent to. */
    fun register(onVerify: (verificationId: String, email: String) -> Unit) {
        val s = _state.value
        if (s.loading) return
        val problem = when {
            s.name.isBlank() || s.email.isBlank() -> "Enter your name and email."
            s.password.length < 8 -> "The passcode must be at least 8 characters."
            s.password != s.confirm -> "The passcodes don't match."
            else -> null
        }
        if (problem != null) { _state.update { it.copy(error = problem) }; return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            auth.register(s.name, s.email, s.password, s.confirm, s.module)
                .onSuccess { id -> _state.update { it.copy(loading = false) }; onVerify(id, s.email.trim()) }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.userMessage()) } }
        }
    }
}

data class OtpUiState(val otp: String = "", val loading: Boolean = false, val error: String? = null)

class VerifyOtpViewModel(private val auth: AuthRepository, handle: SavedStateHandle) : ViewModel() {
    private val verificationId: String = handle.get<String>("verificationId").orEmpty()
    val email: String = handle.get<String>("email").orEmpty()

    private val _state = MutableStateFlow(OtpUiState())
    val state: StateFlow<OtpUiState> = _state.asStateFlow()

    fun onOtp(v: String) = _state.update { it.copy(otp = v.filter(Char::isDigit).take(6), error = null) }

    fun verify(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.loading) return
        if (s.otp.length < 4) { _state.update { it.copy(error = "Enter the code from your email.") }; return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            auth.verifyOtp(verificationId, s.otp)
                .onSuccess { _state.update { it.copy(loading = false) }; onSuccess() }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.userMessage()) } }
        }
    }
}

data class ForgotUiState(
    val step: Int = 1, // 1 = ask for the email, 2 = enter the token from the email + new passcode
    val email: String = "",
    val token: String = "",
    val password: String = "",
    val confirm: String = "",
    val loading: Boolean = false,
    val info: String? = null,
    val error: String? = null,
    val done: Boolean = false,
)

class ForgotPasswordViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(ForgotUiState())
    val state: StateFlow<ForgotUiState> = _state.asStateFlow()

    fun onEmail(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onToken(v: String) = _state.update { it.copy(token = v, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onConfirm(v: String) = _state.update { it.copy(confirm = v, error = null) }
    fun haveToken() = _state.update { it.copy(step = 2, error = null) }

    fun sendLink() {
        val s = _state.value
        if (s.loading) return
        if (s.email.isBlank()) { _state.update { it.copy(error = "Enter your email.") }; return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            auth.forgotPassword(s.email)
                .onSuccess { msg -> _state.update { it.copy(loading = false, info = msg, step = 2) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.userMessage()) } }
        }
    }

    fun reset() {
        val s = _state.value
        if (s.loading) return
        val problem = when {
            s.token.isBlank() -> "Paste the reset code from the email."
            s.password.length < 8 -> "The passcode must be at least 8 characters."
            s.password != s.confirm -> "The passcodes don't match."
            else -> null
        }
        if (problem != null) { _state.update { it.copy(error = problem) }; return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            auth.resetPassword(s.token, s.email, s.password, s.confirm)
                .onSuccess { msg -> _state.update { it.copy(loading = false, info = msg, done = true) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.userMessage()) } }
        }
    }
}

// ------------------------------------------------------------------ shared pieces

@Composable
private fun LangPill() {
    Row(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xCC081226))
            .border(1.dp, C.StrokeStrong, RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) { Text("EN", style = mono(13f, C.Text, .1f)) }
}

@Composable
private fun ErrorText(message: String?, modifier: Modifier = Modifier) {
    if (message != null) Text(message, style = sans(14f, C.RedText, lh = 1.35f), modifier = modifier.fillMaxWidth().padding(top = 12.dp))
}

@Composable
private fun InfoText(message: String?, modifier: Modifier = Modifier) {
    if (message != null) Text(message, style = sans(14f, C.Green, lh = 1.35f), modifier = modifier.fillMaxWidth().padding(top = 12.dp))
}

@Composable
private fun EyeToggle(onClick: () -> Unit) {
    Box(Modifier.clickable(onClick = onClick)) { FIcon(Ic.Eye, size = 24.dp, tint = C.Muted) }
}

@Composable
private fun CheckBox(checked: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (checked) C.Orange else Color.Transparent)
            .border(1.5.dp, if (checked) C.Orange else C.Faint, RoundedCornerShape(4.dp))
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) { if (checked) FIcon(Ic.Check, size = 15.dp, tint = C.OnOrange) }
}

@Composable
private fun Planet(modifier: Modifier) {
    Box(
        modifier.drawBehind {
            // large horizon planet at the top (LEO) — radial blue glow
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF2A6FB8), Color(0xFF123E7A), Color(0xFF08193A), Color(0xFF030814)),
                    center = Offset(size.width / 2, size.height * .92f),
                    radius = size.width * 1.1f,
                ),
                radius = size.width * 1.1f, center = Offset(size.width / 2, size.height * .92f),
            )
        },
    )
}

// ------------------------------------------------------------------ Login

@Composable
fun LoginScreen(vm: LoginViewModel, onLoggedIn: () -> Unit, onSignUp: () -> Unit, onForgot: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val orbit = rememberInfiniteTransition(label = "orbit")
    val rot by orbit.animateFloat(0f, 360f, infiniteRepeatable(tween(120_000, easing = LinearEasing)), label = "rot")
    val lock = rememberInfiniteTransition(label = "lock")

    Box(Modifier.fillMaxSize().background(C.BgSplash)) {
        // planet
        Canvas(Modifier.fillMaxWidth().height(260.dp)) {
            val cx = size.width / 2
            val cy = size.height * 1.05f
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF2A6FB8), Color(0xFF123E7A), Color(0xFF08193A), Color(0xFF030814)),
                    Offset(cx, cy - 60 * density), size.width * .95f,
                ),
                radius = size.width * .95f, center = Offset(cx, cy + 160 * density),
            )
            drawCircle(Color(0x665BA8E8), size.width * .95f, Offset(cx, cy + 160 * density), style = Stroke(1.2f * density))
        }
        Starfield(Modifier.fillMaxSize())
        // dashed orbit ring
        Canvas(Modifier.fillMaxSize().rotate(rot)) {
            drawCircle(Color(0x3396B4DC), size.width * .85f, Offset(size.width / 2, size.height * .38f), style = Stroke(1f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6 * density, 8 * density))))
        }

        Column(
            Modifier.fillMaxSize().statusBarsPadding().imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                FIcon(Ic.FlameOutline, size = 26.dp, tint = C.Orange)
                Spacer(Modifier.width(12.dp))
                Text("PYRONOVA", style = chakra(15f, C.Text, FontWeight.SemiBold, .2f))
                Spacer(Modifier.weight(1f))
                LangPill()
            }
            Spacer(Modifier.height(72.dp))
            Text("LEO · 412 KM · UPLINK SECURE", style = mono(11.5f, Color(0xFF9FB6D6), .24f), modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(100.dp))
            Text("// MISSION ACCESS", style = mono(12f, C.Orange, .22f))
            Spacer(Modifier.height(12.dp))
            Text("Welcome back,\ncrew member.", style = chakra(34f, Color(0xFFE8EEF6), FontWeight.Bold).copy(lineHeight = 37.sp))
            Spacer(Modifier.height(14.dp))
            Text("Authenticate to receive live fire alerts and AI guidance aboard the station.", style = sans(15f, C.Muted, lh = 1.4f))
            Spacer(Modifier.height(24.dp))

            val dot by lock.animateFloat(1f, .3f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "d")
            ChannelInput(
                label = "CH-01 · CREW EMAIL",
                value = s.email, onValueChange = vm::onEmail,
                leading = Ic.Mail,
                placeholder = "name@crew.station",
                rightLabel = "● SIGNAL LOCK", rightLabelColor = C.Green.copy(alpha = dot),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            Spacer(Modifier.height(14.dp))
            ChannelInput(
                label = "CH-02 · PASSCODE", rightLabel = "AES-256",
                value = s.password, onValueChange = vm::onPassword,
                placeholder = "Your passcode",
                leading = Ic.Lock, password = true, showPassword = s.showPassword,
                trailing = { EyeToggle(vm::toggleShow) },
            )
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CheckBox(s.trustDevice, vm::toggleTrust)
                Spacer(Modifier.width(12.dp))
                Text("Trust this device", style = sans(15f, C.Text2))
                Spacer(Modifier.weight(1f))
                Text("Forgot passcode?", style = sans(15f, C.Orange), modifier = Modifier.clickable(onClick = onForgot).padding(vertical = 6.dp))
            }
            ErrorText(s.error)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChamferedButton("ESTABLISH LINK", { vm.login(onLoggedIn) }, Modifier.weight(1f), loading = s.loading)
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF07131D)).border(1.dp, C.Cyan.copy(.45f), RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center,
                ) { FIcon(Ic.Fingerprint, size = 26.dp, tint = C.Cyan) }
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(32.dp))
            Row(Modifier.align(Alignment.CenterHorizontally).clickable(onClick = onSignUp).padding(vertical = 8.dp)) {
                Text("No crew credentials? ", style = sans(15f, C.Muted))
                Text("Request access", style = sans(15f, C.Orange, FontWeight.SemiBold))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ------------------------------------------------------------------ Sign up

@Composable
fun SignUpScreen(vm: SignUpViewModel, onBack: () -> Unit, onVerify: (verificationId: String, email: String) -> Unit, onLogin: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val orbit = rememberInfiniteTransition(label = "orbit")
    val rot by orbit.animateFloat(0f, 360f, infiniteRepeatable(tween(80_000, easing = LinearEasing)), label = "rot")

    Box(Modifier.fillMaxSize().background(C.BgSplash)) {
        Starfield(Modifier.fillMaxSize())
        // moon top-right
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width + 20 * density, 50 * density)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFE6E2D6), Color(0xFFB4AFA2), Color(0xFF6B675E)), c - Offset(60 * density, 30 * density), 220 * density), 120 * density, c)
            drawCircle(Color(0x66595550), 14 * density, Offset(size.width - 100 * density, 75 * density))
            drawCircle(Color(0x55595550), 9 * density, Offset(size.width - 20 * density, 118 * density))
        }
        Canvas(Modifier.fillMaxSize().rotate(rot)) {
            drawCircle(Color(0x2296B4DC), size.width * .75f, Offset(size.width * .75f, 60 * density), style = Stroke(1f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6 * density, 8 * density))))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Box(
                Modifier.padding(top = 8.dp).size(44.dp).clip(CircleShape).background(Color(0xFF07101E)).border(1.dp, C.StrokeStrong, CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { FIcon(Ic.Back, size = 20.dp, tint = C.Text) }
            Spacer(Modifier.height(44.dp))
            Text("// CREW ENROLLMENT", style = mono(12f, C.Orange, .22f))
            Spacer(Modifier.height(10.dp))
            Text("Join the mission", style = chakra(34f, Color(0xFFE8EEF6), FontWeight.Bold))
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("01 PROFILE", style = mono(12f, C.Orange, .16f))
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f).height(2.dp).background(Color(0x3396B4DC))) { Box(Modifier.fillMaxWidth(.5f).fillMaxHeight().background(C.Orange)) }
                Spacer(Modifier.width(12.dp))
                Text("02 VERIFY", style = mono(12f, C.Muted, .16f))
            }
            Spacer(Modifier.height(20.dp))
            ChannelInput("CH-01 · FULL NAME", s.name, vm::onName, placeholder = "Your full name", leading = Ic.User, allCorners = false, height = 52.dp)
            Spacer(Modifier.height(14.dp))
            ChannelInput("CH-02 · CREW EMAIL", s.email, vm::onEmail, placeholder = "name@crew.station", leading = Ic.Mail, allCorners = false, height = 52.dp, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            Spacer(Modifier.height(14.dp))
            ChannelInput(
                "CH-03 · PASSCODE", s.password, vm::onPassword, placeholder = "Min. 8 characters", leading = Ic.Lock,
                password = true, showPassword = s.showPassword, allCorners = false, height = 52.dp,
                trailing = { EyeToggle(vm::toggleShow) },
            )
            Spacer(Modifier.height(14.dp))
            ChannelInput(
                "CH-04 · CONFIRM PASSCODE", s.confirm, vm::onConfirm, placeholder = "Repeat the passcode", leading = Ic.Lock,
                password = true, showPassword = s.showPassword, allCorners = false, height = 52.dp,
            )
            Spacer(Modifier.height(20.dp))
            Text("ASSIGNED MODULE", style = mono(11f, C.Muted, .18f))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssignedModule.entries.forEach { m ->
                    val sel = m == s.module
                    Box(
                        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (sel) Color(0xFF26120A) else Color(0x990A0F18))
                            .border(1.dp, if (sel) C.Orange else C.StrokeStrong, RoundedCornerShape(3.dp))
                            .clickable { vm.onModule(m) },
                        contentAlignment = Alignment.Center,
                    ) { Text(m.name, style = chakra(11.5f, if (sel) C.OrangeLight else C.Text, FontWeight.SemiBold, .1f), maxLines = 1) }
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CheckBox(s.allowCritical, vm::toggleCritical)
                Spacer(Modifier.width(12.dp))
                Text("Allow critical alerts to bypass silent mode", style = sans(15f, C.Text2))
            }
            ErrorText(s.error)
            Spacer(Modifier.height(20.dp))
            ChamferedButton("CREATE CREW PROFILE", { vm.register(onVerify) }, Modifier.fillMaxWidth(), loading = s.loading)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(28.dp))
            Row(Modifier.align(Alignment.CenterHorizontally).clickable(onClick = onLogin).padding(vertical = 8.dp)) {
                Text("Already enrolled? ", style = sans(15f, C.Muted))
                Text("Log in", style = sans(15f, C.Orange, FontWeight.SemiBold))
            }
        }
    }
}

// ------------------------------------------------------------------ Email verification (OTP)

@Composable
fun VerifyOtpScreen(vm: VerifyOtpViewModel, onBack: () -> Unit, onVerified: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(C.BgSplash)) {
        Starfield(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Box(
                Modifier.padding(top = 8.dp).size(44.dp).clip(CircleShape).background(Color(0xFF07101E)).border(1.dp, C.StrokeStrong, CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { FIcon(Ic.Back, size = 20.dp, tint = C.Text) }
            Spacer(Modifier.height(44.dp))
            Text("// CREW ENROLLMENT", style = mono(12f, C.Orange, .22f))
            Spacer(Modifier.height(10.dp))
            Text("Verify your email", style = chakra(34f, Color(0xFFE8EEF6), FontWeight.Bold))
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("01 PROFILE", style = mono(12f, C.Muted, .16f))
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f).height(2.dp).background(C.Orange))
                Spacer(Modifier.width(12.dp))
                Text("02 VERIFY", style = mono(12f, C.Orange, .16f))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                if (vm.email.isNotBlank()) "We sent a one-time code to ${vm.email}. Enter it below to finish creating your profile."
                else "Enter the one-time code we emailed you to finish creating your profile.",
                style = sans(15f, C.Muted, lh = 1.4f),
            )
            Spacer(Modifier.height(22.dp))
            ChannelInput(
                "CH-01 · ONE-TIME CODE", s.otp, vm::onOtp, placeholder = "0000", leading = Ic.Lock, mono = true,
                allCorners = false, height = 52.dp, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
            ErrorText(s.error)
            Spacer(Modifier.height(24.dp))
            ChamferedButton("VERIFY & CONTINUE", { vm.verify(onVerified) }, Modifier.fillMaxWidth(), loading = s.loading)
        }
    }
}

// ------------------------------------------------------------------ Forgot / reset passcode

@Composable
fun ForgotPasswordScreen(vm: ForgotPasswordViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(C.BgSplash)) {
        Starfield(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Box(
                Modifier.padding(top = 8.dp).size(44.dp).clip(CircleShape).background(Color(0xFF07101E)).border(1.dp, C.StrokeStrong, CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { FIcon(Ic.Back, size = 20.dp, tint = C.Text) }
            Spacer(Modifier.height(44.dp))
            Text("// ACCESS RECOVERY", style = mono(12f, C.Orange, .22f))
            Spacer(Modifier.height(10.dp))
            Text(if (s.done) "Passcode updated" else "Reset your passcode", style = chakra(32f, Color(0xFFE8EEF6), FontWeight.Bold))
            Spacer(Modifier.height(14.dp))
            Text(
                when {
                    s.done -> "You can now sign in with your new passcode."
                    s.step == 1 -> "Enter your crew email and we'll send you a reset link."
                    else -> "Paste the reset code from the email, then choose a new passcode."
                },
                style = sans(15f, C.Muted, lh = 1.4f),
            )
            Spacer(Modifier.height(22.dp))
            if (!s.done) {
                ChannelInput(
                    "CH-01 · CREW EMAIL", s.email, vm::onEmail, placeholder = "name@crew.station", leading = Ic.Mail, allCorners = false, height = 52.dp,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                if (s.step == 2) {
                    Spacer(Modifier.height(14.dp))
                    ChannelInput("CH-02 · RESET CODE", s.token, vm::onToken, placeholder = "Code from the email", leading = Ic.Lock, allCorners = false, height = 52.dp)
                    Spacer(Modifier.height(14.dp))
                    ChannelInput("CH-03 · NEW PASSCODE", s.password, vm::onPassword, placeholder = "Min. 8 characters", leading = Ic.Lock, password = true, allCorners = false, height = 52.dp)
                    Spacer(Modifier.height(14.dp))
                    ChannelInput("CH-04 · CONFIRM PASSCODE", s.confirm, vm::onConfirm, placeholder = "Repeat the passcode", leading = Ic.Lock, password = true, allCorners = false, height = 52.dp)
                }
            }
            InfoText(s.info)
            ErrorText(s.error)
            Spacer(Modifier.height(24.dp))
            when {
                s.done -> ChamferedButton("BACK TO LOGIN", onBack, Modifier.fillMaxWidth())
                s.step == 1 -> {
                    ChamferedButton("SEND RESET LINK", vm::sendLink, Modifier.fillMaxWidth(), loading = s.loading)
                    Text(
                        "I already have a code", style = sans(15f, C.Orange, FontWeight.SemiBold),
                        modifier = Modifier.align(Alignment.CenterHorizontally).clickable(onClick = vm::haveToken).padding(vertical = 16.dp),
                    )
                }
                else -> ChamferedButton("RESET PASSCODE", vm::reset, Modifier.fillMaxWidth(), loading = s.loading)
            }
        }
    }
}
