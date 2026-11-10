package com.flame.astronaut.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp as colorLerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val Dark = Color(0xFF18171A)
private val FlameOrange = Color(0xFFFF8B19)
private val EdgeOrange = Color(0xFFF96E0C)
private val Ground = Color(0xFFF2F3F8)
private val Ink = Color(0xFF1B1210)
private val Glow = Color(255, 196, 96)

private const val DURATION_S = 3.2f

private fun inOut(x: Float) = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).let { it * it * it } / 2f
private fun backOut(x: Float): Float { val d = x - 1f; return 1f + 1.9f * d * d * d + 0.9f * d * d }
private fun prog(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun lerpF(a: Float, b: Float, f: Float) = a + (b - a) * f

// Full-screen rect with a 2px overshoot, 12 vertices on the 390x844 reference canvas.
private val RECT = listOf(
    195f to 846f, 392f to 846f, 392f to 560f, 392f to 250f, 392f to -2f, 316f to -2f,
    240f to -2f, 120f to -2f, -2f to -2f, -2f to 250f, -2f to 560f, -2f to 846f,
)

// Flame outline relative to (cx, cy); indices 4, 6 and 8 are the three tongue tips.
private val FLAME = listOf(
    0f to 62f, 62f to 54f, 76f to 2f, 60f to -44f, 64f to -92f, 34f to -52f,
    12f to -148f, -18f to -62f, -46f to -108f, -62f to -36f, -76f to 8f, -62f to 52f,
)

private fun DrawScope.buildPath(t: Float, m: Float, k: Float, cx: Float, cy: Float, bob: Float, sy: Float): Path {
    val w = size.width
    val h = size.height
    val rect = RECT.map { Offset(it.first * w / 390f, it.second * h / 844f) }

    val flame = FLAME.mapIndexed { i, (fx, fy) ->
        var x = fx
        var y = fy
        when (i) {
            4 -> { x += sin(t * 8f + 1f) * 2.6f; y += sin(t * 9f) * 3f }
            6 -> { x += sin(t * 7f) * 3.6f; y += sin(t * 6f + 2f) * 4.5f }
            8 -> { x += sin(t * 8.5f + 3f) * 2.6f; y += sin(t * 7.5f + 4f) * 3.5f }
            5, 7 -> x += sin(t * 5f + i) * 1.2f
        }
        val yy = 62f + (y - 62f) * sy + bob
        Offset(cx + x * k, cy + yy * k)
    }

    fun tangent(i: Int): Offset =
        if (i == 4 || i == 6 || i == 8) Offset.Zero else (flame[(i + 1) % 12] - flame[(i + 11) % 12]) * 0.5f

    val path = Path()
    val start = lerp(rect[0], flame[0], m)
    path.moveTo(start.x, start.y)
    for (i in 0 until 12) {
        val j = (i + 1) % 12
        val rc1 = rect[i] + (rect[j] - rect[i]) / 3f
        val rc2 = rect[i] + (rect[j] - rect[i]) * (2f / 3f)
        val fc1 = flame[i] + tangent(i) / 3f
        val fc2 = flame[j] - tangent(j) / 3f
        val c1 = lerp(rc1, fc1, m)
        val c2 = lerp(rc2, fc2, m)
        val end = lerp(rect[j], flame[j], m)
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)
    }
    path.close()
    return path
}

private fun DrawScope.drawFlameFace(t: Float, m: Float, k: Float, cx: Float, cy: Float, bob: Float) {
    val mc = m.coerceIn(0f, 1.04f)
    val fs = lerpF(1.9f, 1f, mc)
    val faceY = lerpF(0.52f * size.height, cy + (12f + bob) * k, m.coerceIn(0f, 1f))
    val look = if (t > 0.15f) sin((t - 0.15f) * 2.6f) else 0f
    val blink = max(0f, 1f - abs(t - 2.4f) / 0.09f)
    val u = fs * k

    for (side in listOf(-1f, 1f)) {
        val ex = cx + side * 25f * u + look * 2.4f * u
        val ry = 9.4f * u * (1f - 0.9f * blink)
        drawOval(Ink, Offset(ex - 7.2f * u, faceY - ry), Size(14.4f * u, ry * 2f))
        if (blink < 0.5f) {
            drawCircle(Color.White, 2.5f * u, Offset(ex + 2.6f * u, faceY - 3.6f * u))
            drawCircle(Color.White, 1.2f * u, Offset(ex - 2.4f * u, faceY + 3.4f * u))
        }
    }

    // smile: lower arc, 0.16π → 0.84π
    val sc = Offset(cx + look * 0.4f * u, faceY + 15f * u)
    val r = 9f * u
    drawArc(
        color = Ink,
        startAngle = (0.16f * PI).toFloat() * 180f / PI.toFloat(),
        sweepAngle = (0.68f * PI).toFloat() * 180f / PI.toFloat(),
        useCenter = false,
        topLeft = Offset(sc.x - r, sc.y - r),
        size = Size(r * 2f, r * 2f),
        style = Stroke(width = 2.8f * u, cap = StrokeCap.Round),
    )
}

/**
 * First splash: one Canvas, one Animatable time driver (0..3.2 s). The time value is read only inside the
 * draw lambda, so nothing recomposes per frame. [onFinished] fires when the animation completes.
 */
@Composable
fun IntroSplashScreen(onFinished: () -> Unit) {
    val time = remember { Animatable(0f) }
    val finished = rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        time.animateTo(DURATION_S, tween(durationMillis = 3200, easing = LinearEasing))
        finished.value()
    }

    Canvas(Modifier.fillMaxSize()) {
        val t = time.value
        val w = size.width
        val h = size.height
        val k = min(w / 390f, h / 844f)
        val cx = w / 2f
        val cy = 0.521f * h

        val fade = colorLerp(Dark, FlameOrange, inOut(prog(t, 0f, 0.25f)))
        val bg = if (t < 1.1f) fade else Ground
        drawRect(bg)

        val m = if (t < 1.1f) 0f else backOut(prog(t, 1.1f, 1.7f))
        val idleW = prog(t, 1.7f, 1.9f)
        val bob = idleW * sin((t - 1.7f) / 1.2f * 2f * PI.toFloat()) * 6f
        val sy = 1f + idleW * 0.02f * sin(t * 4.5f)

        val path = buildPath(t, m, k, cx, cy, bob, sy)

        if (m > 0.9f) {
            val rx = (62f - bob * 0.6f) * k
            val ry = (7f - bob * 0.2f) * k
            drawOval(Color(40, 20, 10, 18), Offset(cx - rx, cy + 96f * k - ry), Size(rx * 2f, ry * 2f))
        }
        translate(-6f * k, -4f * k) { drawPath(path, EdgeOrange) }
        drawPath(path, if (t < 1.1f) fade else FlameOrange)

        if (m > 0.5f) {
            clipPath(path) {
                val gc = Offset(cx, cy + 40f * k + bob * k)
                drawRect(
                    Brush.radialGradient(
                        0f to Glow.copy(alpha = 0.55f * m.coerceIn(0f, 1f)),
                        1f to Glow.copy(alpha = 0f),
                        center = gc,
                        radius = 90f * k,
                    ),
                )
            }
        }

        drawFlameFace(t, m, k, cx, cy, bob)
    }
}
