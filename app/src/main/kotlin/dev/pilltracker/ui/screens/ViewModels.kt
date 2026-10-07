package dev.pilltracker.ui.screens

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilltracker.domain.rules.monthLayout
import dev.pilltracker.ui.components.DayCell
import dev.pilltracker.ui.components.DayStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

// ponytail: all state is in memory until the storage, permission and alarm beads land.

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
}

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(clock: Clock) : ViewModel() {
        // ponytail: "today" is read once; refresh at midnight when the repository bead adds observation.
        private val _state = MutableStateFlow(HomeUiState(LocalDate.now(clock)))
        val state: StateFlow<HomeUiState> = _state.asStateFlow()

        fun onEvent(event: HomeUiEvent) =
            _state.update {
                when (event) {
                    HomeUiEvent.Taken -> it.copy(taken = true)
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

@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        clock: Clock,
        private val firstDayOfWeek: DayOfWeek,
    ) : ViewModel() {
        private val today = LocalDate.now(clock)
        private val _state = MutableStateFlow(stateFor(YearMonth.from(today)))
        val state: StateFlow<HistoryUiState> = _state.asStateFlow()

        fun onEvent(event: HistoryUiEvent) =
            _state.update {
                when (event) {
                    HistoryUiEvent.Previous -> stateFor(it.month.minusMonths(1))
                    HistoryUiEvent.Next -> stateFor(it.month.plusMonths(1))
                }
            }

        private fun stateFor(month: YearMonth) =
            HistoryUiState(
                month = month,
                firstDayOfWeek = firstDayOfWeek,
                weeks =
                    monthLayout(month, firstDayOfWeek).map { week ->
                        week.map { date ->
                            date?.let {
                                val status = if (it > today) DayStatus.UPCOMING else DayStatus.NOT_TAKEN
                                DayCell(it, status, isToday = it == today)
                            }
                        }
                    },
            )
    }
