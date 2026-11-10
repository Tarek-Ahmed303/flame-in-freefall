package com.flame.astronaut.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter

/** 24x24 stroke icons from the design (1.8 stroke, round caps), built from the inline SVG paths. */
private fun icon(name: String, stroke: Float = 1.8f, fill: Boolean = false, paths: Array<String>): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach { d ->
            addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = if (fill) SolidColor(Color.Black) else null,
                stroke = if (fill) null else SolidColor(Color.Black),
                strokeLineWidth = stroke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

private fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r},$y h${w - 2 * r} a$r,$r 0 0 1 $r,$r v${h - 2 * r} a$r,$r 0 0 1 -$r,$r h${-(w - 2 * r)} a$r,$r 0 0 1 -$r,-$r v${-(h - 2 * r)} a$r,$r 0 0 1 $r,-$r z"

object Ic {
    val Info = icon("info", paths = arrayOf(circle(12f, 12f, 9f), "M12 11v6M12 7.5v.5"))
    val Clock = icon("clock", paths = arrayOf(circle(12f, 12f, 9f), "M12 7v5l3 2"))
    val Power = icon("power", paths = arrayOf("M12 3v8", "M6.5 6.5a8 8 0 1 0 11 0"))
    val Thermo = icon("thermo", paths = arrayOf("M10 14V5a2 2 0 0 1 4 0v9a4 4 0 1 1-4 0z"))
    val Box = icon("box", paths = arrayOf("M3 7l9-4 9 4v10l-9 4-9-4z", "M3 7l9 4 9-4", "M12 11v10"))
    val Door = icon("door", paths = arrayOf(rrect(6f, 3f, 12f, 18f, 1f), "M14 12v.5"))
    val Mask = icon("mask", paths = arrayOf("M4 10c0-2 3-3 8-3s8 1 8 3v3c0 3-3 5-8 5s-8-2-8-5z", "M9 13h6"))
    val Extinguisher = icon("extinguisher", paths = arrayOf(rrect(8f, 9f, 8f, 12f, 2f), "M12 9V5l3-2", "M15 5h3"))
    val Gauge = icon("gauge", paths = arrayOf(circle(12f, 12f, 9f), "M12 12l3-4"))
    val Smoke = icon("smoke", paths = arrayOf("M7 18a4 4 0 0 1 0-8 5 5 0 0 1 9.5-1A4.5 4.5 0 0 1 17 18z"))
    val Home = icon("home", paths = arrayOf("M4 11l8-7 8 7v9a1 1 0 0 1-1 1h-4v-6H9v6H5a1 1 0 0 1-1-1z"))
    val Chat = icon("chat", paths = arrayOf("M4 5h16v11H10l-5 4v-4H4z"))
    val Guide = icon("guide", paths = arrayOf("M12 3l8 3v6c0 4.5-3.2 7.8-8 9-4.8-1.2-8-4.5-8-9V6z", "M8.5 12l2.5 2.5 4.5-5"))
    val Settings = icon(
        "settings",
        paths = arrayOf(
            "M4 7h9M17 7h3M4 12h3M11 12h9M4 17h11M19 17h1",
            circle(15f, 7f, 2f), circle(9f, 12f, 2f), circle(17f, 17f, 2f),
        ),
    )
    val Bell = icon("bell", paths = arrayOf("M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z", "M10 20a2 2 0 0 0 4 0"))
    val Mic = icon("mic", stroke = 2f, paths = arrayOf(rrect(9f, 3f, 6f, 11f, 3f), "M5 11a7 7 0 0 0 14 0M12 18v3"))
    val Send = icon("send", paths = arrayOf("M4 12l16-8-6 16-2-6z"))
    val Back = icon("back", stroke = 2f, paths = arrayOf("M15 6l-6 6 6 6"))
    val ChevronRight = icon("chevR", stroke = 2f, paths = arrayOf("M9 6l6 6-6 6"))
    val Speaker = icon("speaker", stroke = 1.9f, paths = arrayOf("M4 9h4l5-4v14l-5-4H4z", "M16 9a4 4 0 0 1 0 6M19 6a8 8 0 0 1 0 12"))
    val SpeakerOff = icon("speakerOff", stroke = 2f, paths = arrayOf("M4 9h4l5-4v14l-5-4H4z", "M17 9l4 6M21 9l-4 6"))
    val Crew = icon("crew", stroke = 2f, paths = arrayOf(circle(9f, 8f, 3f), "M3 20c.8-3 3-5 6-5s5.2 2 6 5", "M16 5a3 3 0 0 1 0 6M18 15c1.6.6 2.6 2.4 3 5"))
    val Flame = icon("flame", fill = true, paths = arrayOf("M12 3c1 4 5 5 5 10a5 5 0 0 1-10 0c0-3 2-4 2-7 1.5 1 2 2 2 3 1-2 1-4 1-6z"))
    val FlameOutline = icon("flameO", paths = arrayOf("M12 3c1 4 5 5 5 10a5 5 0 0 1-10 0c0-3 2-4 2-7 1.5 1 2 2 2 3 1-2 1-4 1-6z"))
    val Vibrate = icon("vibrate", paths = arrayOf(rrect(8f, 3f, 8f, 18f, 2f), "M4 8v8M20 8v8M1.5 10.5v3M22.5 10.5v3"))
    val Check = icon("check", stroke = 2.6f, paths = arrayOf("M5 12l5 5 9-10"))
    val Mail = icon("mail", paths = arrayOf(rrect(3f, 5f, 18f, 14f, 2f), "M3 7l9 6 9-6"))
    val Lock = icon("lock", paths = arrayOf(rrect(5f, 11f, 14f, 9f, 2f), "M8 11V8a4 4 0 0 1 8 0v3"))
    val Eye = icon("eye", paths = arrayOf("M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z", circle(12f, 12f, 3f)))
    val User = icon("user", paths = arrayOf(circle(12f, 8f, 4f), "M4 21c1-4 4-6 8-6s7 2 8 6"))
    val Fingerprint = icon(
        "finger",
        paths = arrayOf(
            "M6 10a6 6 0 0 1 12 0v2", "M9 21c-.5-2-1-4-1-7a4 4 0 0 1 8 0c0 2 .3 3.5 1 5",
            "M12 14c0 3 .3 5 1.2 7", "M4 14c0 3 .4 5 1.2 7", "M19 14c0 2 .3 3.5 1 5",
        ),
    )
    val Search = icon("search", paths = arrayOf(circle(11f, 11f, 7f), "M20 20l-4-4"))
    val Play = icon("play", fill = true, paths = arrayOf("M7 4l13 8-13 8z"))
    val Edit = icon("edit", paths = arrayOf("M4 20h4L19 9l-4-4L4 16z", "M13 7l4 4"))
    val SignOut = icon("signout", stroke = 2f, paths = arrayOf("M10 5H5v14h5", "M9 12h11M9 12l4-4M9 12l4 4"))
    val ArrowRight = icon("arrowR", stroke = 2f, paths = arrayOf("M5 12h14M13 6l6 6-6 6"))
    val Warning = icon("warn", stroke = 2f, paths = arrayOf("M12 3l10 18H2z", "M12 10v5M12 18v.5"))
    val Wind = icon("wind", paths = arrayOf("M3 8h10a3 3 0 1 0-3-3", "M3 12h15a3 3 0 1 1-3 3", "M3 16h7a2 2 0 1 1-2 2"))
    val Sheet = icon("sheet", paths = arrayOf("M5 19L19 5", "M7 5l12 12", "M5 5l14 14"))
    val ChevronUp = icon("chevU", stroke = 2f, paths = arrayOf("M6 15l6-6 6 6"))
    val Plus = icon("plus", stroke = 2f, paths = arrayOf("M12 5v14M5 12h14"))
}

@Composable
fun FIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = null,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

