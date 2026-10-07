package dev.pilltracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilltracker.domain.IntakeRepository
import dev.pilltracker.domain.ReminderScheduler
import dev.pilltracker.domain.Settings
import dev.pilltracker.domain.SettingsRepository
import dev.pilltracker.domain.rules.DayKind
import dev.pilltracker.domain.rules.PACK_DAYS
import dev.pilltracker.domain.rules.PackType
import dev.pilltracker.domain.rules.dayKind
import dev.pilltracker.domain.rules.dayOfPack
import dev.pilltracker.domain.rules.monthLayout
import dev.pilltracker.domain.rules.packStart
import dev.pilltracker.ui.components.DayCell
import dev.pilltracker.ui.components.DayStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

/** Any fixed day: setup only needs the day kind relative to the pack, not a real date. */
private val REFERENCE_DAY: LocalDate = LocalDate.ofEpochDay(0)

/** Preset reminder time (AC1.1). */
private val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.parse("09:00")

/** Kind of [date] under [settings]; before setup every day counts as active. */
private fun kindOf(
    settings: Settings?,
    date: LocalDate,
) = settings?.let { dayKind(it.packType, it.packStart, date) } ?: DayKind.ACTIVE

data class SetupUiState(
    val packType: PackType = PackType.WITH_PLACEBO,
    val pillNumber: Int = 1,
    val time: LocalTime = DEFAULT_REMINDER_TIME,
    val showTimePicker: Boolean = false,
    val permissionDenied: Boolean = false,
) {
    /** What the chosen day is, so 21-pill days 22-28 read as break days (AC1.1). */
    val dayKind: DayKind get() = dayKind(packType, packStart(REFERENCE_DAY, pillNumber), REFERENCE_DAY)
}

sealed interface SetupUiEvent {
    data class PackTypeChosen(val type: PackType) : SetupUiEvent

    data class PillNumberChanged(val number: Int) : SetupUiEvent

    data object TimeClicked : SetupUiEvent

    data class TimePicked(val time: LocalTime) : SetupUiEvent

    data object PickerDismissed : SetupUiEvent

    /** The notification permission request came back denied (AC1.3). */
    data object NotificationsDenied : SetupUiEvent

    /** Enable or Continue: store the answers (AC1.4). */
    data object Save : SetupUiEvent
}

@HiltViewModel
class SetupViewModel
    @Inject
    constructor(
        private val clock: Clock,
        private val settings: SettingsRepository,
        private val reminders: ReminderScheduler,
    ) : ViewModel() {
        private val _state = MutableStateFlow(SetupUiState())
        val state: StateFlow<SetupUiState> = _state.asStateFlow()

        fun onEvent(event: SetupUiEvent) {
            if (event == SetupUiEvent.Save) return save()
            _state.update {
                when (event) {
                    is SetupUiEvent.PackTypeChosen -> it.copy(packType = event.type)
                    is SetupUiEvent.PillNumberChanged -> it.copy(pillNumber = event.number.coerceIn(1, PACK_DAYS))
                    SetupUiEvent.TimeClicked -> it.copy(showTimePicker = true)
                    is SetupUiEvent.TimePicked -> it.copy(time = event.time, showTimePicker = false)
                    SetupUiEvent.PickerDismissed -> it.copy(showTimePicker = false)
                    SetupUiEvent.NotificationsDenied -> it.copy(permissionDenied = true)
                    SetupUiEvent.Save -> it
                }
            }
        }

        private fun save() {
            val s = _state.value
            val start = packStart(LocalDate.now(clock), s.pillNumber)
            viewModelScope.launch {
                settings.save(Settings(s.packType, start, s.time))
                reminders.scheduleNext(s.time)
            }
        }
    }

/** Today's Taken / Break state and the month grid on one screen (AC3.1, AC4.1). */
data class HomeUiState(
    val today: LocalDate,
    val taken: Boolean = false,
    val dayKind: DayKind = DayKind.ACTIVE,
    val month: YearMonth = YearMonth.from(today),
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val weeks: List<List<DayCell?>> = emptyList(),
)

sealed interface HomeUiEvent {
    data object Taken : HomeUiEvent

    /** App came to the foreground: re-read "today" (midnight passed, or the zone changed), back to its month. */
    data object Resumed : HomeUiEvent

    data object PreviousMonth : HomeUiEvent

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
    ) : ViewModel() {
        private val today = MutableStateFlow(LocalDate.now(clock))
        private val month = MutableStateFlow(YearMonth.from(today.value))

        val state: StateFlow<HomeUiState> =
            combine(today, month, ::Pair)
                .flatMapLatest { (day, m) ->
                    // One query covers the shown month and today, which may lie outside it.
                    val from = minOf(m.atDay(1), day)
                    val to = maxOf(m.atEndOfMonth(), day)
                    combine(intakes.takenDays(from, to), settings.settings) { taken, s -> stateFor(day, m, taken, s) }
                }.stateIn(viewModelScope, SharingStarted.Eagerly, stateFor(today.value, month.value, emptySet(), null))

        fun onEvent(event: HomeUiEvent) {
            when (event) {
                HomeUiEvent.PreviousMonth -> month.update { it.minusMonths(1) }
                HomeUiEvent.NextMonth -> month.update { it.plusMonths(1) }
                // Both re-read the clock, so a tap after midnight logs the new pill day.
                HomeUiEvent.Resumed -> {
                    today.value = LocalDate.now(clock)
                    month.value = YearMonth.from(today.value)
                }
                HomeUiEvent.Taken -> {
                    today.value = LocalDate.now(clock)
                    viewModelScope.launch { intakes.markTaken(today.value, clock.instant()) }
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

/** Settings sheet on Home (AC7.1-7.3). */
data class SettingsUiState(
    val settings: Settings? = null,
    /** Today's day of the pack, 1-28; editable to realign a pack started late or early. */
    val dayOfPack: Int = 1,
    val breakDay: Boolean = false,
    val showTimePicker: Boolean = false,
    val confirmReset: Boolean = false,
    /** All data is gone: leave for Setup. */
    val resetDone: Boolean = false,
)

sealed interface SettingsUiEvent {
    data object TimeClicked : SettingsUiEvent

    data class TimePicked(val time: LocalTime) : SettingsUiEvent

    data object PickerDismissed : SettingsUiEvent

    data class DayOfPackChanged(val day: Int) : SettingsUiEvent

    data object ResetClicked : SettingsUiEvent

    data object ResetDismissed : SettingsUiEvent

    data object ResetConfirmed : SettingsUiEvent
}

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val clock: Clock,
        private val settings: SettingsRepository,
        private val reminders: ReminderScheduler,
    ) : ViewModel() {
        private val ui = MutableStateFlow(SettingsUiState())

        val state: StateFlow<SettingsUiState> =
            combine(ui, settings.settings) { u, s ->
                val today = LocalDate.now(clock)
                u.copy(
                    settings = s,
                    dayOfPack = s?.let { dayOfPack(it.packStart, today) } ?: 1,
                    breakDay = kindOf(s, today) == DayKind.BREAK,
                )
            }
                .stateIn(viewModelScope, SharingStarted.Eagerly, ui.value)

        fun onEvent(event: SettingsUiEvent) {
            when (event) {
                SettingsUiEvent.TimeClicked -> ui.update { it.copy(showTimePicker = true) }
                SettingsUiEvent.PickerDismissed -> ui.update { it.copy(showTimePicker = false) }
                is SettingsUiEvent.TimePicked -> {
                    ui.update { it.copy(showTimePicker = false) }
                    val current = state.value.settings ?: return
                    viewModelScope.launch {
                        settings.save(current.copy(reminderTime = event.time))
                        reminders.scheduleNext(event.time)
                    }
                }
                is SettingsUiEvent.DayOfPackChanged -> {
                    val current = state.value.settings ?: return
                    val day = event.day.coerceIn(1, PACK_DAYS)
                    // ponytail: one pack start, so past break/placebo marks shift with it; keep a start
                    // history if she needs exact past packs. Intake records are untouched.
                    viewModelScope.launch {
                        settings.save(
                            current.copy(packStart = packStart(LocalDate.now(clock), day)),
                        )
                    }
                }
                SettingsUiEvent.ResetClicked -> ui.update { it.copy(confirmReset = true) }
                SettingsUiEvent.ResetDismissed -> ui.update { it.copy(confirmReset = false) }
                SettingsUiEvent.ResetConfirmed ->
                    viewModelScope.launch {
                        reminders.cancel()
                        settings.deleteAll()
                        ui.update { it.copy(confirmReset = false, resetDone = true) }
                    }
            }
        }
    }
