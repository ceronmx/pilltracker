package dev.pilltracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilltracker.domain.IntakeRepository
import dev.pilltracker.domain.LogTakenUseCase
import dev.pilltracker.domain.ReminderScheduler
import dev.pilltracker.domain.Settings
import dev.pilltracker.domain.SettingsRepository
import dev.pilltracker.domain.rules.DayKind
import dev.pilltracker.domain.rules.dayKind
import dev.pilltracker.domain.rules.daysSinceLastPill
import dev.pilltracker.domain.rules.hoursLate
import dev.pilltracker.domain.rules.monthLayout
import dev.pilltracker.domain.rules.packStart
import dev.pilltracker.domain.rules.shouldRemind
import dev.pilltracker.ui.components.DayCell
import dev.pilltracker.ui.components.DayStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Kind of [date] under [settings]; before setup every day counts as active. */
internal fun kindOf(
    settings: Settings?,
    date: LocalDate,
) = settings?.let { dayKind(it.packType, it.packStart, date) } ?: DayKind.ACTIVE

/** Today's Taken / Break state and the month grid on one screen (AC3.1, AC4.1). */
data class HomeUiState(
    val today: LocalDate,
    val taken: Boolean = false,
    val dayKind: DayKind = DayKind.ACTIVE,
    val month: YearMonth = YearMonth.from(today),
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val weeks: List<List<DayCell?>> = emptyList(),
    /** Notifications are switched off in system settings: reminders can't show (x6z). */
    val notificationsOff: Boolean = false,
    /** Days since the last logged pill, set only when a pill day in between went unlogged (AC4.3). */
    val daysSinceLastPill: Int? = null,
    /** Whole hours since today's reminder time while today is unlogged, from the first full hour (AC3.6). */
    val hoursLate: Int? = null,
)

sealed interface HomeUiEvent {
    data object Taken : HomeUiEvent

    /** Undo a wrong Taken tap for the shown day (AC3.5). */
    data object NotTaken : HomeUiEvent

    /** App came to the foreground: re-read "today" (midnight passed, or the zone changed), back to its month. */
    data object Resumed : HomeUiEvent

    data object PreviousMonth : HomeUiEvent

    /** Result of the platform check on resume; the ViewModel stays free of Android APIs. */
    data class NotificationsChecked(val allowed: Boolean) : HomeUiEvent

    data object NextMonth : HomeUiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val clock: Clock,
        private val firstDayOfWeek: DayOfWeek,
        private val intakes: IntakeRepository,
        settings: SettingsRepository,
        private val logTaken: LogTakenUseCase,
        private val reminders: ReminderScheduler,
    ) : ViewModel() {
        private val today = MutableStateFlow(LocalDate.now(clock))
        private val month = MutableStateFlow(YearMonth.from(today.value))
        private val notificationsOff = MutableStateFlow(false)

        /** Bumped on resume so hours late refresh within the same day. */
        private val resumedAt = MutableStateFlow(clock.instant())

        val state: StateFlow<HomeUiState> =
            combine(today, month, resumedAt) { day, m, _ -> day to m }
                .flatMapLatest { (day, m) ->
                    // One query covers the shown month and today, which may lie outside it.
                    val from = minOf(m.atDay(1), day)
                    val to = maxOf(m.atEndOfMonth(), day)
                    val last = intakes.lastTakenDay(day)
                    combine(intakes.takenDays(from, to), last, settings.settings) { taken, lastDay, s ->
                        val sinceLast = s?.let { daysSinceLastPill(it.packType, it.packStart, lastDay, day) }
                        val late =
                            s?.takeIf { shouldRemind(it, day, day in taken) }
                                ?.let { hoursLate(it.reminderTime, clock.instant(), clock.zone) }
                        stateFor(day, m, taken, s).copy(daysSinceLastPill = sinceLast, hoursLate = late)
                    }
                }.combine(notificationsOff) { s, off -> s.copy(notificationsOff = off) }
                .stateIn(viewModelScope, SharingStarted.Eagerly, stateFor(today.value, month.value, emptySet(), null))

        fun onEvent(event: HomeUiEvent) {
            when (event) {
                HomeUiEvent.PreviousMonth -> month.update { it.minusMonths(1) }
                HomeUiEvent.NextMonth -> month.update { it.plusMonths(1) }
                is HomeUiEvent.NotificationsChecked -> notificationsOff.value = !event.allowed
                // Both re-read the clock, so a tap after midnight logs the new pill day.
                HomeUiEvent.Resumed -> {
                    resumedAt.value = clock.instant()
                    today.value = LocalDate.now(clock)
                    month.value = YearMonth.from(today.value)
                }
                HomeUiEvent.Taken -> {
                    today.value = LocalDate.now(clock)
                    viewModelScope.launch { logTaken() }
                }
                // The day on screen, not a re-read clock: undo what she sees as "logged for today".
                // Repeats stay off: undoing a wrong tap must not restart them (ADR 0005).
                HomeUiEvent.NotTaken ->
                    viewModelScope.launch {
                        intakes.markNotTaken(today.value)
                        reminders.cancelRepeat()
                    }
            }
        }

        private fun stateFor(
            today: LocalDate,
            month: YearMonth,
            taken: Set<LocalDate>,
            settings: Settings?,
        ) = HomeUiState(
            today = today,
            taken = today in taken,
            dayKind = kindOf(settings, today),
            month = month,
            firstDayOfWeek = firstDayOfWeek,
            weeks =
                monthLayout(month, firstDayOfWeek).map {
                        week ->
                    week.map { it?.let { cell(it, today, taken, settings) } }
                },
        )

        private fun cell(
            date: LocalDate,
            today: LocalDate,
            taken: Set<LocalDate>,
            settings: Settings?,
        ): DayCell {
            val kind = kindOf(settings, date)
            val status =
                when {
                    kind == DayKind.BREAK -> DayStatus.BREAK
                    date in taken -> DayStatus.TAKEN
                    date > today -> DayStatus.UPCOMING
                    else -> DayStatus.NOT_TAKEN
                }
            return DayCell(date, status, isToday = date == today, isPlacebo = kind == DayKind.PLACEBO)
        }
    }
