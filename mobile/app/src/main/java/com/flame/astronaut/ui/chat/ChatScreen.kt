package com.flame.astronaut.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.flame.astronaut.domain.*
import com.flame.astronaut.ui.components.*
import com.flame.astronaut.ui.fmtUS
import com.flame.astronaut.ui.home.levelColor
import com.flame.astronaut.ui.home.levelLabel
import com.flame.astronaut.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ------------------------------------------------------------------ Emergency steps screen
// The step list comes from GET /guide (cached, with a bundled offline fallback). Questions are answered by the
// AI Assistant screens, not here.

data class ChatUiState(
    val alert: FireAlert? = null,
    val steps: List<Step> = emptyList(),
    val loading: Boolean = true,
    val stepIndex: Int = 0,
    val elapsedSeconds: Int = 0,
    val input: String = "",
) {
    val allDone get() = steps.isNotEmpty() && stepIndex >= steps.size
}

class ChatViewModel(
    private val guide: GuideRepository,
    private val emergencyRepository: EmergencyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        val alert = emergencyRepository.currentAlert.value
        _state.update { it.copy(alert = alert) }
        viewModelScope.launch {
            val steps = guide.getGuide().fireSteps
            _state.update { it.copy(steps = steps, loading = false) }
        }
        viewModelScope.launch {
            val start = alert?.detectedAtMillis ?: System.currentTimeMillis()
            while (true) { // elapsed timer, counts up from detection time
                _state.update { it.copy(elapsedSeconds = ((System.currentTimeMillis() - start) / 1000).toInt().coerceAtLeast(0)) }
                delay(1000)
            }
        }
    }

    fun onInput(v: String) = _state.update { it.copy(input = v) }
    fun clearInput() = _state.update { it.copy(input = "") }

    fun markStepDone() = _state.update { if (it.stepIndex < it.steps.size) it.copy(stepIndex = it.stepIndex + 1) else it }

    fun finishProtocol(onDone: () -> Unit) {
        viewModelScope.launch { emergencyRepository.resolve(); onDone() }
    }
}

@Composable
fun ChatScreen(
    vm: ChatViewModel,
    onBack: () -> Unit,
    onProtocolComplete: () -> Unit,
    onOpenAssistant: () -> Unit,
    onAsk: (String) -> Unit,
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val glow = rememberInfiniteTransition(label = "glow").animateFloat(.35f, 1f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g")
    val scroll = rememberScrollState()
    val mm = (s.elapsedSeconds / 60).toString().padStart(2, '0')
    val ss = (s.elapsedSeconds % 60).toString().padStart(2, '0')
    val a = s.alert

    Box(Modifier.fillMaxSize().background(C.BgEmergency)) {
        Box(Modifier.fillMaxWidth().height(260.dp).alpha(glow.value).background(Brush.radialGradient(listOf(Color(0x48FF2D1E), Color.Transparent), Offset(540f, 160f), 700f)))
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().navigationBarsPadding()) {
            // emergency banner
            Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp).fillMaxWidth().panel(Color(0xE6280808), 20.dp, Color(0x99FF4538))) {
                HazardStripe(height = 5.dp)
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0x0FFFFFFF)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                        FIcon(Ic.Back, size = 18.dp, tint = Color(0xFFF2D9D6))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BlinkDot(C.Red, 8.dp, 800)
                            Spacer(Modifier.width(8.dp))
                            Text("FIRE EMERGENCY", style = chakra(16f, C.RedText2, FontWeight.Bold, .12f))
                        }
                        val where = buildList {
                            a?.module?.let { add(it.uppercase()) }
                            a?.rack?.let { add(rackLabel(it)) }
                        }.joinToString(" · ")
                        if (where.isNotEmpty()) Text(where, style = mono(11f, Color(0xFFD8B4AF), .06f), maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$mm:$ss", style = mono(24f, Color.White, 0f, FontWeight.Medium))
                        Text("ELAPSED", style = mono(9f, Color(0xFFD8B4AF), .16f))
                    }
                }
            }

            Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val received = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(Date(a?.detectedAtMillis ?: System.currentTimeMillis()))
                Text("ALERT RECEIVED · $received", style = mono(10f, C.Muted, .16f), modifier = Modifier.align(Alignment.CenterHorizontally))

                // alert card — only what the push actually contained
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).background(Color(0xFF160B0C)).border(1.dp, Color(0x66FF4538), RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AiAvatar(30.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Fire alert", style = chakra(13f, C.Text, FontWeight.SemiBold, .06f))
                        Spacer(Modifier.weight(1f))
                        val lvl = a?.risk ?: RiskLevel.HIGH
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(levelColor(lvl)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                            Text(levelLabel(lvl), style = mono(9.5f, if (lvl == RiskLevel.HIGH) Color.White else C.OnOrange, .14f, FontWeight.Medium))
                        }
                    }
                    Text(
                        "Fire detected" + (a?.module?.let { " — $it" } ?: "") + ".",
                        style = sans(16f, Color(0xFFFFE1DC), FontWeight.SemiBold, 1.4f),
                    )
                    a?.let { alert ->
                        val line = alert.summaryLine()
                        if (line != "Open the emergency steps.") Text(line, style = sans(14f, Color(0xFFE6D5D2), lh = 1.4f))
                    }
                    val chips = buildList {
                        a?.o2Percent?.let { add(Triple("O₂", "%.1f%%".fmtUS(it), false)) }
                        a?.coPpm?.let { add(Triple("CO", "$it ppm" + if (it >= 35) " ↑" else "", it >= 35)) }
                        a?.airflow?.let { add(Triple("FLOW", "%.2f m/s".fmtUS(it), false)) }
                    }
                    if (chips.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            chips.forEach { (label, value, alarm) -> TelemetryChip(label, value, Modifier.weight(1f), if (alarm) C.RedText else C.Text) }
                        }
                    }
                }

                // protocol header
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("EXTINGUISHING PROTOCOL", style = mono(11f, C.OrangeLight, .16f))
                    Text("${minOf(s.stepIndex, s.steps.size)} / ${s.steps.size}", style = mono(12f, C.Text, 0f, FontWeight.Medium))
                }
                if (s.loading) Text("Loading the emergency protocol…", style = sans(14f, C.Muted))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    s.steps.indices.forEach { i ->
                        val fill by animateFloatAsState(if (i < s.stepIndex) 1f else 0f, tween(250), label = "seg")
                        Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x2E96B4DC))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(C.Orange))
                        }
                    }
                }
                s.steps.forEachIndexed { i, step ->
                    Box(Modifier.animateContentSize(tween(250))) {
                        when {
                            i < s.stepIndex -> StepDone(step)
                            i == s.stepIndex -> StepCurrent(i + 1, s.steps.size, step, vm::markStepDone)
                            else -> StepUpcoming(i + 1, step)
                        }
                    }
                }
                if (s.allDone) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x143DDC97)).border(1.dp, Color(0x593DDC97), RoundedCornerShape(16.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Protocol complete", style = chakra(18f, Color(0xFF7FF0BE)))
                        Text("Continue to follow the approved procedures until the atmosphere and equipment are declared safe.", style = sans(14f, Color(0xFFC9D2DE), lh = 1.5f))
                        Box(
                            Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0x733DDC97), RoundedCornerShape(12.dp)).clickable { vm.finishProtocol(onProtocolComplete) },
                            contentAlignment = Alignment.Center,
                        ) { Text("RETURN TO NORMAL MODE", style = chakra(13f, Color(0xFF7FF0BE), FontWeight.SemiBold, .12f)) }
                    }
                }
            }

            // ask dock: questions are answered by the AI Assistant
            Column(
                Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp, top = 6.dp).fillMaxWidth().panel(Color(0xFF0E1219), 24.dp, Color(0x2996B4DC)).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(C.Surface4).padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (s.input.isEmpty()) Text("Ask the AI assistant…", style = sans(14f, C.Faint))
                        BasicTextField(s.input, vm::onInput, textStyle = sans(14f, C.Text), singleLine = true, cursorBrush = SolidColor(C.Orange), modifier = Modifier.fillMaxWidth())
                    }
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).clickable(enabled = s.input.isNotBlank()) { val q = s.input; vm.clearInput(); onAsk(q) },
                        contentAlignment = Alignment.Center,
                    ) { FIcon(Ic.Send, size = 18.dp, tint = if (s.input.isNotBlank()) C.Orange else C.Muted) }
                }
                Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tap to ask by voice", style = chakra(15f, C.Text, FontWeight.Medium))
                        Text("opens the AI assistant", style = sans(12f, C.Muted))
                    }
                    PulseMic(64.dp, ringMillis = 1600, onClick = onOpenAssistant)
                }
            }
        }

        // Floating button: open the AI research assistant (new chat) without leaving the protocol.
        Box(
            // sits just above the ask dock, on the right edge, so it doesn't cover the step's buttons
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 14.dp, bottom = 168.dp)
                .size(46.dp).clip(CircleShape).background(C.Orange).clickable(onClick = onOpenAssistant),
            contentAlignment = Alignment.Center,
        ) { FIcon(Ic.Chat, size = 22.dp, tint = C.OnOrange) }
    }
}

private fun rackLabel(rack: String) = if (rack.startsWith("RACK", ignoreCase = true)) rack.uppercase() else "RACK ${rack.uppercase()}"

// ------------------------------------------------------------------ pieces

@Composable
private fun AiAvatar(size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFFFB27A), C.Orange, Color(0xFFC2410C)), Offset(size.value * 1.2f, size.value * 1.1f), size.value * 3f)),
        contentAlignment = Alignment.Center,
    ) { FIcon(Ic.Flame, size = size / 2, tint = C.OnOrange) }
}

@Composable
private fun TelemetryChip(label: String, value: String, modifier: Modifier, valueColor: Color = C.Text) {
    Column(modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x0AFFFFFF)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = mono(9.5f, Color(0xFFB9A3A0), .1f))
        Text(value, style = chakra(14f, valueColor, FontWeight.SemiBold), maxLines = 1)
    }
}

@Composable
private fun StepDone(step: Step) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x0F3DDC97)).border(1.dp, Color(0x333DDC97), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(C.Green), contentAlignment = Alignment.Center) { FIcon(Ic.Check, size = 14.dp, tint = Color(0xFF04140C)) }
        Spacer(Modifier.width(12.dp))
        Text(step.title, style = sans(14f, C.Text2).copy(textDecoration = TextDecoration.LineThrough), modifier = Modifier.weight(1f))
        Text("DONE", style = mono(10f, C.Green, .1f))
    }
}

@Composable
private fun StepUpcoming(n: Int, step: Step) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF0C0F16)).border(1.dp, Color(0x1A96B4DC), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(26.dp).clip(CircleShape).border(1.dp, Color(0x4D96B4DC), CircleShape), contentAlignment = Alignment.Center) { Text("$n", style = chakra(12f, C.Muted)) }
        Spacer(Modifier.width(12.dp))
        Text(step.title, style = sans(14f, C.Muted))
    }
}

@Composable
private fun StepCurrent(n: Int, total: Int, step: Step, onDone: () -> Unit) {
    // Speaker button reads this step aloud (tap again to stop); speech stops on step change / leaving the screen.
    val context = androidx.compose.ui.platform.LocalContext.current
    var speaking by remember { mutableStateOf(false) }
    val speaker = remember {
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        com.flame.astronaut.voice.StepSpeaker(context) { v -> main.post { speaking = v } }
    }
    DisposableEffect(Unit) { onDispose { speaker.release() } }
    LaunchedEffect(n) { speaker.stop() }
    Column(
        Modifier.fillMaxWidth().drawBehind { drawRoundRect(Color(0x1AFF7A2F), Offset(-4.dp.toPx(), -4.dp.toPx()), androidx.compose.ui.geometry.Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(20.dp.toPx())) }
            .clip(RoundedCornerShape(16.dp)).background(Color(0xFF14100E)).border(1.dp, C.Orange, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(C.Orange), contentAlignment = Alignment.Center) { Text("$n", style = chakra(14f, C.OnOrange, FontWeight.Bold)) }
            Spacer(Modifier.width(10.dp))
            Text("STEP $n OF $total · NOW", style = mono(10.5f, C.OrangeLight, .16f))
        }
        Text(step.title, style = chakra(20f, C.Text, FontWeight.SemiBold))
        Text(step.body, style = sans(14f, Color(0xFFC9D2DE), lh = 1.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp)).background(C.Orange).clickable { speaker.stop(); onDone() },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                FIcon(Ic.Check, size = 18.dp, tint = C.OnOrange); Spacer(Modifier.width(8.dp))
                Text("MARK DONE", style = chakra(14f, C.OnOrange, FontWeight.Bold, .14f))
            }
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (speaking) C.Orange else Color.Transparent)
                    .border(1.dp, Color(0x73FF7A2F), RoundedCornerShape(12.dp))
                    .clickable { speaker.toggle(n, step.title, step.body) },
                contentAlignment = Alignment.Center,
            ) {
                FIcon(Ic.Speaker, size = 20.dp, tint = if (speaking) C.OnOrange else C.OrangeLight)
            }
        }
    }
}
