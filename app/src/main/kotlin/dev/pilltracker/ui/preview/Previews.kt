package dev.pilltracker.ui.preview

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import dev.pilltracker.domain.rules.monthLayout
import dev.pilltracker.ui.components.DayCell
import dev.pilltracker.ui.components.DayStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** Light/dark × en/es for every component state and screen. */
@Preview(name = "en light", locale = "en", showBackground = true)
@Preview(name = "en dark", locale = "en", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "es light", locale = "es", showBackground = true)
@Preview(name = "es dark", locale = "es", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
annotation class PillPreviews

// Static sample data for previews and UI tests only.
object Sample {
    val time: LocalTime = LocalTime.of(9, 0)
    val today: LocalDate = LocalDate.of(2026, 10, 6)
    val month: YearMonth = YearMonth.from(today)
    private val missed = setOf(2, 4)

    fun weeks(firstDayOfWeek: DayOfWeek): List<List<DayCell?>> =
        monthLayout(month, firstDayOfWeek).map { week ->
            week.map { date ->
                date?.let {
                    val status =
                        when {
                            it > today -> DayStatus.UPCOMING
                            it.dayOfMonth in missed -> DayStatus.NOT_TAKEN
                            else -> DayStatus.TAKEN
                        }
                    DayCell(it, status, isToday = it == today)
                }
            }
        }
}
