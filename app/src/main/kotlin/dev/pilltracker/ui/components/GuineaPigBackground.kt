package dev.pilltracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.pilltracker.R

private const val PATTERN_ALPHA = 0.06f
private const val TILT_DEGREES = 12f

/** Faint, staggered guinea pig silhouettes behind [content]; decorative, so no semantics. */
@Composable
fun GuineaPigBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val pig = painterResource(R.drawable.ic_notification)
    val tint = ColorFilter.tint(MaterialTheme.colorScheme.primary.copy(alpha = PATTERN_ALPHA))
    Box(
        modifier.drawBehind {
            val size = 40.dp.toPx()
            val stepX = 120.dp.toPx()
            val stepY = 104.dp.toPx()
            var row = 0
            var y = 0f
            while (y < this.size.height) {
                // Odd rows shift half a step and tilt the other way, so it reads as scattered, not a grid.
                var x = if (row % 2 == 0) 0f else stepX / 2
                val tilt = if (row % 2 == 0) -TILT_DEGREES else TILT_DEGREES
                while (x < this.size.width) {
                    translate(x, y) {
                        rotate(tilt, Offset(size / 2, size / 2)) {
                            with(pig) { draw(Size(size, size), colorFilter = tint) }
                        }
                    }
                    x += stepX
                }
                y += stepY
                row++
            }
        },
        content = content,
    )
}
