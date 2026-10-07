package dev.pilltracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilltracker.domain.IntakeRepository
import dev.pilltracker.domain.rules.monthLayout
import dev.pilltracker.ui.components.DayCell
import dev.pilltracker.ui.components.DayStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

// ponytail: setup answers are in memory until the setup-questions bead persists them.

/** Preset reminder time (AC1.1). */
private val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.parse("09:00")

data class SetupUiState(
    val time: LocalTime = DEFAULT_REMINDER_TIME,
    val showTimePicker: Boolean = false,
    val permissionDenied: Boolean = false,
)

sealed interface SetupUiEvent {
    data object TimeClicked : SetupUiEvent

    data class TimePicked(val time: LocalTime) : SetupUiEvent

    data object PickerDismissed : SetupUiEvent
}

@HiltViewModel
class SetupViewModel
    @Inject
    constructor() : ViewModel() {
        private val _state = MutableStateFlow(SetupUiState())
        val state: StateFlow<SetupUiState> = _state.asStateFlow()

        fun onEvent(event: SetupUiEvent) =
            _state.update {
                when (event) {
                    SetupUiEvent.TimeClicked -> it.copy(showTimePicker = true)
                    is SetupUiEvent.TimePicked -> it.copy(time = event.time, showTimePicker = false)
                    SetupUiEvent.PickerDismissed -> it.copy(showTimePicker = false)
                }
            }
    }

data class HomeUiState(
    val today: LocalDate,
    val taken: Boolean = false,
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
    ) : ViewModel() {
        private val today = MutableStateFlow(LocalDate.now(clock))

        val state: StateFlow<HomeUiState> =
            today
                .flatMapLatest { day -> intakes.takenDays(day, day).map { HomeUiState(day, day in it) } }
                .stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState(today.value))

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
    ) : ViewModel() {
        private val today = LocalDate.now(clock)
        private val month = MutableStateFlow(YearMonth.from(today))

        val state: StateFlow<HistoryUiState> =
            month
                .flatMapLatest { m -> intakes.takenDays(m.atDay(1), m.atEndOfMonth()).map { stateFor(m, it) } }
                .stateIn(viewModelScope, SharingStarted.Eagerly, stateFor(month.value, emptySet()))

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
        ) = HistoryUiState(
            month = month,
            firstDayOfWeek = firstDayOfWeek,
            weeks =
                monthLayout(month, firstDayOfWeek).map { week ->
                    week.map { date ->
                        date?.let {
                            val status =
                                when {
                                    it > today -> DayStatus.UPCOMING
                                    it in taken -> DayStatus.TAKEN
                                    else -> DayStatus.NOT_TAKEN
                                }
                            DayCell(it, status, isToday = it == today)
                        }
                    }
                },
        )
    }
