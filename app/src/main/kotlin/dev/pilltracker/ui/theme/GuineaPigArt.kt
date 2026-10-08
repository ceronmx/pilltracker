package dev.pilltracker.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp
import dev.pilltracker.domain.GuineaPig

/** The chosen guinea pig (AC7.5), provided by [PillTrackerTheme]. */
val LocalGuineaPig = staticCompositionLocalOf { GuineaPig.TRICOLOR }

private class Coat(
    val body: Color,
    val patch: Color,
    val ear: Color,
    val eye: Color,
    val nose: Color = Color(0xFF3B2414),
    val feet: Color = Color(0xFFE8A3A0),
)

private val coats =
    mapOf(
        GuineaPig.TRICOLOR to Coat(Color(0xFFFFF3DF), Color(0xFFA0622D), Color(0xFF7A4520), Color(0xFF3B2414)),
        GuineaPig.GINGER to Coat(Color(0xFFF2C08A), Color(0xFFC46F25), Color(0xFF9A531A), Color(0xFF3B2414)),
        GuineaPig.BLACK_WHITE to Coat(Color(0xFFFAF7F2), Color(0xFF3A3330), Color(0xFF231E1C), Color(0xFF000000)),
        GuineaPig.CREAM to
            Coat(
                Color(0xFFF6E7D0),
                Color(0xFFE6CBA3),
                Color(0xFFE7B3A6),
                Color(0xFFB23A48),
                nose = Color(0xFFC98A80),
            ),
    )

// Same shapes as res/drawable/guinea_pig.xml (the launcher icon, always tricolor).
private const val FOOT_LEFT = "M16,42a4,2.5 0 1,0 8,0a4,2.5 0 1,0 -8,0z"
private const val FOOT_RIGHT = "M42,42a4,2.5 0 1,0 8,0a4,2.5 0 1,0 -8,0z"
private const val BODY = "M8,26a26,16 0 1,0 52,0a26,16 0 1,0 -52,0z"
private const val REAR_PATCH = "M40,16a17,17 0 1,0 34,0a17,17 0 1,0 -34,0z"
private const val EAR = "M17,13a6,5 0 1,0 12,0a6,5 0 1,0 -12,0z"
private const val HEAD = "M3,24a13,13 0 1,0 26,0a13,13 0 1,0 -26,0z"
private const val BLAZE = "M1.5,27a3.5,9 0 1,0 7,0a3.5,9 0 1,0 -7,0z"
private const val EYE = "M11.8,21a2.2,2.2 0 1,0 4.4,0a2.2,2.2 0 1,0 -4.4,0z"
private const val GLINT = "M14,20.3a0.7,0.7 0 1,0 1.4,0a0.7,0.7 0 1,0 -1.4,0z"
private const val NOSE = "M3,28.5a1.3,1.1 0 1,0 2.6,0a1.3,1.1 0 1,0 -2.6,0z"
private const val CANVAS = 64f
private const val DROP = 9f

private fun art(pig: GuineaPig): ImageVector {
    val c = coats.getValue(pig)
    val b =
        ImageVector.Builder(
            defaultWidth = 64.dp,
            defaultHeight = 64.dp,
            viewportWidth = CANVAS,
            viewportHeight = CANVAS,
        )

    fun fill(
        color: Color,
        path: String,
    ) = b.addPath(addPathNodes(path), fill = SolidColor(color))
    b.group(translationY = DROP) {
        fill(c.feet, FOOT_LEFT)
        fill(c.feet, FOOT_RIGHT)
        fill(c.body, BODY)
        group(clipPathData = addPathNodes(BODY)) { fill(c.patch, REAR_PATCH) }
        fill(c.ear, EAR)
        fill(c.patch, HEAD)
        group(clipPathData = addPathNodes(HEAD)) { fill(c.body, BLAZE) }
        fill(c.eye, EYE)
        fill(Color.White, GLINT)
        fill(c.nose, NOSE)
    }
    return b.build()
}

/** Decorative guinea pig in the chosen coat, or [pig] when shown as an option. No semantics unless [description]. */
@Composable
fun GuineaPigImage(
    modifier: Modifier = Modifier,
    pig: GuineaPig = LocalGuineaPig.current,
    description: String? = null,
) {
    val vector = remember(pig) { art(pig) }
    Image(vector, description, modifier)
}
