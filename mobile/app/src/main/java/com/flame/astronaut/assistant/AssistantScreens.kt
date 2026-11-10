@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.flame.astronaut.assistant

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flame.astronaut.ui.components.FIcon
import com.flame.astronaut.ui.components.Ic
import com.flame.astronaut.ui.components.panel
import com.flame.astronaut.ui.theme.C
import com.flame.astronaut.ui.theme.chakra
import com.flame.astronaut.ui.theme.mono
import com.flame.astronaut.ui.theme.sans
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

// ============================================================== recent chats (GET /chats)

@Composable
fun AssistantListScreen(
    vm: AssistantListViewModel,
    onBack: () -> Unit,
    onNewChat: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    // coming back from a conversation: refresh quietly so new/updated chats show up
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Ic.Back, onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("AI Assistant", style = chakra(22f, C.Text, FontWeight.SemiBold))
                Text("RESEARCH AGENT · RECENT CHATS", style = mono(10.5f, C.Muted, .14f))
            }
        }
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(C.Orange).clickable(onClick = onNewChat),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            FIcon(Ic.Plus, size = 20.dp, tint = C.OnOrange)
            Spacer(Modifier.width(8.dp))
            Text("NEW CHAT", style = chakra(14f, C.OnOrange, FontWeight.Bold, .14f))
        }
        Spacer(Modifier.height(12.dp))

        when (val s = state) {
            ChatListState.Loading -> CenterMessage("Loading your chats…")
            ChatListState.Empty -> CenterMessage("No conversations yet.\nAsk the assistant about fire behavior in microgravity.")
            is ChatListState.Error -> Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(s.message, style = sans(15f, C.RedText, lh = 1.4f), textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                RetryButton(vm::load)
            }
            is ChatListState.Content -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(s.chats, key = { it.id }) { c ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF0B1018)).border(1.dp, C.Stroke, RoundedCornerShape(16.dp))
                            .clickable { onOpen(c.id) }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(c.title?.takeIf { it.isNotBlank() } ?: "New chat", style = sans(16f, C.Text, FontWeight.Medium), maxLines = 2)
                        formatServerDate(c.updatedAt)?.let { Text(it, style = mono(10.5f, C.Faint, .12f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = sans(15f, C.Muted, lh = 1.4f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun RetryButton(onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, Color(0x88FF7B6E), RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 9.dp)) {
        Text("RETRY", style = chakra(12.5f, Color(0xFFFFC9C2), FontWeight.SemiBold, .12f))
    }
}

/** Laravel timestamps are ISO-8601 UTC ("2026-10-23T21:15:57.000000Z"). */
private fun formatServerDate(iso: String?): String? = iso?.let {
    runCatching { SimpleDateFormat("MMM d · HH:mm", Locale.US).format(Date(Instant.parse(it).toEpochMilli())).uppercase() }.getOrNull()
}

// ============================================================== chat

@Composable
fun AssistantChatScreen(vm: AssistantChatViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val messages by vm.messages.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var input by rememberSaveable { mutableStateOf("") }
    var sheet by remember { mutableStateOf<Pair<String, AskResponse>?>(null) }

    val canSend = state !is AssistantUiState.Loading && history is HistoryState.Ready

    // --- voice in / out
    var listening by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }
    val speech = remember { SpeechInput(context, onText = { t, _ -> input = t }, onState = { l, e -> listening = l; voiceError = e }) }
    val speaker = remember { Speaker(context) }
    DisposableEffect(Unit) { onDispose { speech.destroy(); speaker.shutdown() } }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) voiceError = "Microphone permission is needed for voice input."
    }

    val extra = if (state is AssistantUiState.Loading || state is AssistantUiState.Error) 1 else 0
    LaunchedEffect(messages.size, state) {
        val count = messages.size + extra
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().imePadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Ic.Back, onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("AI Assistant", style = chakra(20f, C.Text, FontWeight.SemiBold))
                Text("RESEARCH AGENT", style = mono(10.5f, C.Green, .14f))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Stroke))

        when (val h = history) {
            HistoryState.Loading -> Box(Modifier.weight(1f).fillMaxWidth()) { CenterMessage("Loading conversation…") }
            is HistoryState.Error -> Column(Modifier.weight(1f).fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(h.message, style = sans(15f, C.RedText, lh = 1.4f), textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                RetryButton { vm.loadHistory() }
            }
            HistoryState.Ready -> LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (messages.isEmpty() && state !is AssistantUiState.Loading) {
                    item {
                        Text(
                            "Ask about oxygen, pressure, airflow or materials. Answers are grounded in the microgravity combustion experiments.",
                            style = sans(15f, C.Muted, lh = 1.4f), modifier = Modifier.padding(vertical = 24.dp), textAlign = TextAlign.Center,
                        )
                    }
                }
                items(messages, key = { it.id }) { m ->
                    MessageItem(m, onRead = { speaker.speak(it) }, onSource = { id, r -> sheet = id to r })
                }
                if (state is AssistantUiState.Loading) item { TypingIndicator() }
                (state as? AssistantUiState.Error)?.let { e -> item { ErrorBubble(e.message, e.retry) } }
            }
        }

        // --- composer
        voiceError?.let { Text(it, style = sans(12.5f, C.RedText), modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).background(Color(0xFF0B1220)).border(1.dp, C.StrokeStrong, RoundedCornerShape(27.dp)).padding(start = 20.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (input.isEmpty()) Text(if (listening) "Listening…" else "Ask the research assistant…", style = sans(16f, C.Faint))
                    BasicTextField(
                        input, { input = it }, textStyle = sans(16f, C.Text), singleLine = true,
                        cursorBrush = SolidColor(C.Orange), modifier = Modifier.fillMaxWidth(),
                    )
                }
                Box(
                    Modifier.size(42.dp).clip(CircleShape).alpha(if (canSend && input.isNotBlank()) 1f else .35f)
                        .clickable(enabled = canSend && input.isNotBlank()) { vm.send(input); input = "" },
                    contentAlignment = Alignment.Center,
                ) { FIcon(Ic.Send, size = 22.dp, tint = C.Orange) }
            }
            Spacer(Modifier.width(10.dp))
            // push-to-talk: hold to speak, release to stop
            Box(
                Modifier.size(54.dp).clip(CircleShape).background(if (listening) C.Red else C.Orange)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                voiceError = null
                                speech.start()
                                tryAwaitRelease()
                                speech.stop()
                            } else {
                                micPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        })
                    },
                contentAlignment = Alignment.Center,
            ) { FIcon(Ic.Mic, size = 24.dp, tint = C.OnOrange) }
        }
    }

    sheet?.let { (id, response) ->
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF0B1018),
        ) { SourceSheet(id, response) }
    }
}

// ============================================================== messages

@Composable
private fun MessageItem(m: ChatMessageUi, onRead: (String) -> Unit, onSource: (String, AskResponse) -> Unit) {
    if (m.fromUser) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)).background(C.Orange).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(m.text, style = sans(16f, C.OnOrange, FontWeight.Medium, 1.4f))
            }
        }
    } else {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val r = m.response
            if (r == null) {
                // stored text only (response JSON missing or unreadable)
                Box(Modifier.fillMaxWidth().panel(Color(0xFF0B1018), 18.dp).padding(16.dp)) { Text(m.text, style = sans(16f, Color(0xFFD3DBE6), lh = 1.45f)) }
            } else {
                AssistantCard(r, onRead = { onRead(readAloudText(r)) }, onSource = { onSource(it, r) })
            }
            // The ML notice is shown whenever the answer carries a prediction.
            if (!r?.prediction.isNullOrEmpty()) {
                Text(
                    r?.predictionNotice?.takeIf { it.isNotBlank() } ?: DEFAULT_PREDICTION_NOTICE,
                    style = mono(10f, C.Faint, .02f, FontWeight.Normal), modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun AssistantCard(r: AskResponse, onRead: () -> Unit, onSource: (String) -> Unit) {
    val evidence = r.experimentalEvidence.orEmpty()
    val prediction = r.prediction.orEmpty()
    val trends = r.observedTrends.orEmpty().filter { it.isNotBlank() }
    val limitations = r.limitations.orEmpty().filter { it.isNotBlank() }
    val sources = r.sources.orEmpty().filter { it.isNotBlank() }

    Column(Modifier.fillMaxWidth().panel(Color(0xFF0B1018), 18.dp)) {
        if (r.answerability?.answerable == false) {
            Row(Modifier.fillMaxWidth().background(Color(0x26FFC042)).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                FIcon(Ic.Warning, size = 18.dp, tint = C.Amber)
                Spacer(Modifier.width(10.dp))
                Text("This question may not be covered by the experiments", style = sans(13.5f, C.Amber, FontWeight.Medium))
            }
        }
        if (!r.answer.isNullOrBlank()) Section("ANSWER", expanded = true) { Text(r.answer, style = sans(16f, Color(0xFFD3DBE6), lh = 1.45f)) }
        if (evidence.isNotEmpty()) Section("EXPERIMENTAL EVIDENCE") {
            evidence.forEach { e ->
                Column(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(e.experimentId ?: "—", C.OrangeLight)
                        e.source?.let { Text(it, style = mono(11f, C.Muted, .06f)) }
                        e.oxygenConcentration?.let { Text("O₂ ${fmt(it)}%", style = mono(11f, C.Cyan, .04f)) }
                    }
                    if (!e.finding.isNullOrBlank()) Text(e.finding, style = sans(14.5f, C.Text2, lh = 1.4f))
                }
            }
        }
        if (prediction.isNotEmpty()) Section("PREDICTION") {
            prediction.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(p.experimentId ?: "—", C.OrangeLight)
                    RiskChip(p.riskBand)
                    p.probability?.let { Text("${fmt(it * 100, 0)}%", style = mono(13f, C.Text, .02f, FontWeight.Medium)) }
                    Text(if (p.fire == true) "fire" else if (p.fire == false) "no fire" else (p.label ?: ""), style = sans(14f, C.Text2))
                }
            }
        }
        if (trends.isNotEmpty()) Section("OBSERVED TRENDS") {
            trends.forEach { Bullet(it) }
        }
        if (!r.interpretation.isNullOrBlank()) Section("INTERPRETATION") { Text(r.interpretation, style = sans(15f, Color(0xFFD3DBE6), lh = 1.45f)) }
        if (!r.safetyInsight.isNullOrBlank()) Section("SAFETY INSIGHT", expanded = true, accent = C.OrangeLight, highlight = true) {
            Text(r.safetyInsight, style = sans(15.5f, Color(0xFFFFE1CB), FontWeight.Medium, 1.45f))
        }
        if (limitations.isNotEmpty()) Section("LIMITATIONS") { limitations.forEach { Bullet(it) } }

        if (sources.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sources.forEach { s ->
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, Color(0x73FF7A2F), RoundedCornerShape(20.dp)).clickable { onSource(s) }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(s, style = mono(12f, C.OrangeLight, .06f))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 12.dp, top = 4.dp)) {
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0x73FF7A2F), RoundedCornerShape(12.dp)).clickable(onClick = onRead).padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FIcon(Ic.Speaker, size = 16.dp, tint = C.OrangeLight)
                Spacer(Modifier.width(8.dp))
                Text("Read aloud", style = sans(14f, C.OrangeLight))
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    expanded: Boolean = false,
    accent: Color = C.Muted,
    highlight: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    var open by rememberSaveable(title) { mutableStateOf(expanded) }
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)
            .then(if (highlight) Modifier.clip(shape).background(Color(0x1AFF7A2F)).border(1.dp, Color(0x66FF7A2F), shape) else Modifier)
            .animateContentSize(),
    ) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = mono(11.5f, accent, .16f), modifier = Modifier.weight(1f))
            FIcon(Ic.ChevronUp, Modifier.rotate(if (open) 0f else 180f), 18.dp, C.Muted)
        }
        if (open) Column(Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp), content = content)
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text("•", style = sans(15f, C.Orange), modifier = Modifier.padding(end = 8.dp))
        Text(prettify(text), style = sans(14.5f, Color(0xFFD3DBE6), lh = 1.45f))
    }
}

/** The model sometimes emits machine tags such as "insufficient_evidence": show them as plain words. */
private fun prettify(s: String): String =
    if (Regex("[a-z]+(_[a-z]+)+").matches(s.trim())) s.trim().replace('_', ' ').replaceFirstChar { it.uppercase() } else s

@Composable
private fun Chip(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = .14f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text, style = mono(11.5f, color, .06f, FontWeight.Medium))
    }
}

@Composable
private fun RiskChip(band: String?) {
    val b = band?.uppercase().orEmpty()
    val (bg, fg) = when (b) {
        "HIGH" -> C.Red to Color.White
        "MEDIUM", "MED" -> C.Amber to C.OnOrange
        "LOW" -> C.Green to Color(0xFF04140C)
        else -> Color(0xFF2A3342) to C.Text2
    }
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(if (b.isEmpty()) "—" else b, style = mono(11.5f, fg, .12f, FontWeight.Medium))
    }
}

@Composable
private fun TypingIndicator() {
    Box(Modifier.clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).background(Color(0xFF0F1520)).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Flame AI is thinking…", style = mono(12f, C.Muted, .04f))
    }
}

@Composable
private fun ErrorBubble(message: String, retry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF2A1010)).border(1.dp, Color(0x66FF4538), RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(message, style = sans(15f, Color(0xFFFFD6D1), lh = 1.4f))
        Box(Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, Color(0x88FF7B6E), RoundedCornerShape(10.dp)).clickable(onClick = retry).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("RETRY", style = chakra(12.5f, Color(0xFFFFC9C2), FontWeight.SemiBold, .12f))
        }
    }
}

// ============================================================== source bottom sheet

@Composable
private fun SourceSheet(id: String, r: AskResponse) {
    val evidence = r.experimentalEvidence.orEmpty().filter { it.experimentId == id }
    val prediction = r.prediction.orEmpty().filter { it.experimentId == id }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(id, style = chakra(22f, C.Text, FontWeight.Bold))
        if (evidence.isEmpty() && prediction.isEmpty()) {
            Text("No details are available for this experiment in this answer.", style = sans(15f, C.Muted))
        }
        evidence.forEach { e ->
            Text("EXPERIMENTAL EVIDENCE", style = mono(11.5f, C.Muted, .16f))
            e.source?.let { Fact("Source", it) }
            e.oxygenConcentration?.let { Fact("Oxygen concentration", "${fmt(it)} %") }
            e.flameTemperature?.let { Fact("Flame temperature", fmt(it)) }
            if (!e.finding.isNullOrBlank()) Text(e.finding, style = sans(15f, Color(0xFFD3DBE6), lh = 1.45f))
        }
        prediction.forEach { p ->
            Text("PREDICTION", style = mono(11.5f, C.Muted, .16f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RiskChip(p.riskBand)
                p.probability?.let { Text("${fmt(it * 100, 0)}%", style = mono(14f, C.Text, .02f, FontWeight.Medium)) }
                Text(if (p.fire == true) "fire" else if (p.fire == false) "no fire" else (p.label ?: ""), style = sans(14.5f, C.Text2))
            }
            p.threshold?.let { Fact("Threshold", fmt(it)) }
            p.model?.let { Fact("Model", it) }
            p.inputs?.let { i ->
                Text("INPUTS", style = mono(11.5f, C.Muted, .16f))
                i.oxygenConcentrationPct?.let { Fact("Oxygen", "${fmt(it)} %") }
                i.pressureKpa?.let { Fact("Pressure", "${fmt(it)} kPa") }
                i.flowVelocityCmS?.let { Fact("Flow velocity", "${fmt(it)} cm/s") }
                i.ambientTemperatureC?.let { Fact("Ambient temperature", "${fmt(it)} °C") }
                i.material?.let { Fact("Material", it) }
                i.materialType?.let { Fact("Material type", it) }
                i.flowConfiguration?.let { Fact("Flow configuration", it) }
            }
            if (!p.note.isNullOrBlank()) Text(p.note, style = mono(10.5f, C.Faint, .02f, FontWeight.Normal))
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF111826)).padding(horizontal = 12.dp, vertical = 9.dp)) {
        Text(label, style = sans(14.5f, C.Text2), modifier = Modifier.weight(1f))
        Text(value, style = mono(13.5f, C.Text, 0f, FontWeight.Normal))
    }
}

@Composable
private fun RoundIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF111826)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        FIcon(icon, size = 22.dp, tint = C.Text)
    }
}

private fun fmt(v: Double, decimals: Int = 2): String = String.format(Locale.US, "%.${decimals}f", v).let {
    if (decimals > 0) it.trimEnd('0').trimEnd('.') else it
}
