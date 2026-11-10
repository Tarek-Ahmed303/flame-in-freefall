package com.flame.astronaut.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.flame.astronaut.BuildConfig
import com.flame.astronaut.domain.AssignedModule
import com.flame.astronaut.domain.AuthRepository
import com.flame.astronaut.domain.UserProfile
import com.flame.astronaut.net.userMessage
import com.flame.astronaut.ui.components.*
import com.flame.astronaut.ui.fmtUS
import com.flame.astronaut.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val readAloud: Boolean = true,
    val wakePhrase: Boolean = true,
    val criticalAlerts: Boolean = true,
    val speechRate: Float = com.flame.astronaut.AppPrefs.speechRate,
    val alarmVolume: Float = com.flame.astronaut.AppPrefs.alarmVolume,
)

class SettingsViewModel(
    private val auth: AuthRepository,
    private val pushTokens: com.flame.astronaut.push.PushTokens,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    val pushToken: StateFlow<String?> = pushTokens.token
    val pushAvailable: Boolean get() = pushTokens.available
    fun refreshToken() = pushTokens.refresh()

    val user: StateFlow<UserProfile?> = auth.user

    private val _moduleMessage = MutableStateFlow<String?>(null)
    val moduleMessage: StateFlow<String?> = _moduleMessage.asStateFlow()

    init { viewModelScope.launch { auth.refreshProfile() } }

    /** PUT /reset-module, then re-read the profile so the screen shows what the server has. */
    fun changeModule(module: AssignedModule) {
        viewModelScope.launch {
            _moduleMessage.value = null
            auth.resetModule(module)
                .onSuccess { auth.refreshProfile(); _moduleMessage.value = "Module updated to ${module.label}." }
                .onFailure { _moduleMessage.value = it.userMessage() }
        }
    }

    fun readAloud(v: Boolean) = _state.update { it.copy(readAloud = v) }
    fun wakePhrase(v: Boolean) = _state.update { it.copy(wakePhrase = v) }
    fun critical(v: Boolean) = _state.update { it.copy(criticalAlerts = v) }
    fun speechRate(v: Float) { com.flame.astronaut.AppPrefs.speechRate = v; _state.update { it.copy(speechRate = v) } }
    fun volume(v: Float) { com.flame.astronaut.AppPrefs.alarmVolume = v; _state.update { it.copy(alarmVolume = v) } }

    fun signOut(onDone: () -> Unit) { viewModelScope.launch { auth.logout(); onDone() } }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel, onSignOut: () -> Unit, onNavigate: (NavTab) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val token by vm.pushToken.collectAsStateWithLifecycle()
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val user by vm.user.collectAsStateWithLifecycle()
    val moduleMessage by vm.moduleMessage.collectAsStateWithLifecycle()
    var pickModule by remember { mutableStateOf(false) }
    val name = user?.name?.trim().orEmpty()
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "—" }
    Box(Modifier.fillMaxSize().background(C.Bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 110.dp)) {
            Row(Modifier.padding(top = 24.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", style = chakra(32f, C.Text, FontWeight.Bold), modifier = Modifier.weight(1f))
                Row(Modifier.clickable { vm.signOut(onSignOut) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FIcon(Ic.SignOut, size = 20.dp, tint = C.RedText); Spacer(Modifier.width(8.dp))
                    Text("Sign out", style = sans(17f, C.RedText, FontWeight.Medium))
                }
            }
            // profile
            Column(Modifier.fillMaxWidth().panel(Color(0xFF0A1018), 24.dp, C.Stroke).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(56.dp).drawBehind {
                            drawCircle(Color(0x2296B4DC), style = Stroke(2.dp.toPx()))
                            drawArc(C.Orange, -90f, 260f, false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                        }.padding(4.dp).clip(CircleShape).background(C.Surface3),
                        contentAlignment = Alignment.Center,
                    ) { Text(initials, style = chakra(20f, C.Text, FontWeight.Bold)) }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name.ifEmpty { "Crew member" }, style = chakra(20f, C.Text, FontWeight.SemiBold), maxLines = 1)
                        Text(user?.email.orEmpty(), style = mono(12f, C.Muted, .04f), maxLines = 1)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Surface3).clickable { pickModule = true }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ASSIGNED MODULE", style = mono(11f, C.Muted, .16f))
                        Spacer(Modifier.height(4.dp))
                        Text(AssignedModule.fromApi(user?.module)?.label ?: user?.module.orEmpty().ifEmpty { "—" }, style = chakra(17f, C.Text, FontWeight.SemiBold), maxLines = 1)
                    }
                    Text("CHANGE", style = mono(11.5f, C.OrangeLight, .14f))
                }
                moduleMessage?.let { Text(it, style = sans(13.5f, C.Text2, lh = 1.3f), modifier = Modifier.padding(top = 10.dp)) }
            }

            SectionLabel("VOICE")
            Group {
                ToggleRow("Read replies aloud", "AI speaks every step during alerts", s.readAloud, vm::readAloud)
                Divider()
                ToggleRow("Wake phrase “Hey Flame”", "Hands-free while your hands are busy", s.wakePhrase, vm::wakePhrase)
                Divider()
                ValueRow("Assistant voice", "Atlas · calm")
                Divider()
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row {
                        Text("Speech rate", style = sans(18f, C.Text, FontWeight.Medium), modifier = Modifier.weight(1f))
                        Text("%.1f×".fmtUS(s.speechRate), style = mono(14f, C.Orange, .02f))
                    }
                    OrangeSlider(s.speechRate, vm::speechRate, 0.5f..2f)
                }
            }

            SectionLabel("ALERTS")
            Group {
                ToggleRow("Critical alerts", "Override silent & focus modes", s.criticalAlerts, vm::critical)
                Divider()
                ValueRow("Vibration pattern", "SOS pulse")
                Divider()
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row {
                        Text("Alarm volume", style = sans(18f, C.Text, FontWeight.Medium), modifier = Modifier.weight(1f))
                        Text("${(s.alarmVolume * 100).toInt()}%", style = mono(14f, C.Orange, .02f))
                    }
                    OrangeSlider(s.alarmVolume, vm::volume, 0f..1f)
                }
            }

            if (BuildConfig.DEBUG) SectionLabel("DEVICE · PUSH TOKEN (DEBUG)")
            if (BuildConfig.DEBUG) Group {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("FCM token", style = sans(18f, C.Text, FontWeight.Medium))
                    Text(
                        when {
                            token != null -> token!!
                            vm.pushAvailable -> "Waiting for Firebase…"
                            else -> "Push not configured — add app/google-services.json and rebuild."
                        },
                        style = mono(11.5f, if (token != null) C.Text2 else C.Faint, .02f, FontWeight.Normal),
                        maxLines = 4,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).background(if (token != null) C.Orange else Color(0xFF2A3342))
                                .clickable(enabled = token != null) { clipboard.setText(androidx.compose.ui.text.AnnotatedString(token!!)); copied = true }
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(if (copied) "COPIED" else "COPY TOKEN", style = chakra(13f, if (token != null) C.OnOrange else C.Muted, FontWeight.Bold, .12f)) }
                        Box(
                            Modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, C.StrokeStrong, RoundedCornerShape(12.dp)).clickable { copied = false; vm.refreshToken() }.padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("REFRESH", style = chakra(13f, C.Text2, FontWeight.SemiBold, .12f)) }
                    }
                }
            }
        }
        FlameBottomNav(NavTab.Settings, onNavigate, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 12.dp))
    }

    if (pickModule) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickModule = false },
            title = { Text("Assigned module") },
            text = {
                Column {
                    AssignedModule.entries.forEach { m ->
                        val selected = m.name.equals(user?.module, ignoreCase = true)
                        Text(
                            (if (selected) "● " else "○ ") + m.label,
                            style = sans(17f, if (selected) C.Orange else Color(0xFF222222), FontWeight.Medium),
                            modifier = Modifier.fillMaxWidth().clickable { pickModule = false; if (!selected) vm.changeModule(m) }.padding(vertical = 12.dp),
                        )
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton({ pickModule = false }) { Text("Cancel") } },
        )
    }
}

@Composable private fun SectionLabel(t: String) = Text(t, style = mono(12f, C.Muted, .22f), modifier = Modifier.padding(start = 8.dp, top = 22.dp, bottom = 10.dp))

@Composable private fun Group(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxWidth().panel(Color(0xFF0A1018), 22.dp, C.Stroke), content = content)

@Composable private fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(C.Stroke))

@Composable
private fun Info(label: String, value: String, modifier: Modifier, mono: Boolean = false) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(C.Surface3).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(label, style = mono(11f, C.Muted, .16f))
        Spacer(Modifier.height(4.dp))
        Text(value, style = if (mono) chakra(17f, C.Text, FontWeight.Bold) else chakra(17f, C.Text, FontWeight.SemiBold), maxLines = 1)
    }
}

@Composable
private fun ToggleRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = sans(18f, C.Text, FontWeight.Medium))
            Text(sub, style = sans(15f, C.Muted))
        }
        FlameSwitch(checked)
    }
}

@Composable
private fun FlameSwitch(checked: Boolean) {
    val bg by animateColorAsState(if (checked) C.Orange else Color(0xFF2A3342), label = "bg")
    val x by animateDpAsState(if (checked) 40.dp else 4.dp, label = "x")
    Box(Modifier.size(width = 76.dp, height = 44.dp).clip(RoundedCornerShape(22.dp)).background(bg)) {
        Box(Modifier.offset(x = x - 0.dp).padding(top = 4.dp).size(36.dp).clip(CircleShape).background(if (checked) Color(0xFF1A0800) else Color(0xFF8B98AB)))
    }
}

@Composable
private fun ValueRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = sans(18f, C.Text, FontWeight.Medium), modifier = Modifier.weight(1f))
        Text(value, style = sans(16f, C.Muted))
        Spacer(Modifier.width(10.dp))
        FIcon(Ic.ChevronRight, size = 18.dp, tint = C.Muted)
    }
}

@Composable
private fun OrangeSlider(value: Float, onChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>) {
    Slider(
        value = value, onValueChange = onChange, valueRange = range,
        colors = SliderDefaults.colors(thumbColor = C.Orange, activeTrackColor = C.Orange, inactiveTrackColor = Color(0xFF3A404C)),
    )
}
