package com.flame.astronaut.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.flame.astronaut.ui.components.*
import com.flame.astronaut.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(private val health: com.flame.astronaut.domain.HealthRepository) : ViewModel() {
    private val _progress = MutableStateFlow(0.03f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _health = MutableStateFlow<com.flame.astronaut.domain.HealthStatus?>(null)
    val healthStatus: StateFlow<com.flame.astronaut.domain.HealthStatus?> = _health.asStateFlow()

    init {
        viewModelScope.launch {
            // a cold network right after launch can fail the first probe: retry before showing OFFLINE
            var status = health.check()
            var attempt = 1
            while (!status.backendOk && attempt < 3) {
                delay(1500)
                status = health.check()
                attempt++
            }
            _health.value = status
        }
        viewModelScope.launch {
            val steps = 68
            for (i in 1..steps) {
                delay(50)
                _progress.value = 0.03f + 0.97f * i / steps
            }
        }
    }
}

@Composable
fun SplashScreen(vm: SplashViewModel, onContinue: () -> Unit) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    val health by vm.healthStatus.collectAsStateWithLifecycle()
    val done = progress >= 1f
    LaunchedEffect(done) {
        if (done) {
            delay(1200)
            onContinue()
        }
    }

    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(1200, easing = LinearOutSlowInEasing)) }
    val blink = rememberInfiniteTransition(label = "sync")
    val syncAlpha by blink.animateFloat(1f, .25f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "s")

    Box(
        Modifier
            .fillMaxSize()
            .background(C.BgSplash)
            .drawBehind {
                drawRect(Brush.radialGradient(listOf(Color(0x33FF5A1E), Color.Transparent), Offset(size.width, size.height * .75f), size.width))
                drawRect(Brush.radialGradient(listOf(Color(0x331B3A7A), Color.Transparent), Offset(0f, size.height * .3f), size.width * .9f))
            },
    ) {
        Starfield(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MiniGame(Modifier.fillMaxWidth().weight(1f))
            Column(
                Modifier.padding(horizontal = 40.dp).alpha(enter.value).offset(y = (14 * (1 - enter.value)).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("MISSION SAFETY SYSTEM", style = mono(11.5f, C.Muted, .32f))
                Spacer(Modifier.height(14.dp))
                Text("FLAME IN", style = chakra(46f, Color(0xFFE8EEF6), FontWeight.Bold, .06f), textAlign = TextAlign.Center)
                Text("FREEFALL", style = chakra(46f, C.Orange, FontWeight.Bold, .06f), textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text("Astronaut Emergency Response", style = sans(17f, C.Text2))
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.padding(horizontal = 40.dp)) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0x1F96B4DC))) {
                    Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(C.Orange))
                }
                Spacer(Modifier.height(16.dp))
                val h = health
                if (h == null) {
                    StatusLine("Station backend link", "CHECKING", C.Amber, true, Modifier.alpha(syncAlpha))
                    StatusLine("Flame AI core", "CHECKING", C.Amber, true, Modifier.alpha(syncAlpha))
                    StatusLine("Fire-risk telemetry", "SYNCING", C.Amber, true, Modifier.alpha(syncAlpha))
                } else {
                    StatusLine("Station backend link", if (h.backendOk) "OK" else "OFFLINE", if (h.backendOk) C.Green else C.Red, true)
                    StatusLine("Flame AI core", if (h.aiOnline) "ONLINE" else "FALLBACK", if (h.aiOnline) C.Green else C.Amber, true)
                    // The backend has no telemetry feed yet: be honest about it instead of showing "OK".
                    if (com.flame.astronaut.BuildConfig.USE_FAKE) {
                        StatusLine("Fire-risk telemetry", if (h.backendOk) "OK" else "OFFLINE", if (h.backendOk) C.Green else C.Red, true)
                    } else {
                        StatusLine("Fire-risk telemetry", "NOT CONNECTED", C.Amber, true)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 12.dp).clickableNoRipple(onContinue),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("NASA SPACE APPS 2026 · v1.0", style = mono(9.5f, C.Faint, .06f), maxLines = 1, softWrap = false)
                Spacer(Modifier.weight(1f))
                Text("TAP TO CONTINUE", style = mono(9.5f, C.Text, .1f), maxLines = 1, softWrap = false)
                Spacer(Modifier.width(6.dp))
                FIcon(Ic.ChevronRight, size = 14.dp, tint = C.Text)
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String, color: Color, shown: Boolean, valueModifier: Modifier = Modifier) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).alpha(if (shown) 1f else .35f), verticalAlignment = Alignment.Bottom) {
        Text(label, style = sans(14.5f, C.Text2))
        Spacer(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .height(1.dp)
                .drawBehind {
                    var x = 0f
                    while (x < size.width) {
                        drawCircle(Color(0x4496B4DC), 0.7f * density, Offset(x, 0f)); x += 3 * density
                    }
                },
        )
        Text(if (shown) value else "···", style = mono(14f, color, .06f), modifier = valueModifier)
    }
}
