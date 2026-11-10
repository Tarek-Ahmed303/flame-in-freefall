package com.flame.astronaut.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.flame.astronaut.domain.GuideContent
import com.flame.astronaut.domain.GuideRepository
import com.flame.astronaut.ui.components.*
import com.flame.astronaut.ui.home.levelColor
import com.flame.astronaut.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GuideViewModel(private val repo: GuideRepository) : ViewModel() {
    private val _guide = MutableStateFlow<GuideContent?>(null)
    val guide: StateFlow<GuideContent?> = _guide.asStateFlow()
    private val _tab = MutableStateFlow(0)
    val tab: StateFlow<Int> = _tab.asStateFlow()

    init { viewModelScope.launch { _guide.value = repo.getGuide() } }
    fun selectTab(i: Int) { _tab.value = i }
}

@Composable
fun GuideScreen(vm: GuideViewModel, onNavigate: (NavTab) -> Unit) {
    val guide by vm.guide.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(C.Bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Safety Guide", style = chakra(32f, C.Text, FontWeight.Bold))
                    Text("Fire response procedure · works offline", style = sans(15f, C.Muted))
                }
                Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF0A2018)).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FIcon(Ic.Check, size = 14.dp, tint = C.Green); Spacer(Modifier.width(8.dp))
                    Text("OFFLINE", style = mono(12.5f, C.Green, .16f))
                }
            }
            Row(Modifier.fillMaxWidth().height(48.dp).panel(C.Surface, 24.dp, C.StrokeStrong).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                FIcon(Ic.Search, size = 22.dp, tint = C.Muted); Spacer(Modifier.width(14.dp))
                Text("Search procedures, materials…", style = sans(16f, C.Faint), modifier = Modifier.weight(1f))
                FIcon(Ic.Mic, size = 22.dp, tint = C.Orange)
            }
            // The material-risk and oxygen cards only exist in demo mode (the backend has no data for them yet).
            val extras = guide?.materials != null
            // segmented control
            if (extras) Row(Modifier.fillMaxWidth().panel(Color(0xFF0A0F18), 18.dp, Color.Transparent).padding(4.dp)) {
                listOf("Fire", "Atmosphere", "Materials").forEachIndexed { i, label ->
                    Box(
                        Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(14.dp)).background(if (i == tab) Color(0xFF161E2E) else Color.Transparent).clickable { vm.selectTab(i) },
                        contentAlignment = Alignment.Center,
                    ) { Text(label, style = sans(16f, if (i == tab) C.Text else C.Muted, if (i == tab) FontWeight.SemiBold else FontWeight.Normal)) }
                }
            }
            val g = guide
            if (g == null) {
                Text("Loading the procedure…", style = sans(15f, C.Muted), modifier = Modifier.padding(top = 8.dp))
            } else if (!extras) {
                FireResponse(g)
            } else {
                if (tab == 0 || tab == 1) FireResponse(g)
                if (tab == 0 || tab == 2) MaterialRisk(g)
                if (tab == 0 || tab == 1) OxygenScale(g)
            }
        }
        FlameBottomNav(NavTab.Guide, onNavigate, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 12.dp))
    }
}

@Composable
private fun FireResponse(g: GuideContent) {
    Column(Modifier.fillMaxWidth().panel(C.Surface, 22.dp, Color(0x55FF7A2F))) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text("FIRE RESPONSE · ${g.fireSteps.size} STEPS", style = mono(12f, C.Orange, .18f), modifier = Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x44FF7A2F)))
        Column(Modifier.padding(horizontal = 16.dp)) {
            g.fireSteps.forEachIndexed { i, s ->
                Row(Modifier.padding(vertical = 14.dp)) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A1810)), contentAlignment = Alignment.Center) { Text("${i + 1}", style = chakra(14f, C.Orange, FontWeight.SemiBold)) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(s.title, style = sans(18f, C.Text, FontWeight.SemiBold))
                        Text(s.body, style = sans(15f, C.Muted))
                    }
                }
                if (i < g.fireSteps.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(C.Stroke))
            }
        }
    }
}

@Composable
private fun MaterialRisk(g: GuideContent) {
    Column(Modifier.fillMaxWidth().panel(C.Surface, 22.dp, C.Stroke).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row {
            Text("MATERIAL RISK", style = mono(12f, C.Muted, .2f), modifier = Modifier.weight(1f))
            Text("at 21% O₂ · low airflow", style = mono(11.5f, C.Faint, .04f))
        }
        g.materials.orEmpty().forEach { m ->
            val col = levelColor(m.level)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(m.name, style = sans(17f, C.Text), modifier = Modifier.width(120.dp), maxLines = 1)
                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF1B2230))) {
                    Box(Modifier.fillMaxWidth(m.fraction).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(col))
                }
                Text(m.level.name, style = mono(13f, if (m.level.name == "HIGH") C.RedText else col, .06f, FontWeight.Medium), modifier = Modifier.width(52.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
    }
}

@Composable
private fun OxygenScale(g: GuideContent) {
    val o2Now = g.o2Now ?: return
    Column(Modifier.fillMaxWidth().panel(C.Surface, 22.dp, C.Stroke).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row {
            Text("OXYGEN THRESHOLDS", style = mono(12f, C.Muted, .2f), modifier = Modifier.weight(1f))
            Text("NOW $o2Now%", style = mono(13f, C.Green, .06f, FontWeight.Medium))
        }
        // 15 → 30 % scale: <19.5 low · 19.5–23.5 nominal · >23.5 fire risk rises
        val lo = 15f; val hi = 30f
        Box(Modifier.fillMaxWidth().height(24.dp).drawBehind {
            fun x(v: Float) = size.width * (v - lo) / (hi - lo)
            val h = 12.dp.toPx(); val y = (size.height - h) / 2; val gap = 3.dp.toPx()
            drawRoundRect(C.Amber, Offset(0f, y), Size(x(19.5f) - gap, h), androidx.compose.ui.geometry.CornerRadius(h / 2))
            drawRoundRect(C.Green, Offset(x(19.5f), y), Size(x(23.5f) - x(19.5f) - gap, h), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            drawRoundRect(C.Red, Offset(x(23.5f), y), Size(size.width - x(23.5f), h), androidx.compose.ui.geometry.CornerRadius(h / 2))
            drawRect(Color.White, Offset(x(o2Now.toFloat()) - 1.dp.toPx(), 0f), Size(2.dp.toPx(), size.height))
        })
        Row(Modifier.fillMaxWidth()) {
            Text("19.5", style = mono(12f, C.Muted), modifier = Modifier.weight(1f))
            Text("nominal 19.5–23.5", style = mono(12f, C.Muted))
            Text("23.5", style = mono(12f, C.Muted), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
}
