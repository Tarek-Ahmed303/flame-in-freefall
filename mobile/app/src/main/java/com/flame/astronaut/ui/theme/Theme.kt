package com.flame.astronaut.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.flame.astronaut.R

object C {
    val BgSplash = Color(0xFF02040A)
    val Bg = Color(0xFF04060B)
    val BgEmergency = Color(0xFF06050A)
    val Surface = Color(0xFF0B1018)
    val Surface2 = Color(0xFF0F1520)
    val Surface3 = Color(0xFF111826)
    val Surface4 = Color(0xFF161C27)
    val Text = Color(0xFFE8EEF6)
    val Text2 = Color(0xFFB7C2D2)
    val Muted = Color(0xFF8B98AB)
    val Faint = Color(0xFF6F7C90)
    val Orange = Color(0xFFFF7A2F)
    val OrangeLight = Color(0xFFFF9A5C)
    val OnOrange = Color(0xFF1A0800)
    val Cyan = Color(0xFF5BC8F5)
    val Red = Color(0xFFFF4538)
    val RedText = Color(0xFFFF8A7F)
    val RedText2 = Color(0xFFFF7B6E)
    val Green = Color(0xFF3DDC97)
    val Amber = Color(0xFFFFC042)
    val Stroke = Color(0x2496B4DC) // rgba(150,180,220,.14)
    val StrokeStrong = Color(0x3896B4DC)
}

val ChakraPetch = FontFamily(
    Font(R.font.chakra_petch_medium, FontWeight.Medium),
    Font(R.font.chakra_petch_semibold, FontWeight.SemiBold),
    Font(R.font.chakra_petch_bold, FontWeight.Bold),
)

val PlexMono = FontFamily(
    Font(R.font.plex_mono_regular, FontWeight.Normal),
    Font(R.font.plex_mono_medium, FontWeight.Medium),
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val PlexSans = FontFamily(
    listOf(400, 500, 600).map { w ->
        Font(
            R.font.plex_sans,
            FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    }
)

/** Letter spacing is given in em like in the CSS. */
fun mono(size: Float, color: Color = C.Muted, ls: Float = 0.1f, w: FontWeight = FontWeight.Medium) =
    TextStyle(fontFamily = PlexMono, fontSize = size.sp, fontWeight = w, color = color, letterSpacing = (ls * size).sp)

fun chakra(size: Float, color: Color = C.Text, w: FontWeight = FontWeight.SemiBold, ls: Float = 0f) =
    TextStyle(fontFamily = ChakraPetch, fontSize = size.sp, fontWeight = w, color = color, letterSpacing = (ls * size).sp)

fun sans(size: Float, color: Color = C.Text, w: FontWeight = FontWeight.Normal, lh: Float = 1.4f) =
    TextStyle(fontFamily = PlexSans, fontSize = size.sp, fontWeight = w, color = color, lineHeight = (size * lh).sp)

@Composable
fun FlameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.Orange,
            onPrimary = C.OnOrange,
            background = C.Bg,
            surface = C.Surface,
            onBackground = C.Text,
            onSurface = C.Text,
        ),
        content = content,
    )
}
