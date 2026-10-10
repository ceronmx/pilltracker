package dev.pilltracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilltracker.domain.GuineaPig
import dev.pilltracker.domain.ReminderScheduler
import dev.pilltracker.domain.Settings
import dev.pilltracker.domain.SettingsRepository
import dev.pilltracker.domain.rules.DayKind
import dev.pilltracker.domain.rules.PACK_DAYS
import dev.pilltracker.domain.rules.dayOfPack
import dev.pilltracker.domain.rules.packStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

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

    data class GuineaPigChosen(val pig: GuineaPig) : SettingsUiEvent

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
                    change({ it.copy(reminderTime = event.time) }) { reminders.scheduleNext(event.time) }
                }
                // ponytail: one pack start, so past break/placebo marks shift with it; keep a start
                // history if she needs exact past packs. Intake records are untouched.
                is SettingsUiEvent.DayOfPackChanged ->
                    change({ it.copy(packStart = packStart(LocalDate.now(clock), event.day.coerceIn(1, PACK_DAYS))) })
                is SettingsUiEvent.GuineaPigChosen -> change({ it.copy(guineaPig = event.pig) })
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

        /** Saves an edit of the stored settings, then runs [after]; no-op before setup. */
        private fun change(
            edit: (Settings) -> Settings,
            after: () -> Unit = {},
        ) {
            val current = state.value.settings ?: return
            viewModelScope.launch {
                settings.save(edit(current))
                after()
            }
        }
    }
