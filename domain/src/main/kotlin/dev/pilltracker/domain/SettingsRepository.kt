package dev.pilltracker.domain

import dev.pilltracker.domain.rules.PackType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/** The app's guinea pig art (AC7.5). Cosmetic only. */
enum class GuineaPig { TRICOLOR, GINGER, BLACK_WHITE, CREAM }

/** Setup answers (AC1.1, AC1.4). [packStart] is the day of pill 1 of the current pack. */
data class Settings(
    val packType: PackType,
    val packStart: LocalDate,
    val reminderTime: LocalTime,
    val guineaPig: GuineaPig = GuineaPig.TRICOLOR,
)

interface SettingsRepository {
    /** Null until setup is done. */
    val settings: Flow<Settings?>

    suspend fun save(settings: Settings)

    /** Reset (AC7.3): deletes all app data, settings and intakes alike. */
    suspend fun deleteAll()
}
