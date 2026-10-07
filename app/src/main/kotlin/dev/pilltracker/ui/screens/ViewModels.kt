package dev.pilltracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilltracker.domain.IntakeRepository
import dev.pilltracker.domain.Settings
import dev.pilltracker.domain.SettingsRepository
import dev.pilltracker.domain.rules.DayKind
import dev.pilltracker.domain.rules.PackType
import dev.pilltracker.domain.rules.dayKind
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

/** Preset reminder time (AC1.1). */
private val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.parse("09:00")

/** Kind of [date] under [settings]; before setup every day counts as active. */
private fun kindOf(
    settings: Settings?,
    date: LocalDate,
) = settings?.let { dayKind(it.packType, it.packStart, date) } ?: DayKind.ACTIVE

data class SetupUiState(
    val packType: PackType = PackType.TWENTY_EIGHT,
    val pillNumber: Int = 1,
    val time: LocalTime = DEFAULT_REMINDER_TIME,
    val showTimePicker: Boolean = false,
    val permissionDenied: Boolean = false,
) {
    /** 28 for a 28 pack, 21 for 21+7 (AC1.1). */
    val maxPillNumber: Int get() = if (packType == PackType.TWENTY_EIGHT) PILLS_28 else PILLS_21

    private companion object {
        const val PILLS_28 = 28
        const val PILLS_21 = 21
    }
}

sealed interface SetupUiEvent {
    data class PackTypeChosen(val type: PackType) : SetupUiEvent

    data class PillNumberChanged(val number: Int) : SetupUiEvent

    data object TimeClicked : SetupUiEvent

    data class TimePicked(val time: LocalTime) : SetupUiEvent

    data object PickerDismissed : SetupUiEvent

    /** Enable or Continue: store the answers (AC1.4). */
    data object Save : SetupUiEvent
}

@HiltViewModel
class SetupViewModel
    @Inject
    constructor(
        private val clock: Clock,
        private val settings: SettingsRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(SetupUiState())
        val state: StateFlow<SetupUiState> = _state.asStateFlow()

        fun onEvent(event: SetupUiEvent) {
            if (event == SetupUiEvent.Save) return save()
            _state.update {
                when (event) {
                    is SetupUiEvent.PackTypeChosen -> it.copy(packType = event.type).clampPill()
                    is SetupUiEvent.PillNumberChanged -> it.copy(pillNumber = event.number).clampPill()
                    SetupUiEvent.TimeClicked -> it.copy(showTimePicker = true)
                    is SetupUiEvent.TimePicked -> it.copy(time = event.time, showTimePicker = false)
                    SetupUiEvent.PickerDismissed -> it.copy(showTimePicker = false)
                    SetupUiEvent.Save -> it
                }
            }
        }

        private fun SetupUiState.clampPill() = copy(pillNumber = pillNumber.coerceIn(1, maxPillNumber))

        private fun save() {
            val s = _state.value
            val start = packStart(LocalDate.now(clock), s.pillNumber)
            viewModelScope.launch { settings.save(Settings(s.packType, start, s.time)) }
        }
    }

data class HomeUiState(
    val today: LocalDate,
    val taken: Boolean = false,
    val dayKind: DayKind = DayKind.ACTIVE,
)

sealed interface HomeUiEvent {
    data object Taken : HomeUiEvent

    /** App came to the foreground: re-read "today" (midnight passed, or the zone changed). */
    data object Resumed : HomeUiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val clock: Clock,
        private val intakes: IntakeRepository,
        settings: SettingsRepository,
    ) : ViewModel() {
        private val today = MutableStateFlow(LocalDate.now(clock))

        val state: StateFlow<HomeUiState> =
            today
                .flatMapLatest { day ->
                    combine(intakes.takenDays(day, day), settings.settings) { taken, s ->
                        HomeUiState(day, day in taken, kindOf(s, day))
                    }
                }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState(today.value))

        fun onEvent(event: HomeUiEvent) {
            // Both events re-read the clock, so a tap after midnight logs the new pill day.
            today.value = LocalDate.now(clock)
            if (event == HomeUiEvent.Taken) {
                viewModelScope.launch { intakes.markTaken(today.value, clock.instant()) }
            }
        }
    }

data class HistoryUiState(
    val month: YearMonth,
    val firstDayOfWeek: DayOfWeek,
    val weeks: List<List<DayCell?>>,
)

sealed interface HistoryUiEvent {
    data object Previous : HistoryUiEvent

    data object Next : HistoryUiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        clock: Clock,
        private val firstDayOfWeek: DayOfWeek,
        intakes: IntakeRepository,
        settings: SettingsRepository,
    ) : ViewModel() {
        private val today = LocalDate.now(clock)
        private val month = MutableStateFlow(YearMonth.from(today))

        val state: StateFlow<HistoryUiState> =
            month
                .flatMapLatest { m ->
                    combine(intakes.takenDays(m.atDay(1), m.atEndOfMonth()), settings.settings) { taken, s ->
                        stateFor(m, taken, s)
                    }
                }.stateIn(viewModelScope, SharingStarted.Eagerly, stateFor(month.value, emptySet(), null))

        fun onEvent(event: HistoryUiEvent) =
            month.update {
                when (event) {
                    HistoryUiEvent.Previous -> it.minusMonths(1)
                    HistoryUiEvent.Next -> it.plusMonths(1)
                }
            }

        private fun stateFor(
            month: YearMonth,
            taken: Set<LocalDate>,
            settings: Settings?,
        ) = HistoryUiState(
            month = month,
            firstDayOfWeek = firstDayOfWeek,
            weeks =
                monthLayout(month, firstDayOfWeek).map {
                        week ->
                    week.map { it?.let { cell(it, taken, settings) } }
                },
        )

        private fun cell(
            date: LocalDate,
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
