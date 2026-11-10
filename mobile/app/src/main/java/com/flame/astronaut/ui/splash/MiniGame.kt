package com.flame.astronaut.ui.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.flame.astronaut.ui.theme.C
import com.flame.astronaut.ui.theme.mono
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random



private class Enemy(val bx: Float, val by: Float, val ph: Float) {
    var x = 0f; var y = 0f; var alive = true
}
private class Ship { var x = 195f; var y = 440f; var tx = 195f; var hit = 0f }
private class Shot(var x: Float, var y: Float)
private class Fire(var x: Float, var y: Float, val vy: Float, val wob: Float)
private class Part(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val max: Float, val r: Float, val col: Color)

private class GameState {
    val ship = Ship()
    var shots = mutableListOf<Shot>()
    var fire = mutableListOf<Fire>()
    var parts = mutableListOf<Part>()
    var enemies = mutableListOf<Enemy>()
    var t = 0f
    var score = 0
    var cool = 0f
    var manual = 0f
    var wave = 0
    var respawn = 0f
    private val rnd = Random.Default

    init { spawn() }

    fun spawn() {
        enemies = mutableListOf()
        wave += 1
        for (r in 0 until 4) for (c in 0 until 6) enemies.add(Enemy(60f + c * 54f, 78f + r * 46f, rnd.nextFloat() * 6.28f))
        respawn = 0f
    }

    fun burst(x: Float, y: Float, colors: List<Color>, n: Int, speed: Float) {
        for (i in 0 until n) {
            val a = rnd.nextFloat() * 6.28f
            val v = speed * (0.3f + rnd.nextFloat())
            parts.add(Part(x, y, cos(a) * v, sin(a) * v, 0.5f + rnd.nextFloat() * 0.4f, 0.9f, 1.5f + rnd.nextFloat() * 2.5f, colors[i % colors.size]))
        }
    }

    fun step(dt: Float) {
        val s = ship
        t += dt
        val ox = sin(t * 0.6f) * 28f
        val oy = sin(t * 1.3f) * 5f
        val alive = enemies.filter { it.alive }
        alive.forEach { it.x = it.bx + ox; it.y = it.by + oy + sin(t * 3f + it.ph) * 3f }

        if (manual > 0) manual -= dt
        else if (alive.isNotEmpty()) {
            var best = alive[0]
            alive.forEach { if (abs(it.x - s.x) < abs(best.x - s.x)) best = it }
            s.tx = best.x
            fire.forEach { f -> if (f.y > s.y - 110 && f.y < s.y && abs(f.x - s.x) < 24) s.tx = s.x + (if (f.x > s.x) -60f else 60f) }
            s.tx = max(24f, min(366f, s.tx))
        }
        val d = s.tx - s.x
        val sp = 260f * dt
        s.x += if (abs(d) < sp) d else sign(d) * sp
        if (s.hit > 0) s.hit -= dt

        cool -= dt
        if (cool <= 0 && alive.isNotEmpty()) { shots.add(Shot(s.x, s.y - 20)); cool = 0.2f }
        shots.forEach { it.y -= 540f * dt }
        shots = shots.filter { b ->
            if (b.y < -10) return@filter false
            for (e in alive) {
                if (e.alive && abs(b.x - e.x) < 15 && abs(b.y - e.y) < 18) {
                    e.alive = false; score += 100
                    burst(e.x, e.y, listOf(Color.White, Color(0xFFE9EDF2), Color(0xFF9FE0FF), Color(0xFFE23B2E), Color(0xFFFFB020)), 18, 90f)
                    return@filter false
                }
            }
            true
        }.toMutableList()

        if (alive.isNotEmpty() && rnd.nextFloat() < dt * 1.1f) {
            val e = alive[rnd.nextInt(alive.size)]
            fire.add(Fire(e.x, e.y + 18, 120f + rnd.nextFloat() * 70f, rnd.nextFloat() * 6.28f))
        }
        fire.forEach { it.y += it.vy * dt; it.x += sin(t * 6f + it.wob) * 12f * dt }
        fire = fire.filter { f ->
            if (f.y > 520) return@filter false
            if (abs(f.x - s.x) < 15 && abs(f.y - s.y) < 16) {
                s.hit = 0.6f
                burst(f.x, f.y, listOf(Color(0xFFFFB15C), Color(0xFFFF6A1F), Color(0xFFFFE3B0)), 10, 70f)
                return@filter false
            }
            true
        }.toMutableList()

        parts.forEach { it.x += it.vx * dt; it.y += it.vy * dt; it.vx *= 0.96f; it.vy *= 0.96f; it.life -= dt }
        parts = parts.filter { it.life > 0 }.toMutableList()

        if (enemies.none { it.alive }) {
            respawn += dt
            if (respawn > 1.2f) spawn()
        }
    }
}

private fun DrawScope.chicken(x: Float, y: Float, t: Float, ph: Float) {
    val flap = sin(t * 12f + ph)
    translate(x, y) {
        for (sd in listOf(-1f, 1f)) {
            translate(6f * sd, 0f) {
                rotate(Math.toDegrees((sd * (-0.35f - flap * 0.45f)).toDouble()).toFloat(), Offset.Zero) {
                    drawOval(Color(0xFFDDE3EA), Offset(8f * sd - 9f, -4f), Size(18f, 8f))
                }
            }
        }
        val leg = Color(0xFFFF9F1C)
        drawLine(leg, Offset(-3f, 13f), Offset(-3f, 18f), 1.6f, StrokeCap.Round)
        drawLine(leg, Offset(3f, 13f), Offset(3f, 18f), 1.6f, StrokeCap.Round)
        drawOval(
            Brush.radialGradient(listOf(Color.White, Color(0xFFC9D2DC)), Offset(-3f, 0f), 13f),
            Offset(-9.5f, -6.5f), Size(19f, 21f),
        )
        drawCircle(Color(0xFFF4F6F9), 7f, Offset(0f, -9f))
        val red = Color(0xFFE23B2E)
        drawCircle(red, 2.6f, Offset(-3f, -15.5f)); drawCircle(red, 2.8f, Offset(0f, -17.2f)); drawCircle(red, 2.6f, Offset(3f, -15.5f))
        drawOval(red, Offset(-1.5f, -4.8f), Size(3f, 4.4f))
        val beak = Path().apply { moveTo(-2.6f, -7.2f); lineTo(2.6f, -7.2f); lineTo(0f, -3.6f); close() }
        drawPath(beak, Color(0xFFFFB020))
        val dark = Color(0xFF141414)
        drawCircle(dark, 1.3f, Offset(-2.6f, -10f)); drawCircle(dark, 1.3f, Offset(2.6f, -10f))
        drawLine(dark, Offset(-4.6f, -13f), Offset(-1.2f, -11.8f), 1.2f)
        drawLine(dark, Offset(4.6f, -13f), Offset(1.2f, -11.8f), 1.2f)
    }
}

private fun DrawScope.ship(s: Ship) {
    translate(s.x, s.y) {
        val fl = 8f + Random.nextFloat() * 6f
        val flame = Color(0xE6FF8C3C)
        drawPath(Path().apply { moveTo(-7f, 10f); lineTo(-4f, 10f + fl); lineTo(-1f, 10f); close() }, flame)
        drawPath(Path().apply { moveTo(1f, 10f); lineTo(4f, 10f + fl); lineTo(7f, 10f); close() }, flame)
        drawPath(Path().apply { moveTo(0f, -12f); lineTo(20f, 10f); lineTo(8f, 8f); lineTo(-8f, 8f); lineTo(-20f, 10f); close() }, Color(0xFFAFC2D8))
        drawPath(Path().apply {
            moveTo(0f, -24f); quadraticTo(8f, -12f, 8f, 10f); lineTo(-8f, 10f); quadraticTo(-8f, -12f, 0f, -24f); close()
        }, Color(0xFFE8EEF6))
        drawRect(C.Orange, Offset(-8f, 2f), Size(16f, 3f))
        drawOval(C.Cyan, Offset(-3.2f, -16f), Size(6.4f, 12f))
        if (s.hit > 0) drawCircle(C.Cyan.copy(alpha = s.hit / 0.6f), 30f, Offset(0f, -4f), style = Stroke(2f))
    }
}

private fun DrawScope.render(g: GameState) {
    g.enemies.forEach { if (it.alive) chicken(it.x, it.y, g.t, it.ph) }
    g.fire.forEach { f ->
        drawLine(
            Brush.verticalGradient(listOf(Color(0x00FF5A1E), Color(0xBFFF963C)), f.y - 22f, f.y),
            Offset(f.x, f.y - 22f), Offset(f.x, f.y), 4f, StrokeCap.Round,
        )
        drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFFFD27A), Color(0x00FF5A1E)), Offset(f.x, f.y), 7f), 7f, Offset(f.x, f.y))
    }
    g.shots.forEach { b ->
        drawOval(C.Cyan.copy(.35f), Offset(b.x - 5f, b.y - 9f), Size(10f, 18f))
        drawOval(Color(0xFFBFEFFF), Offset(b.x - 2.2f, b.y - 6f), Size(4.4f, 12f))
    }
    g.parts.forEach { p -> drawCircle(p.col.copy(alpha = max(0f, p.life / p.max)), p.r, Offset(p.x, p.y)) }
    ship(g.ship)
}

@Composable
fun MiniGame(modifier: Modifier = Modifier) {
    val game = remember { GameState() }
    var frame by remember { mutableLongStateOf(0L) }
    var boxW by remember { mutableFloatStateOf(1f) }
    var boxH by remember { mutableFloatStateOf(1f) }
    // uniform scale that fits the 390x500 game space in the available box, centred horizontally
    fun sc() = min(boxW / 390f, boxH / 500f)
    fun ox() = (boxW - 390f * sc()) / 2f

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) game.step(min(0.05f, (now - last) / 1e9f))
                last = now
                frame = now
            }
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .onSizeChanged { boxW = it.width.toFloat(); boxH = it.height.toFloat() }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { o ->
                    game.ship.tx = ((o.x - ox()) / sc()).coerceIn(24f, 366f); game.manual = 2.5f
                }) { change, _ ->
                    game.ship.tx = ((change.position.x - ox()) / sc()).coerceIn(24f, 366f); game.manual = 2.5f
                }
            },
    ) {
        Canvas(Modifier.matchParentSize()) {
            frame // read to invalidate every frame
            val k = min(size.width / 390f, size.height / 500f)
            translate((size.width - 390f * k) / 2f, 0f) { scale(k, k, Offset.Zero) { render(game) } }
        }
        Text("HITS ${(game.score / 100).toString().padStart(3, '0')}", style = mono(11f, C.Muted, 0.04f), modifier = Modifier.align(Alignment.TopStart).run { this }.padding20())
        Text("SCORE ${game.score.toString().padStart(6, '0')}", style = mono(11f, Color(0xFFCFE6FF), 0.04f), modifier = Modifier.align(Alignment.TopEnd).padding20())
    }
}

private fun Modifier.padding20() = this.then(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp))
