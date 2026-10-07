package dev.pilltracker.ui.screens

import dev.pilltracker.ui.components.DayStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

class ViewModelsTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun clockAt(
        date: LocalDate,
        time: LocalTime,
        zone: ZoneId = madrid,
    ) = Clock.fixed(ZonedDateTime.of(date, time, zone).toInstant(), zone)

    @Test
    fun setupStartsAtNineAndPickerUpdatesTime() {
        val vm = SetupViewModel()
        assertEquals(LocalTime.of(9, 0), vm.state.value.time)
        vm.onEvent(SetupUiEvent.TimeClicked)
        assertTrue(vm.state.value.showTimePicker)
        vm.onEvent(SetupUiEvent.TimePicked(LocalTime.of(21, 30)))
        assertEquals(LocalTime.of(21, 30), vm.state.value.time)
        assertFalse(vm.state.value.showTimePicker)
    }

    @Test
    fun takenTwiceIsANoOp() {
        val vm = HomeViewModel(clockAt(LocalDate.of(2026, 10, 6), LocalTime.NOON))
        vm.onEvent(HomeUiEvent.Taken)
        vm.onEvent(HomeUiEvent.Taken)
        assertTrue(vm.state.value.taken)
    }

    @Test
    fun todayFollowsZoneNotUtcAtDayBoundary() {
        // 23:59 Oct 5 in Madrid is already Oct 5 21:59 UTC; at 00:00 Madrid it's Oct 6 locally.
        assertEquals(
            LocalDate.of(2026, 10, 5),
            HomeViewModel(clockAt(LocalDate.of(2026, 10, 5), LocalTime.of(23, 59))).state.value.today,
        )
        assertEquals(
            LocalDate.of(2026, 10, 6),
            HomeViewModel(clockAt(LocalDate.of(2026, 10, 6), LocalTime.MIDNIGHT)).state.value.today,
        )
    }

    @Test
    fun todayFollowsTravelZoneChange() {
        // Same instant: still Oct 5 in New York, already Oct 6 in Madrid.
        val instant = ZonedDateTime.of(LocalDate.of(2026, 10, 6), LocalTime.of(2, 0), madrid).toInstant()
        val ny = ZoneId.of("America/New_York")
        assertEquals(LocalDate.of(2026, 10, 5), HomeViewModel(Clock.fixed(instant, ny)).state.value.today)
    }

    @Test
    fun todayOnDstChangeDay() {
        // Europe falls back on 2026-10-25 at 03:00 -> 02:00.
        assertEquals(
            LocalDate.of(2026, 10, 25),
            HomeViewModel(clockAt(LocalDate.of(2026, 10, 25), LocalTime.of(2, 30))).state.value.today,
        )
    }

    @Test
    fun historyMarksTodayAndUpcomingAndNavigatesMonths() {
        val vm = HistoryViewModel(clockAt(LocalDate.of(2026, 10, 6), LocalTime.NOON), MONDAY)
        val cells = vm.state.value.weeks.flatten().filterNotNull()
        assertEquals(YearMonth.of(2026, 10), vm.state.value.month)
        assertTrue(cells.single { it.isToday }.date == LocalDate.of(2026, 10, 6))
        assertEquals(DayStatus.UPCOMING, cells.first { it.date.dayOfMonth == 7 }.status)
        assertEquals(DayStatus.NOT_TAKEN, cells.first { it.date.dayOfMonth == 5 }.status)

        vm.onEvent(HistoryUiEvent.Previous)
        assertEquals(YearMonth.of(2026, 9), vm.state.value.month)
        assertFalse(vm.state.value.weeks.flatten().filterNotNull().any { it.isToday })
        vm.onEvent(HistoryUiEvent.Next)
        vm.onEvent(HistoryUiEvent.Next)
        assertEquals(YearMonth.of(2026, 11), vm.state.value.month)
    }
}
