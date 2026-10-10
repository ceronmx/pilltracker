package dev.pilltracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilltracker.domain.ReminderScheduler
import dev.pilltracker.domain.Settings
import dev.pilltracker.domain.SettingsRepository
import dev.pilltracker.domain.rules.DayKind
import dev.pilltracker.domain.rules.PACK_DAYS
import dev.pilltracker.domain.rules.PackType
import dev.pilltracker.domain.rules.dayKind
import dev.pilltracker.domain.rules.packStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** Any fixed day: setup only needs the day kind relative to the pack, not a real date. */
private val REFERENCE_DAY: LocalDate = LocalDate.ofEpochDay(0)

/** Preset reminder time (AC1.1). */
private val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.parse("09:00")

data class SetupUiState(
    val packType: PackType = PackType.WITH_PLACEBO,
    val pillNumber: Int = 1,
    val time: LocalTime = DEFAULT_REMINDER_TIME,
    val showTimePicker: Boolean = false,
    val permissionDenied: Boolean = false,
    /** Settings are written and the reminder scheduled: only now may Setup be left (AC1.4). */
    val saved: Boolean = false,
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
                _state.update { it.copy(saved = true) }
            }
        }
    }
