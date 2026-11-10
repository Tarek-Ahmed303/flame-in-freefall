package com.flame.astronaut.ui.emergency

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flame.astronaut.BuildConfig
import com.flame.astronaut.domain.RiskLevel
import com.flame.astronaut.domain.summaryLine
import com.flame.astronaut.emergency.ActiveAlert
import com.flame.astronaut.emergency.EmergencyCoordinator
import com.flame.astronaut.ui.components.*
import com.flame.astronaut.ui.theme.*
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmergencyViewModel(private val coordinator: EmergencyCoordinator) : ViewModel() {
    val active: StateFlow<ActiveAlert?> = coordinator.active
    fun setSignedIn(value: Boolean) = coordinator.setSignedIn(value)
    fun silence() = coordinator.silence()
    fun acknowledge() = coordinator.acknowledge()
}

// ------------------------------------------------------------------ Full-screen "incoming call" alert (design 07)

@Composable
fun EmergencyAlertScreen(vm: EmergencyViewModel, onAcknowledge: () -> Unit) {
    val active by vm.active.collectAsStateWithLifecycle()
    val alert = active?.alert
    val silenced = active?.silenced == true

    val t = rememberInfiniteTransition(label = "alert")
    val ambient by t.animateFloat(.25f, .85f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "amb")
    val ring1 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearOutSlowInEasing)), label = "r1")
    val ring2 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearOutSlowInEasing), initialStartOffset = StartOffset(700)), label = "r2")
    val shake by t.animateFloat(-2.5f, 2.5f, infiniteRepeatable(tween(200), RepeatMode.Reverse), label = "shake")
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(600, easing = CubicBezierEasing(.2f, .9f, .3f, 1.2f))) }

    val now = remember { Date() }
    Box(Modifier.fillMaxSize().background(Color(0xFF05060C))) {
        Starfield(Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().alpha(ambient).background(Brush.radialGradient(listOf(Color(0x66FF2D1E), Color.Transparent), Offset(540f, 700f), 900f)))
        // planet at the bottom
        Canvas(Modifier.fillMaxSize()) {
            val r = size.width * 1.1f
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF3B83CC), Color(0xFF1A4F9A), Color(0xFF0A2250), Color(0x00030814)), Offset(size.width / 2, size.height * 1.02f), r),
                r, Offset(size.width / 2, size.height + r * .42f),
            )
        }

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            if (BuildConfig.USE_FAKE) Box(Modifier.clip(RoundedCornerShape(24.dp)).background(Color(0x66FFFFFF).copy(.07f)).padding(horizontal = 18.dp, vertical = 8.dp)) {
                Text("DEMO SCENARIO · SIMULATED DATA", style = mono(11f, C.Text2, .16f))
            }
            Spacer(Modifier.height(24.dp))
            Text(SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH).format(now), style = sans(18f, C.Text2))
            Text(SimpleDateFormat("HH:mm", Locale.ENGLISH).format(now), style = sans(104f, Color(0xFFF2F5FA), FontWeight.Normal, 1f))
            Spacer(Modifier.height(32.dp))

            // notification card
            Column(
                Modifier.fillMaxWidth().offset { IntOffset(0, (-30.dp.toPx() * (1 - enter.value.coerceIn(0f, 1.2f))).toInt()) }.alpha(enter.value.coerceIn(0f, 1f))
                    .panel(Color(0xF2200C0E), 22.dp, Color(0x99FF4538)),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(C.Orange), contentAlignment = Alignment.Center) {
                            FIcon(Ic.Flame, size = 18.dp, tint = C.OnOrange)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text("PYRONOVA", style = mono(12.5f, C.Text2, .2f), modifier = Modifier.weight(1f))
                        Text("now", style = sans(13f, C.Muted))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.clip(RoundedCornerShape(4.dp)).background(C.Red).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            FIcon(Ic.Warning, size = 13.dp, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("CRITICAL ALERT", style = mono(11f, Color.White, .14f, FontWeight.Medium))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("bypasses silent mode", style = mono(11.5f, C.Muted, .04f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Fire detected — ${alert?.module ?: "your module"}", style = chakra(20f, Color.White, FontWeight.Bold).copy(fontFamily = PlexSans))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildAnnotatedString {
                            val line = alert?.summaryLine()
                            if (line != null && line != "Open the emergency steps.") append("$line · ")
                            withStyle(SpanStyle(color = C.RedText, fontWeight = FontWeight.SemiBold)) { append("${(alert?.risk ?: RiskLevel.HIGH).name} RISK") }
                            append(". Tap to open the emergency steps.")
                        },
                        style = sans(15f, Color(0xFFE6D5D2), lh = 1.4f),
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x33FF4538)))
                Row(Modifier.height(54.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight().clickable { vm.acknowledge(); onAcknowledge() }, contentAlignment = Alignment.Center) {
                        Text("Acknowledge", style = sans(17f, C.RedText, FontWeight.Medium))
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(Color(0x33FF4538)))
                    Box(Modifier.weight(1f).fillMaxHeight().clickable(enabled = !silenced) { vm.silence() }, contentAlignment = Alignment.Center) {
                        Text(if (silenced) "Silenced" else "Silence alarm", style = sans(17f, if (silenced) C.Muted else C.Text, FontWeight.Medium))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            // vibrating icon + ripples
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    listOf(ring1, ring2).forEach { p ->
                        drawCircle(C.RedText.copy(alpha = .45f * (1f - p)), 36.dp.toPx() * (.7f + p), style = Stroke(1.5f * density))
                    }
                }
                Box(
                    Modifier.size(72.dp).offset(x = if (silenced) 0.dp else shake.dp).clip(CircleShape).background(Color(0x66FF7B6E).copy(.35f)).border(2.dp, C.RedText.copy(.8f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { FIcon(Ic.Vibrate, size = 32.dp, tint = C.RedText) }
            }
            Text(if (silenced) "ALARM SILENCED" else "ALARM TONE · SOS VIBRATION", style = mono(12f, Color(0xFFCFE0F6), .2f))
            Spacer(Modifier.height(36.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { vm.acknowledge(); onAcknowledge() }.padding(8.dp)) {
                FIcon(Ic.ChevronUp, size = 18.dp, tint = Color(0xFFCFE0F6))
                Spacer(Modifier.width(8.dp))
                Text("Swipe up to open", style = sans(16f, Color(0xFFCFE0F6)))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ------------------------------------------------------------------ In-app critical banner (design 06)

@Composable
fun InAppAlertOverlay(active: ActiveAlert, onOpenSteps: () -> Unit, onSilence: () -> Unit, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "glow")
    val glow by t.animateFloat(.4f, 1f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g")
    val shake by t.animateFloat(-2.5f, 2.5f, infiniteRepeatable(tween(250), RepeatMode.Reverse), label = "s")
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(500, easing = CubicBezierEasing(.2f, .9f, .3f, 1.2f))) }

    Box(modifier.fillMaxSize()) {
        // dim + red edge glow
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(Color(0x99000000))
                drawRect(
                    Brush.radialGradient(
                        listOf(Color.Transparent, Color(0x00FF2D1E), Color(0xAAFF2D1E).copy(alpha = .75f * glow)),
                        center, size.maxDimension * .62f,
                    ),
                )
            },
        )
        Column(
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = 10.dp).padding(top = 6.dp)
                .offset { IntOffset(0, (-30.dp.toPx() * (1 - enter.value.coerceIn(0f, 1.2f))).toInt()) }
                .alpha(enter.value.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(22.dp)).background(Color(0xFF1A0B0D)).border(1.dp, Color(0x99FF4538), RoundedCornerShape(22.dp)),
        ) {
            HazardStripe(height = 6.dp, gap = Color(0xFF1A0B0D))
            Column(Modifier.padding(horizontal = 14.dp).padding(top = 12.dp, bottom = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(C.Orange), contentAlignment = Alignment.Center) {
                        FIcon(Ic.Flame, size = 17.dp, tint = C.OnOrange)
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(C.Red).padding(horizontal = 8.dp, vertical = 5.dp)) {
                        Text("CRITICAL ALERT", style = mono(11f, Color.White, .16f))
                    }
                    Spacer(Modifier.weight(1f))
                    Text("now", style = sans(13f, C.Muted))
                }
                Spacer(Modifier.height(10.dp))
                Text("Fire detected — ${active.alert.module}", style = sans(21f, Color.White, FontWeight.SemiBold))
                Spacer(Modifier.height(4.dp))
                Text(
                    buildAnnotatedString {
                        val line = active.alert.summaryLine()
                        if (line != "Open the emergency steps.") append("$line · ")
                        withStyle(SpanStyle(color = C.RedText, fontWeight = FontWeight.SemiBold)) { append("${active.alert.risk.name} RISK") }
                    },
                    style = sans(15f, Color(0xFFE6D5D2)),
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(14.dp)).background(C.Red).clickable(onClick = onOpenSteps),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("OPEN STEPS", style = chakra(15f, Color.White, FontWeight.SemiBold, .2f))
                    Spacer(Modifier.width(10.dp))
                    FIcon(Ic.ArrowRight, size = 20.dp, tint = Color.White)
                }
            }
        }
        // alarm status bar
        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 98.dp)
                .fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF110B0C)).border(1.dp, Color(0x66FF4538), RoundedCornerShape(20.dp)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF3A1210)), contentAlignment = Alignment.Center) {
                FIcon(Ic.Vibrate, Modifier.offset(x = if (active.silenced) 0.dp else shake.dp), size = 24.dp, tint = C.RedText)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (active.silenced) "ALARM SILENCED" else "ALARM SOUNDING · VIBRATING",
                style = mono(12f, Color(0xFFE6D5D2), .12f), modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.height(48.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0x88FF7B6E), RoundedCornerShape(12.dp)).clickable(enabled = !active.silenced, onClick = onSilence).padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) { Text("SILENCE", style = chakra(14f, if (active.silenced) C.Muted else Color(0xFFFFE1DC), FontWeight.SemiBold, .14f)) }
        }
    }
}
