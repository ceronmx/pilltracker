package dev.pilltracker.ui.screens

import dev.pilltracker.domain.GuineaPig
import dev.pilltracker.domain.IntakeRepository
import dev.pilltracker.domain.ReminderScheduler
import dev.pilltracker.domain.Settings
import dev.pilltracker.domain.SettingsRepository
import dev.pilltracker.domain.rules.DayKind
import dev.pilltracker.domain.rules.PackType
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

    override suspend fun markNotTaken(day: LocalDate) = intakes.update { it - day }
}

class InMemorySettingsRepository(initial: Settings? = null) : SettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun save(settings: Settings) {
        this.settings.value = settings
    }

    override suspend fun deleteAll() {
        settings.value = null
    }
}

class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<LocalTime>()

    var cancelled = false

    override fun scheduleNext(time: LocalTime) {
        scheduled += time
    }

    override fun cancel() {
        cancelled = true
    }
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
    private val settings = InMemorySettingsRepository()
    private val scheduler = FakeReminderScheduler()
    private val oct6 = LocalDate.of(2026, 10, 6)

    private fun clockAt(
        date: LocalDate,
        time: LocalTime,
        zone: ZoneId = madrid,
    ) = MutableClock(ZonedDateTime.of(date, time, zone).toInstant(), zone)

    @BeforeEach fun mainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun resetMain() = Dispatchers.resetMain()

    @Test
    fun setupDefaultsAndPickerUpdatesTime() {
        val vm = SetupViewModel(clockAt(oct6, LocalTime.NOON), settings, scheduler)
        assertEquals(SetupUiState(), vm.state.value)
        assertEquals(PackType.WITH_PLACEBO, vm.state.value.packType)
        assertEquals(1, vm.state.value.pillNumber)
        assertEquals(LocalTime.of(9, 0), vm.state.value.time)
        vm.onEvent(SetupUiEvent.TimeClicked)
        assertTrue(vm.state.value.showTimePicker)
        vm.onEvent(SetupUiEvent.TimePicked(LocalTime.of(21, 30)))
        assertEquals(LocalTime.of(21, 30), vm.state.value.time)
        assertFalse(vm.state.value.showTimePicker)
    }

    @Test
    fun deniedNotificationsShowTheNotice() {
        val vm = SetupViewModel(clockAt(oct6, LocalTime.NOON), settings, scheduler)
        vm.onEvent(SetupUiEvent.NotificationsDenied)
        assertTrue(vm.state.value.permissionDenied)
    }

    @Test
    fun pillNumberStaysWithinThePack() {
        val vm = SetupViewModel(clockAt(oct6, LocalTime.NOON), settings, scheduler)
        vm.onEvent(SetupUiEvent.PillNumberChanged(0))
        assertEquals(1, vm.state.value.pillNumber)
        vm.onEvent(SetupUiEvent.PillNumberChanged(29))
        assertEquals(28, vm.state.value.pillNumber)
        vm.onEvent(SetupUiEvent.PackTypeChosen(PackType.WITH_BREAK))
        assertEquals(28, vm.state.value.pillNumber) // 21 pills also counts 1-28: 22-28 is the break week
    }

    @Test
    fun setupDuringTheBreakWeekShowsABreakDayAndSavesThePackStart() {
        val vm = SetupViewModel(clockAt(oct6, LocalTime.NOON), settings, scheduler)
        vm.onEvent(SetupUiEvent.PillNumberChanged(23))
        assertEquals(DayKind.PLACEBO, vm.state.value.dayKind) // 21 + 7 pack: day 23 is a placebo pill
        vm.onEvent(SetupUiEvent.PackTypeChosen(PackType.WITH_BREAK))
        assertEquals(DayKind.BREAK, vm.state.value.dayKind)
        vm.onEvent(SetupUiEvent.PillNumberChanged(21))
        assertEquals(DayKind.ACTIVE, vm.state.value.dayKind)
        vm.onEvent(SetupUiEvent.PillNumberChanged(23))
        vm.onEvent(SetupUiEvent.Save)
        assertEquals(oct6.minusDays(22), settings.settings.value?.packStart)
    }

    @Test
    fun saveStoresPackStartFromTodaysPill() {
        val vm = SetupViewModel(clockAt(oct6, LocalTime.NOON), settings, scheduler)
        vm.onEvent(SetupUiEvent.PackTypeChosen(PackType.WITH_BREAK))
        vm.onEvent(SetupUiEvent.PillNumberChanged(6))
        vm.onEvent(SetupUiEvent.TimePicked(LocalTime.of(22, 0)))
        assertFalse(vm.state.value.saved)
        vm.onEvent(SetupUiEvent.Save)
        assertTrue(vm.state.value.saved) // only now may the screen leave, or leaving cancels the write (u0i)
        assertEquals(
            Settings(PackType.WITH_BREAK, LocalDate.of(2026, 10, 1), LocalTime.of(22, 0)),
            settings.settings.value,
        )
        assertEquals(listOf(LocalTime.of(22, 0)), scheduler.scheduled)
    }

    @Test
    fun homeShowsDayKindFromPack() {
        settings.settings.value = Settings(PackType.WITH_BREAK, LocalDate.of(2026, 10, 1), LocalTime.NOON)
        val clock = clockAt(LocalDate.of(2026, 10, 21), LocalTime.NOON) // pill 21
        val vm = HomeViewModel(clock, MONDAY, repo, settings)
        assertEquals(DayKind.ACTIVE, vm.state.value.dayKind)
        clock.instant = clock.instant.plusSeconds(24 * 3600) // day 22
        vm.onEvent(HomeUiEvent.Resumed)
        assertEquals(DayKind.BREAK, vm.state.value.dayKind)
        settings.settings.value = settings.settings.value?.copy(packType = PackType.WITH_PLACEBO)
        assertEquals(DayKind.PLACEBO, vm.state.value.dayKind)
    }

    @Test
    fun calendarMarksBreakAndPlaceboDays() {
        settings.settings.value = Settings(PackType.WITH_BREAK, LocalDate.of(2026, 10, 1), LocalTime.NOON)
        val vm = HomeViewModel(clockAt(LocalDate.of(2026, 10, 25), LocalTime.NOON), MONDAY, repo, settings)
        val cells = vm.state.value.weeks.flatten().filterNotNull()
        assertEquals(DayStatus.NOT_TAKEN, cells.first { it.date.dayOfMonth == 21 }.status)
        assertEquals(DayStatus.BREAK, cells.first { it.date.dayOfMonth == 22 }.status) // past break: never "not taken"
        assertEquals(DayStatus.BREAK, cells.first { it.date.dayOfMonth == 27 }.status) // future break too

        settings.settings.value = settings.settings.value?.copy(packType = PackType.WITH_PLACEBO)
        val placebo = vm.state.value.weeks.flatten().filterNotNull().first { it.date.dayOfMonth == 22 }
        assertEquals(DayStatus.NOT_TAKEN, placebo.status)
        assertTrue(placebo.isPlacebo)
    }

    @Test
    fun takenStoresPillDayAndInstantAndTwiceIsANoOp() {
        val clock = clockAt(LocalDate.of(2026, 10, 6), LocalTime.of(8, 0))
        val vm = HomeViewModel(clock, MONDAY, repo, settings)
        vm.onEvent(HomeUiEvent.Taken)
        val first = repo.intakes.value
        clock.instant = clock.instant.plusSeconds(60)
        vm.onEvent(HomeUiEvent.Taken)
        assertTrue(vm.state.value.taken)
        assertEquals(first, repo.intakes.value)
        assertEquals(mapOf(LocalDate.of(2026, 10, 6) to first.values.single()), repo.intakes.value)
    }

    @Test
    fun notTakenUndoesTodayOnly() {
        repo.intakes.value = mapOf(oct6.minusDays(1) to Instant.EPOCH)
        val vm = HomeViewModel(clockAt(oct6, LocalTime.of(8, 0)), MONDAY, repo, settings)
        vm.onEvent(HomeUiEvent.Taken)
        assertTrue(vm.state.value.taken)
        vm.onEvent(HomeUiEvent.NotTaken)
        assertFalse(vm.state.value.taken)
        assertEquals(setOf(oct6.minusDays(1)), repo.intakes.value.keys)
    }

    @Test
    fun homeWarnsWhileNotificationsAreOff() {
        val vm = HomeViewModel(clockAt(oct6, LocalTime.NOON), MONDAY, repo, settings)
        assertFalse(vm.state.value.notificationsOff)
        vm.onEvent(HomeUiEvent.NotificationsChecked(allowed = false))
        assertTrue(vm.state.value.notificationsOff)
        vm.onEvent(HomeUiEvent.NotificationsChecked(allowed = true))
        assertFalse(vm.state.value.notificationsOff)
    }

    @Test
    fun homeRollsOverAtMidnightOnResume() {
        val clock = clockAt(LocalDate.of(2026, 10, 5), LocalTime.of(23, 59))
        val vm = HomeViewModel(clock, MONDAY, repo, settings)
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
        val vm = HomeViewModel(clock, MONDAY, repo, settings)
        clock.instant = clock.instant.plusSeconds(120)
        vm.onEvent(HomeUiEvent.Taken)
        assertEquals(setOf(LocalDate.of(2026, 10, 6)), repo.intakes.value.keys)
    }

    @Test
    fun todayFollowsTravelZoneChange() {
        // Same instant: already Oct 6 in Madrid, still Oct 5 in New York.
        val clock = clockAt(LocalDate.of(2026, 10, 6), LocalTime.of(2, 0))
        val vm = HomeViewModel(clock, MONDAY, repo, settings)
        clock.zoneId = ZoneId.of("America/New_York")
        vm.onEvent(HomeUiEvent.Resumed)
        assertEquals(LocalDate.of(2026, 10, 5), vm.state.value.today)
    }

    @Test
    fun todayOnDstChangeDay() {
        // Europe falls back on 2026-10-25 at 03:00 -> 02:00.
        val vm = HomeViewModel(clockAt(LocalDate.of(2026, 10, 25), LocalTime.of(2, 30)), MONDAY, repo, settings)
        assertEquals(LocalDate.of(2026, 10, 25), vm.state.value.today)
    }

    @Test
    fun calendarShowsTakenNotTakenUpcomingAndNavigatesMonths() {
        repo.intakes.value = mapOf(LocalDate.of(2026, 10, 3) to Instant.EPOCH)
        val vm = HomeViewModel(clockAt(LocalDate.of(2026, 10, 6), LocalTime.NOON), MONDAY, repo, settings)
        val cells = vm.state.value.weeks.flatten().filterNotNull()
        assertEquals(YearMonth.of(2026, 10), vm.state.value.month)
        assertEquals(LocalDate.of(2026, 10, 6), cells.single { it.isToday }.date)
        assertEquals(DayStatus.TAKEN, cells.first { it.date.dayOfMonth == 3 }.status)
        assertEquals(DayStatus.NOT_TAKEN, cells.first { it.date.dayOfMonth == 5 }.status)
        assertEquals(DayStatus.UPCOMING, cells.first { it.date.dayOfMonth == 7 }.status)

        vm.onEvent(HomeUiEvent.PreviousMonth)
        assertEquals(YearMonth.of(2026, 9), vm.state.value.month)
        assertFalse(vm.state.value.weeks.flatten().filterNotNull().any { it.isToday })
        vm.onEvent(HomeUiEvent.NextMonth)
        vm.onEvent(HomeUiEvent.NextMonth)
        assertEquals(YearMonth.of(2026, 11), vm.state.value.month)
    }

    @Test
    fun calendarUpdatesWhenAPillIsLogged() {
        val vm = HomeViewModel(clockAt(LocalDate.of(2026, 10, 6), LocalTime.NOON), MONDAY, repo, settings)
        repo.intakes.value = mapOf(LocalDate.of(2026, 10, 6) to Instant.EPOCH)
        assertEquals(DayStatus.TAKEN, vm.state.value.weeks.flatten().filterNotNull().single { it.isToday }.status)
    }

    @Test
    fun resumeReturnsCalendarToTheCurrentMonth() {
        val clock = clockAt(LocalDate.of(2026, 10, 31), LocalTime.of(23, 59))
        val vm = HomeViewModel(clock, MONDAY, repo, settings)
        vm.onEvent(HomeUiEvent.PreviousMonth)
        clock.instant = clock.instant.plusSeconds(60) // Nov 1
        vm.onEvent(HomeUiEvent.Resumed)
        assertEquals(YearMonth.of(2026, 11), vm.state.value.month)
        assertEquals(
            LocalDate.of(2026, 11, 1),
            vm.state.value.weeks.flatten().filterNotNull().single { it.isToday }.date,
        )
    }

    @Test
    fun takenTodayShowsInTheCalendarWhileViewingAnotherMonth() {
        val vm = HomeViewModel(clockAt(LocalDate.of(2026, 10, 6), LocalTime.NOON), MONDAY, repo, settings)
        vm.onEvent(HomeUiEvent.PreviousMonth)
        vm.onEvent(HomeUiEvent.Taken)
        assertTrue(vm.state.value.taken)
        vm.onEvent(HomeUiEvent.NextMonth)
        assertEquals(DayStatus.TAKEN, vm.state.value.weeks.flatten().filterNotNull().single { it.isToday }.status)
    }

    private val saved = Settings(PackType.WITH_BREAK, LocalDate.of(2026, 10, 1), LocalTime.of(9, 0))

    @Test
    fun settingsShowsSavedAnswersAndNewTimeReschedules() {
        settings.settings.value = saved
        val vm = SettingsViewModel(clockAt(oct6, LocalTime.NOON), settings, scheduler)
        assertEquals(saved, vm.state.value.settings)
        vm.onEvent(SettingsUiEvent.TimeClicked)
        assertTrue(vm.state.value.showTimePicker)
        vm.onEvent(SettingsUiEvent.TimePicked(LocalTime.of(21, 0)))
        assertFalse(vm.state.value.showTimePicker)
        assertEquals(saved.copy(reminderTime = LocalTime.of(21, 0)), settings.settings.value)
        assertEquals(listOf(LocalTime.of(21, 0)), scheduler.scheduled)
    }

    @Test
    fun choosingAGuineaPigSavesItAndKeepsTheReminder() {
        settings.settings.value = saved
        val vm = SettingsViewModel(clockAt(oct6, LocalTime.NOON), settings, scheduler)
        assertEquals(GuineaPig.TRICOLOR, vm.state.value.settings?.guineaPig)
        vm.onEvent(SettingsUiEvent.GuineaPigChosen(GuineaPig.CREAM))
        assertEquals(saved.copy(guineaPig = GuineaPig.CREAM), settings.settings.value)
        assertEquals(emptyList<LocalTime>(), scheduler.scheduled)
    }

    @Test
    fun resetAsksFirstThenDeletesEverythingAndCancelsTheReminder() {
        settings.settings.value = saved
        val vm = SettingsViewModel(clockAt(oct6, LocalTime.NOON), settings, scheduler)
        vm.onEvent(SettingsUiEvent.ResetClicked)
        assertTrue(vm.state.value.confirmReset)
        vm.onEvent(SettingsUiEvent.ResetDismissed)
        assertFalse(vm.state.value.confirmReset)
        assertEquals(saved, settings.settings.value) // nothing deleted without confirming

        vm.onEvent(SettingsUiEvent.ResetClicked)
        vm.onEvent(SettingsUiEvent.ResetConfirmed)
        assertEquals(null, settings.settings.value)
        assertTrue(scheduler.cancelled)
        assertTrue(vm.state.value.resetDone)
    }

    @Test
    fun realigningThePackKeepsHistoryAndMovesOnlyThePackStart() {
        settings.settings.value = saved // 21 pills from Oct 1: Oct 6 is day 6
        repo.intakes.value = mapOf(LocalDate.of(2026, 10, 2) to Instant.EPOCH)
        val vm = SettingsViewModel(clockAt(oct6, LocalTime.NOON), settings, scheduler)
        assertEquals(6, vm.state.value.dayOfPack)
        assertFalse(vm.state.value.breakDay)

        vm.onEvent(SettingsUiEvent.DayOfPackChanged(24)) // she is actually in her break week
        assertEquals(saved.copy(packStart = oct6.minusDays(23)), settings.settings.value)
        assertEquals(24, vm.state.value.dayOfPack)
        assertTrue(vm.state.value.breakDay)
        assertEquals(setOf(LocalDate.of(2026, 10, 2)), repo.intakes.value.keys)

        vm.onEvent(SettingsUiEvent.DayOfPackChanged(29))
        assertEquals(28, vm.state.value.dayOfPack) // stays within the pack
    }
}
