package dev.pilltracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.pilltracker.R
import dev.pilltracker.ui.preview.PillPreviews
import dev.pilltracker.ui.preview.Sample
import dev.pilltracker.ui.theme.PillTrackerTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/** BREAK = a pill-free day of a 21-pill pack; never shown as "not taken" (AC4.1). */
enum class DayStatus { TAKEN, NOT_TAKEN, UPCOMING, BREAK }

data class DayCell(
    val date: LocalDate,
    val status: DayStatus,
    val isToday: Boolean,
    val isPlacebo: Boolean = false,
)

/** Month header with prev/next plus a 7-column grid; null cells are days outside the month. */
@Composable
fun MonthGrid(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
    weeks: List<List<DayCell?>>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val title = DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(month).replaceFirstChar { it.titlecase(locale) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onPrevious) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_month))
            }
            Text(
                title,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            IconButton(onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_month))
            }
        }
        Row {
            for (i in DayOfWeek.values().indices) {
                Text(
                    firstDayOfWeek.plus(i.toLong()).getDisplayName(TextStyle.NARROW, locale),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        weeks.forEach { week ->
            Row {
                week.forEach { cell ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp)) { cell?.let { Day(it) } }
                }
            }
        }
    }
}

@Composable
private fun Day(cell: DayCell) {
    val colors = MaterialTheme.colorScheme
    val dateLabel =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            .withLocale(LocalConfiguration.current.locales[0])
            .format(cell.date)
            .let { if (cell.isPlacebo) stringResource(R.string.day_placebo, it) else it }
    val description =
        when (cell.status) {
            DayStatus.TAKEN -> stringResource(R.string.day_taken, dateLabel)
            DayStatus.NOT_TAKEN -> stringResource(R.string.day_not_taken, dateLabel)
            DayStatus.UPCOMING -> dateLabel
            DayStatus.BREAK -> stringResource(R.string.day_break, dateLabel)
        }
    val (background, content) =
        when (cell.status) {
            DayStatus.TAKEN -> colors.primary to colors.onPrimary
            DayStatus.NOT_TAKEN -> colors.surfaceVariant to colors.onSurfaceVariant
            DayStatus.UPCOMING -> Color.Transparent to colors.onSurface
            DayStatus.BREAK -> Color.Transparent to colors.outline
        }
    Box(
        Modifier
            .clip(CircleShape)
            .background(background)
            .then(if (cell.isToday) Modifier.border(BorderStroke(2.dp, colors.onSurface), CircleShape) else Modifier)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(cell.date.dayOfMonth.toString(), color = content, style = MaterialTheme.typography.bodyMedium)
        // Placebo marker: a small dot under the number.
        if (cell.isPlacebo) {
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(4.dp).clip(CircleShape)
                    .background(content),
            )
        }
    }
}

@PillPreviews
@Composable
private fun MonthGridPreview() =
    PillTrackerTheme {
        val first = WeekFields.of(LocalConfiguration.current.locales[0]).firstDayOfWeek
        MonthGrid(Sample.month, first, Sample.weeks(first), {}, {})
    }
