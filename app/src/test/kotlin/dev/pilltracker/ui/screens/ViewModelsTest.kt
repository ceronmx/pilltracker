package dev.pilltracker.ui.screens

import dev.pilltracker.domain.IntakeRepository
import dev.pilltracker.ui.components.DayStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.DayOfWeek.MONDAY
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

class InMemoryIntakeRepository : IntakeRepository {
    val intakes = MutableStateFlow(mapOf<LocalDate, Instant>())

    override fun takenDays(
        from: LocalDate,
        to: LocalDate,
    ): Flow<Set<LocalDate>> = intakes.map { all -> all.keys.filter { it in from..to }.toSet() }

    override suspend fun markTaken(
        day: LocalDate,
        at: Instant,
    ) = intakes.update { if (day in it) it else it + (day to at) }
}

/** A clock tests can move forward or across zones. */
class MutableClock(var instant: Instant, var zoneId: ZoneId) : Clock() {
    override fun getZone() = zoneId

    override fun instant() = instant

    override fun withZone(zone: ZoneId) = MutableClock(instant, zone)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val repo = InMemoryIntakeRepository()

    private fun clockAt(
        date: LocalDate,
        time: LocalTime,
        zone: ZoneId = madrid,
    ) = MutableClock(ZonedDateTime.of(date, time, zone).toInstant(), zone)

    @BeforeEach fun mainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun resetMain() = Dispatchers.resetMain()

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
    fun takenStoresPillDayAndInstantAndTwiceIsANoOp() {
        val clock = clockAt(LocalDate.of(2026, 10, 6), LocalTime.of(8, 0))
        val vm = HomeViewModel(clock, repo)
        vm.onEvent(HomeUiEvent.Taken)
        val first = repo.intakes.value
        clock.instant = clock.instant.plusSeconds(60)
        vm.onEvent(HomeUiEvent.Taken)
        assertTrue(vm.state.value.taken)
        assertEquals(first, repo.intakes.value)
        assertEquals(mapOf(LocalDate.of(2026, 10, 6) to first.values.single()), repo.intakes.value)
    }

    @Test
    fun homeRollsOverAtMidnightOnResume() {
        val clock = clockAt(LocalDate.of(2026, 10, 5), LocalTime.of(23, 59))
        val vm = HomeViewModel(clock, repo)
        vm.onEvent(HomeUiEvent.Taken)
        assertEquals(LocalDate.of(2026, 10, 5), vm.state.value.today)

        clock.instant = clock.instant.plusSeconds(60) // 00:00 Oct 6 Madrid
        vm.onEvent(HomeUiEvent.Resumed)
        assertEquals(LocalDate.of(2026, 10, 6), vm.state.value.today)
        assertFalse(vm.state.value.taken)
    }

    @Test
    fun takenLogsTheCurrentDayEvenWithoutResume() {
        val clock = clockAt(LocalDate.of(2026, 10, 5), LocalTime.of(23, 59))
        val vm = HomeViewModel(clock, repo)
        clock.instant = clock.instant.plusSeconds(120)
        vm.onEvent(HomeUiEvent.Taken)
        assertEquals(setOf(LocalDate.of(2026, 10, 6)), repo.intakes.value.keys)
    }

    @Test
    fun todayFollowsTravelZoneChange() {
        // Same instant: already Oct 6 in Madrid, still Oct 5 in New York.
        val clock = clockAt(LocalDate.of(2026, 10, 6), LocalTime.of(2, 0))
        val vm = HomeViewModel(clock, repo)
        clock.zoneId = ZoneId.of("America/New_York")
        vm.onEvent(HomeUiEvent.Resumed)
        assertEquals(LocalDate.of(2026, 10, 5), vm.state.value.today)
    }

    @Test
    fun todayOnDstChangeDay() {
        // Europe falls back on 2026-10-25 at 03:00 -> 02:00.
        val vm = HomeViewModel(clockAt(LocalDate.of(2026, 10, 25), LocalTime.of(2, 30)), repo)
        assertEquals(LocalDate.of(2026, 10, 25), vm.state.value.today)
    }

    @Test
    fun historyShowsTakenNotTakenUpcomingAndNavigatesMonths() {
        repo.intakes.value = mapOf(LocalDate.of(2026, 10, 3) to Instant.EPOCH)
        val vm = HistoryViewModel(clockAt(LocalDate.of(2026, 10, 6), LocalTime.NOON), MONDAY, repo)
        val cells = vm.state.value.weeks.flatten().filterNotNull()
        assertEquals(YearMonth.of(2026, 10), vm.state.value.month)
        assertEquals(LocalDate.of(2026, 10, 6), cells.single { it.isToday }.date)
        assertEquals(DayStatus.TAKEN, cells.first { it.date.dayOfMonth == 3 }.status)
        assertEquals(DayStatus.NOT_TAKEN, cells.first { it.date.dayOfMonth == 5 }.status)
        assertEquals(DayStatus.UPCOMING, cells.first { it.date.dayOfMonth == 7 }.status)

        vm.onEvent(HistoryUiEvent.Previous)
        assertEquals(YearMonth.of(2026, 9), vm.state.value.month)
        assertFalse(vm.state.value.weeks.flatten().filterNotNull().any { it.isToday })
        vm.onEvent(HistoryUiEvent.Next)
        vm.onEvent(HistoryUiEvent.Next)
        assertEquals(YearMonth.of(2026, 11), vm.state.value.month)
    }

    @Test
    fun historyUpdatesWhenAPillIsLogged() {
        val vm = HistoryViewModel(clockAt(LocalDate.of(2026, 10, 6), LocalTime.NOON), MONDAY, repo)
        repo.intakes.value = mapOf(LocalDate.of(2026, 10, 6) to Instant.EPOCH)
        assertEquals(DayStatus.TAKEN, vm.state.value.weeks.flatten().filterNotNull().single { it.isToday }.status)
    }
}
