package com.flame.astronaut.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flame.astronaut.ui.theme.*
import kotlin.random.Random

// ---------------------------------------------------------------- helpers

fun Modifier.panel(bg: Color, radius: Dp = 16.dp, border: Color = C.Stroke) =
    this.clip(RoundedCornerShape(radius)).background(bg).border(1.dp, border, RoundedCornerShape(radius))

fun Modifier.clickableNoRipple(onClick: () -> Unit) =
    this.clickable(interactionSource = MutableInteractionSource(), indication = null, role = Role.Button, onClick = onClick)

private class Chamfer(private val cutDp: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): Outline {
        val c = cutDp * density.density
        val p = Path().apply {
            moveTo(c, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height - c)
            lineTo(size.width - c, size.height); lineTo(0f, size.height); lineTo(0f, c); close()
        }
        return Outline.Generic(p)
    }
}

// ---------------------------------------------------------------- starfield

private data class Star(val x: Float, val y: Float, val r: Float, val color: Color)

private val layer1 = listOf(
    Star(23f, 41f, 1.3f, Color.White), Star(141f, 92f, 1f, Color.White.copy(.75f)),
    Star(262f, 18f, 1.6f, Color(0xFFD6E9FF)), Star(77f, 210f, 1f, Color.White.copy(.6f)),
    Star(198f, 252f, 1.2f, Color.White), Star(306f, 160f, 1f, Color(0xFFFFD6BE).copy(.85f)),
    Star(338f, 282f, 1f, Color.White.copy(.7f)), Star(360f, 90f, 1f, Color.White.copy(.6f)),
)
private val layer2 = run {
    val r = Random(7)
    listOf(Star(40f, 20f, 1f, Color.White.copy(.55f)), Star(150f, 110f, 1f, Color.White.copy(.45f)), Star(180f, 30f, 1.4f, Color.White)) +
        List(10) { Star(r.nextFloat() * 390f, r.nextFloat() * 300f, 1f, Color.White.copy(.5f)) }
}

/** Two drifting star layers (90s / 45s loops) + twinkle on the second. */
@Composable
fun Starfield(modifier: Modifier = Modifier, alpha: Float = 1f, slow: Boolean = false) {
    val t = rememberInfiniteTransition(label = "stars")
    val d1 by t.animateFloat(0f, 300f, infiniteRepeatable(tween(if (slow) 120_000 else 90_000, easing = LinearEasing)), label = "d1")
    val d2 by t.animateFloat(0f, 300f, infiniteRepeatable(tween(45_000, easing = LinearEasing)), label = "d2")
    val tw by t.animateFloat(1f, .45f, infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tw")
    Canvas(modifier.alpha(alpha)) {
        fun draw(stars: List<Star>, drift: Float, a: Float) {
            val tileH = 300f
            val tileW = 390f
            var ty = -tileH + drift
            while (ty < size.height / density) {
                var tx = 0f
                while (tx < size.width / density) {
                    stars.forEach { s ->
                        drawCircle(s.color.copy(alpha = s.color.alpha * a), s.r * density, Offset((tx + s.x) * density, (ty + s.y) * density))
                    }
                    tx += tileW
                }
                ty += tileH
            }
        }
        draw(layer1, d1, 1f)
        draw(layer2, d2, tw)
    }
}

// ---------------------------------------------------------------- buttons

@Composable
fun ChamferedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    withArrow: Boolean = true,
    loading: Boolean = false,
) {
    Row(
        modifier
            .height(height)
            .clip(Chamfer(14f))
            .background(C.Orange)
            .clickable(enabled = !loading, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (loading) "…" else text, style = chakra(16f, C.OnOrange, FontWeight.Bold, .2f))
        if (withArrow && !loading) {
            Spacer(Modifier.width(10.dp))
            FIcon(Ic.ArrowRight, size = 20.dp, tint = C.OnOrange)
        }
    }
}

// ---------------------------------------------------------------- mic / waveform

@Composable
fun PulseMic(
    size: Dp,
    modifier: Modifier = Modifier,
    ringMillis: Int = 2200,
    iconSize: Dp = 26.dp,
    onClick: () -> Unit = {},
) {
    val t = rememberInfiniteTransition(label = "ring")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(ringMillis, easing = LinearOutSlowInEasing)), label = "p")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().scale(1f)) {
            val s = .7f + .9f * p
            drawCircle(C.Orange.copy(alpha = .7f * (1f - p)), radius = this.size.minDimension / 2 * s, style = Stroke(1.5f * density))
        }
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(Brush.radialGradient(listOf(C.Orange.copy(.45f), Color.Transparent), radius = this.size.minDimension * .85f))
                }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFFFB27A), C.Orange, Color(0xFFC2410C)), center = Offset(.35f * 300f, .3f * 300f), radius = 520f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { FIcon(Ic.Mic, size = iconSize, tint = C.OnOrange) }
    }
}

private val waveDelays = listOf(0, 100, 250, 400, 150, 300, 50, 350, 200, 450, 120, 280)

@Composable
fun Waveform(modifier: Modifier = Modifier, height: Dp = 26.dp, color: Color = C.Orange) {
    val t = rememberInfiniteTransition(label = "wave")
    Row(modifier.height(height), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        waveDelays.forEach { d ->
            val s by t.animateFloat(
                .25f, 1f,
                infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(d)),
                label = "bar",
            )
            Box(Modifier.width(3.dp).fillMaxHeight(s).clip(RoundedCornerShape(2.dp)).background(color))
        }
    }
}

@Composable
fun BlinkDot(color: Color, size: Dp = 8.dp, millis: Int = 1600, glow: Boolean = true) {
    val t = rememberInfiniteTransition(label = "blink")
    val a by t.animateFloat(1f, .3f, infiniteRepeatable(tween(millis), RepeatMode.Reverse), label = "a")
    Box(
        Modifier
            .size(size)
            .alpha(a)
            .drawBehind {
                if (glow) drawCircle(color.copy(.5f), radius = this.size.minDimension * 1.1f)
            }
            .clip(CircleShape)
            .background(color)
    )
}

// ---------------------------------------------------------------- hazard stripe

@Composable
fun HazardStripe(modifier: Modifier = Modifier, height: Dp = 5.dp, stripe: Color = C.Red, gap: Color = Color(0xE6280808)) {
    Canvas(modifier.fillMaxWidth().height(height)) {
        drawRect(gap)
        val h = size.height
        val period = 16f * density * 1.4142f
        var x = -h
        while (x < size.width + h) {
            val p = Path().apply {
                moveTo(x + h, 0f); lineTo(x + h + period / 2, 0f); lineTo(x + period / 2, h); lineTo(x, h); close()
            }
            drawPath(p, stripe)
            x += period
        }
    }
}

// ---------------------------------------------------------------- bottom nav

enum class NavTab(val label: String, val icon: ImageVector) {
    Home("HOME", Ic.Home), Ai("AI", Ic.Chat), Guide("GUIDE", Ic.Guide), Settings("SETTINGS", Ic.Settings)
}

@Composable
fun FlameBottomNav(selected: NavTab, onSelect: (NavTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .panel(Color(0xE60B1018), 24.dp, C.StrokeStrong)
            .padding(horizontal = 8.dp),
    ) {
        NavTab.values().forEach { tab ->
            val sel = tab == selected
            Column(
                Modifier.weight(1f).height(62.dp).clickableNoRipple { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                FIcon(tab.icon, size = 24.dp, tint = if (sel) C.Orange else C.Muted)
                Spacer(Modifier.height(6.dp))
                Text(tab.label, style = mono(10.5f, if (sel) C.Orange else C.Muted, .14f))
            }
        }
    }
}

// ---------------------------------------------------------------- channel input (HUD)

@Composable
fun ChannelInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    rightLabel: String? = null,
    rightLabelColor: Color = C.Muted,
    leading: ImageVector? = null,
    prefix: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    password: Boolean = false,
    showPassword: Boolean = false,
    allCorners: Boolean = true,
    mono: Boolean = false,
    height: Dp = 54.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val accent = if (focused) C.Cyan else C.Faint.copy(alpha = .75f)

    val t = rememberInfiniteTransition(label = "scan")
    val scan by t.animateFloat(-.3f, 1.2f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "scan")

    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = mono(11f, C.Muted, .18f))
            Spacer(Modifier.weight(1f))
            if (rightLabel != null) Text(rightLabel, style = mono(11f, rightLabelColor, .18f))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .drawBehind {
                    if (focused) {
                        drawRoundRect(C.Cyan.copy(.08f), Offset(-4 * density, -4 * density), Size(size.width + 8 * density, size.height + 8 * density), androidx.compose.ui.geometry.CornerRadius(6 * density))
                    }
                }
                .clip(RoundedCornerShape(3.dp))
                .background(if (focused) Color(0xFF07131D) else Color(0xCC0A0F18))
                .border(1.dp, if (focused) C.Cyan.copy(.65f) else C.Stroke, RoundedCornerShape(3.dp))
                .drawWithContentOverlay(focused, scan, accent, allCorners),
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (leading != null) {
                    FIcon(leading, size = 22.dp, tint = if (focused) C.Cyan else C.Muted)
                    Spacer(Modifier.width(14.dp))
                }
                if (prefix != null) {
                    Text(prefix, style = mono(16f, C.Cyan, 0.04f, FontWeight.Normal))
                    Spacer(Modifier.width(12.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = sans(16f, C.Faint))
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        interactionSource = source,
                        textStyle = if (mono) mono(18f, C.Text, .12f, FontWeight.Normal) else sans(16f, C.Text),
                        cursorBrush = SolidColor(C.Cyan),
                        visualTransformation = if (password && !showPassword) PasswordVisualTransformation('•') else VisualTransformation.None,
                        keyboardOptions = keyboardOptions,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
        }
        // tick ruler under the focused field
        if (focused) {
            Canvas(Modifier.fillMaxWidth().padding(top = 10.dp).height(6.dp)) {
                var x = 0f
                while (x < size.width) {
                    drawLine(C.Cyan.copy(.45f), Offset(x, 0f), Offset(x, size.height), 1f * density)
                    x += 8 * density
                }
            }
        }
    }
}

private fun Modifier.drawWithContentOverlay(focused: Boolean, scan: Float, accent: Color, allCorners: Boolean): Modifier =
    this.drawWithContent {
        drawContent()
        if (focused) {
            val w = size.width * .35f
            val x = size.width * scan
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, C.Cyan.copy(.14f), Color.Transparent), x, x + w),
                Offset(x, 0f), Size(w, size.height),
            )
        }
        val len = 10 * density
        val sw = 1.5f * density
        fun corner(ox: Float, oy: Float, dx: Float, dy: Float) {
            drawLine(accent, Offset(ox, oy), Offset(ox + dx * len, oy), sw)
            drawLine(accent, Offset(ox, oy), Offset(ox, oy + dy * len), sw)
        }
        corner(0f, 0f, 1f, 1f)
        corner(size.width, size.height, -1f, -1f)
        if (allCorners || focused) {
            corner(size.width, 0f, -1f, 1f)
            corner(0f, size.height, 1f, -1f)
        }
    }