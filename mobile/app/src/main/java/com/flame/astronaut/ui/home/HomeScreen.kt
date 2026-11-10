package com.flame.astronaut.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.flame.astronaut.domain.EmergencyRepository
import com.flame.astronaut.domain.RiskLevel
import com.flame.astronaut.BuildConfig
import com.flame.astronaut.domain.AuthRepository
import com.flame.astronaut.domain.UserProfile
import com.flame.astronaut.domain.RiskRepository
import com.flame.astronaut.domain.RiskState
import com.flame.astronaut.ui.components.*
import com.flame.astronaut.ui.fmtUS
import com.flame.astronaut.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class HomeViewModel(
    riskRepository: RiskRepository,
    private val emergencyRepository: EmergencyRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    val risk: StateFlow<RiskState?> = riskRepository.observeRisk()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val user: StateFlow<UserProfile?> = auth.user

    val scheduledAt: StateFlow<Long?> = emergencyRepository.scheduledAt
    fun schedule(delayMs: Long) = emergencyRepository.scheduleAlert(delayMs)
    fun cancelSchedule() = emergencyRepository.cancelScheduled()

    /** Result of the last demo trigger (backend message or a friendly error). */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()
    fun dismissNotice() { _notice.value = null }

    init {
        viewModelScope.launch { auth.refreshProfile() } // keep name/module current (silent on failure)
        viewModelScope.launch { emergencyRepository.notices.collect { _notice.value = it } }
    }

    fun simulateEmergency() {
        viewModelScope.launch {
            _notice.value = null
            emergencyRepository.simulate()
                .onSuccess { _notice.value = it }
                .onFailure { _notice.value = it.message ?: "Couldn't trigger the simulation." }
        }
    }
}

fun levelColor(l: RiskLevel) = when (l) { RiskLevel.LOW -> C.Green; RiskLevel.MED -> C.Amber; RiskLevel.HIGH -> C.Red }
fun levelLabel(l: RiskLevel) = when (l) { RiskLevel.LOW -> "LOW RISK"; RiskLevel.MED -> "MEDIUM RISK"; RiskLevel.HIGH -> "HIGH RISK" }

@Composable
fun HomeScreen(vm: HomeViewModel, onOpenChat: () -> Unit, onNavigate: (NavTab) -> Unit) {
    val risk by vm.risk.collectAsStateWithLifecycle()
    val user by vm.user.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    val demo = BuildConfig.DEBUG || BuildConfig.USE_FAKE // demo triggers are hidden in release builds
    val name = user?.name?.trim().orEmpty()
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "—" }

    Box(Modifier.fillMaxSize().background(C.Bg)) {
        Starfield(Modifier.fillMaxSize(), alpha = .55f, slow = true)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // header
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).drawBehind {
                        drawCircle(Color(0x2296B4DC), style = Stroke(2.dp.toPx()))
                        drawArc(C.Orange, -90f, 240f, false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    }.padding(3.dp).clip(CircleShape).background(C.Surface3),
                    contentAlignment = Alignment.Center,
                ) { Text(initials, style = chakra(15f, C.Text, FontWeight.Bold)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("CREW MEMBER", style = mono(11f, C.Muted, .16f))
                    Text(if (name.isNotEmpty()) "Good shift, ${name.substringBefore(' ')}" else "Good shift", style = chakra(20f, C.Text, FontWeight.SemiBold), maxLines = 1)
                }
                Box(Modifier.size(44.dp).clip(CircleShape).background(C.Surface).border(1.dp, C.StrokeStrong, CircleShape), contentAlignment = Alignment.Center) {
                    FIcon(Ic.Bell, size = 22.dp, tint = C.Text)
                }
            }
            // chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (BuildConfig.USE_FAKE) {
                    Row(
                        Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF0A2018)).padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BlinkDot(C.Green, 7.dp, glow = false)
                        Spacer(Modifier.width(8.dp))
                        Text("DEMO MODE", style = mono(11.5f, C.Green, .1f))
                    }
                }
                user?.module?.takeIf { it.isNotBlank() }?.let { Chip("${it.uppercase()} MODULE") }
            }

            // risk card — only with real telemetry; the backend has no telemetry endpoint yet
            val r = risk
            Column(
                Modifier.fillMaxWidth().panel(Color(0xFF0A1018), 24.dp, C.Stroke).padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Row {
                    Text("FIRE RISK STATUS", style = mono(11.5f, C.Muted, .2f))
                    Spacer(Modifier.weight(1f))
                    if (r != null) Text("updated ${r.updatedSecondsAgo}s ago", style = mono(11.5f, C.Faint, .02f))
                }
                if (r == null) {
                    Text(
                        "Live telemetry isn't connected yet. Fire alerts and the emergency steps work normally.",
                        style = sans(14.5f, C.Muted, lh = 1.4f), modifier = Modifier.padding(vertical = 22.dp),
                    )
                } else {
                    RiskGauge(r, Modifier.fillMaxWidth().padding(top = 8.dp))
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Metric("O₂", "%.1f".fmtUS(r.o2), "%", Modifier.weight(1f))
                        Metric("PRESS", "%.1f".fmtUS(r.pressure), "k", Modifier.weight(1f))
                        Metric("FLOW", "%.2f".fmtUS(r.airflow), "m/s", Modifier.weight(1f))
                        Metric("WATCH", r.watchMaterial, "", Modifier.weight(1.05f), watch = true)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Sparkline(r.trend, Modifier.width(75.dp).height(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(r.trendText.ifBlank { "Monitoring." }, style = sans(14f, C.Text2, lh = 1.3f))
                    }
                }
            }

            // Talk to AI
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1A0F09), Color(0xFF100B09))))
                    .border(1.dp, Color(0x66FF7A2F), RoundedCornerShape(22.dp))
                    .clickable(onClick = onOpenChat)
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PulseMic(56.dp, ringMillis = 2200, iconSize = 24.dp)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text("Talk to AI", style = chakra(21f, C.Text, FontWeight.SemiBold))
                    Text("Ask by voice or text · hands-free ready", style = sans(14.5f, C.Text2, lh = 1.3f))
                }
                FIcon(Ic.ChevronRight, size = 22.dp, tint = C.Orange)
            }

            // Simulate emergency (debug / demo builds only): POST /simulate → the backend pushes a fire alert to this module
            if (demo) Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFF16090B)).border(1.dp, Color(0x66FF4538), RoundedCornerShape(22.dp))
                    .clickable(onClick = vm::simulateEmergency),
            ) {
                HazardStripe(height = 7.dp, gap = Color(0xFF16090B))
                Row(Modifier.padding(horizontal = 18.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF3A1210)), contentAlignment = Alignment.Center) {
                        FIcon(Ic.FlameOutline, size = 24.dp, tint = C.RedText2)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text("SIMULATE\nEMERGENCY", style = chakra(18f, C.RedText2, FontWeight.SemiBold, .12f).copy(lineHeight = 20.sp), modifier = Modifier.weight(1f))
                            Box(Modifier.border(1.dp, Color(0x88FF7B6E), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text("DEMO", style = mono(10.5f, C.RedText2, .14f))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Asks the backend to push a fire alert to your module · alarm, vibration & notification", style = sans(14.5f, Color(0xFFD7A9A3), lh = 1.3f))
                    }
                    Spacer(Modifier.width(10.dp))
                    FIcon(Ic.Play, size = 18.dp, tint = C.Red)
                }
            }

            notice?.let { msg ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF111826)).border(1.dp, C.StrokeStrong, RoundedCornerShape(14.dp)).padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(msg, style = sans(14f, C.Text2, lh = 1.35f), modifier = Modifier.weight(1f))
                    Box(Modifier.size(36.dp).clip(CircleShape).clickable(onClick = vm::dismissNotice), contentAlignment = Alignment.Center) {
                        Text("✕", style = sans(14f, C.Muted))
                    }
                }
            }

            if (demo) BackgroundAlertCard(vm)
        }
        FlameBottomNav(
            NavTab.Home, onNavigate,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun Chip(text: String) {
    Box(Modifier.clip(RoundedCornerShape(14.dp)).background(C.Surface3).padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text(text, style = mono(11.5f, C.Text2, .1f))
    }
}

@Composable
private fun Metric(label: String, value: String, unit: String, modifier: Modifier, watch: Boolean = false) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(if (watch) Color(0xFF16120C) else C.Surface3).padding(horizontal = 9.dp, vertical = 10.dp),
    ) {
        Text(label, style = mono(10f, if (watch) C.Amber else C.Muted, .12f), maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(
            buildAnnotatedString {
                append(value)
                if (unit.isNotEmpty()) withStyle(SpanStyle(fontSize = 10.sp, color = C.Muted)) { append(unit) }
            },
            style = chakra(if (watch) 15f else 16f, C.Text, FontWeight.SemiBold), maxLines = 1, softWrap = false,
        )
    }
}

@Composable
private fun Sparkline(values: List<Float>, modifier: Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1)
            val y = size.height * (1f - v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, C.Green, style = Stroke(1.5f * density, cap = StrokeCap.Round))
    }
}

/** 200° arc gauge, value 0-100, colour by level. */
@Composable
fun RiskGauge(risk: RiskState?, modifier: Modifier = Modifier) {
    val value = risk?.index ?: 0
    val color = levelColor(risk?.level ?: RiskLevel.LOW)
    Box(modifier.height(190.dp), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.size(width = 240.dp, height = 160.dp)) {
            val stroke = 14.dp.toPx()
            val d = 214.dp.toPx()
            val tl = Offset((size.width - d) / 2, 8.dp.toPx())
            drawArc(Color(0xFF151C2A), 170f, 200f, false, tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
            val sweep = 200f * value / 100f
            drawArc(color, 170f, sweep, false, tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
            // knob
            val ang = Math.toRadians((170f + sweep).toDouble())
            val r = d / 2
            val c = Offset(tl.x + r + (cos(ang) * r).toFloat(), tl.y + r + (sin(ang) * r).toFloat())
            drawCircle(Color(0xFF04060B), stroke / 2 - 2.dp.toPx(), c)
            drawCircle(color, stroke / 2 - 4.dp.toPx(), c)
            // ticks at 0/50/100
            listOf(170f, 270f, 370f).forEach { a ->
                val rad = Math.toRadians(a.toDouble())
                val inner = r + stroke / 2 + 6.dp.toPx()
                val outer = inner + 8.dp.toPx()
                val o = Offset(tl.x + r, tl.y + r)
                drawLine(Color(0x5596B4DC), o + Offset((cos(rad) * inner).toFloat(), (sin(rad) * inner).toFloat()), o + Offset((cos(rad) * outer).toFloat(), (sin(rad) * outer).toFloat()), 1.5.dp.toPx())
            }
        }
        Column(Modifier.padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", style = chakra(58f, C.Text, FontWeight.Bold))
            Spacer(Modifier.height(2.dp))
            Box(Modifier.clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = .14f)).padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(levelLabel(risk?.level ?: RiskLevel.LOW), style = mono(13.5f, color, .2f, FontWeight.Medium))
            }
        }
        Text("0", style = mono(12f, C.Muted), modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 10.dp))
        Text("100", style = mono(12f, C.Muted), modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 10.dp))
    }
}


/** Demo helper: schedule a lock-screen style alert to fire while the presenter has left the app. */
@Composable
private fun BackgroundAlertCard(vm: HomeViewModel) {
    val scheduledAt by vm.scheduledAt.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val options = listOf("10 s" to 10_000L, "30 s" to 30_000L, "1 min" to 60_000L, "2 min" to 120_000L, "5 min" to 300_000L)
    var selected by remember { mutableIntStateOf(1) }

    val remaining by produceState(0L, scheduledAt) {
        while (true) {
            value = ((scheduledAt ?: 0L) - System.currentTimeMillis()).coerceAtLeast(0L)
            kotlinx.coroutines.delay(250)
        }
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFF0A1018)).border(1.dp, C.StrokeStrong, RoundedCornerShape(22.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("BACKGROUND ALERT", style = mono(11.5f, C.Muted, .2f), modifier = Modifier.weight(1f))
            Box(Modifier.border(1.dp, Color(0x6694B4DC), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("DEMO", style = mono(10.5f, C.Muted, .14f)) }
        }
        if (scheduledAt == null) {
            Text("Send the lock-screen alert after a delay — leave the app or lock the phone.", style = sans(14.5f, C.Text2, lh = 1.3f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEachIndexed { i, (label, _) ->
                    val sel = i == selected
                    Box(
                        Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (sel) Color(0xFF26120A) else Color.Transparent)
                            .border(1.dp, if (sel) C.Orange else C.StrokeStrong, RoundedCornerShape(10.dp))
                            .clickable { selected = i },
                        contentAlignment = Alignment.Center,
                    ) { Text(label, style = mono(12f, if (sel) C.OrangeLight else C.Text2, .02f), maxLines = 1) }
                }
            }
            Row(
                Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(C.Orange).clickable {
                    ensureFullScreenPermission(context)
                    vm.schedule(options[selected].second)
                },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                FIcon(Ic.Bell, size = 18.dp, tint = C.OnOrange); Spacer(Modifier.width(8.dp))
                Text("SCHEDULE ALERT", style = chakra(14f, C.OnOrange, FontWeight.Bold, .14f))
            }
        } else {
            val s = remaining / 1000
            Text("Alert in %d:%02d".fmtUS(s / 60, s % 60), style = chakra(28f, C.Text, FontWeight.Bold))
            Text("Now leave the app or lock the phone. The alert will arrive as a critical notification.", style = sans(14.5f, C.Text2, lh = 1.3f))
            Box(
                Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0x73FF8A7F), RoundedCornerShape(12.dp)).clickable(onClick = vm::cancelSchedule),
                contentAlignment = Alignment.Center,
            ) { Text("CANCEL", style = chakra(13f, Color(0xFFFFC9C2), FontWeight.SemiBold, .14f)) }
        }
    }
}

/** Android 14+ requires the user to allow full-screen intents for non-call apps. */
private fun ensureFullScreenPermission(context: android.content.Context) {
    if (android.os.Build.VERSION.SDK_INT >= 34) {
        val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (!nm.canUseFullScreenIntent()) {
            context.startActivity(
                android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, android.net.Uri.parse("package:${context.packageName}"))
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}