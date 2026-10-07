package dev.pilltracker.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.pilltracker.domain.IntakeRepository
import dev.pilltracker.domain.Settings
import dev.pilltracker.domain.SettingsRepository
import dev.pilltracker.domain.rules.PackType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Pill day is stored as given, never derived from [takenAtMillis] (AC3.2). Primary key = one row per day. */
@Entity(tableName = "intake")
data class IntakeEntity(
    @PrimaryKey val pillDayEpochDay: Long,
    val takenAtMillis: Long,
)

@Dao
interface IntakeDao {
    @Query("SELECT pillDayEpochDay FROM intake WHERE pillDayEpochDay BETWEEN :from AND :to")
    fun takenDays(
        from: Long,
        to: Long,
    ): Flow<List<Long>>

    /** IGNORE keeps the first instant when the same day is logged twice. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(intake: IntakeEntity)
}

/** Single row (id = 0) holding the setup answers. */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val packType: String,
    val packStartEpochDay: Long,
    val reminderMinuteOfDay: Int,
)

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 0")
    fun settings(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: SettingsEntity)
}

// ponytail: still version 1 because nothing has shipped; bump + MigrationTestHelper from the first release on.
@Database(entities = [IntakeEntity::class, SettingsEntity::class], version = 1)
abstract class PillDatabase : RoomDatabase() {
    abstract fun intakeDao(): IntakeDao

    abstract fun settingsDao(): SettingsDao

    companion object {
        private const val NAME = "pills.db"

        fun create(context: Context): PillDatabase =
            Room.databaseBuilder(
                context,
                PillDatabase::class.java,
                NAME,
            ).build()
    }
}

class RoomIntakeRepository(private val dao: IntakeDao) : IntakeRepository {
    override fun takenDays(
        from: LocalDate,
        to: LocalDate,
    ): Flow<Set<LocalDate>> =
        dao.takenDays(from.toEpochDay(), to.toEpochDay()).map {
            it.mapTo(mutableSetOf(), LocalDate::ofEpochDay)
        }

    override suspend fun markTaken(
        day: LocalDate,
        at: Instant,
    ) = dao.insert(IntakeEntity(day.toEpochDay(), at.toEpochMilli()))
}

/** Stored codes are fixed: renaming PackType must not break saved settings (installed 0.1 used these). */
private val PACK_CODES = mapOf(PackType.WITH_PLACEBO to "TWENTY_EIGHT", PackType.WITH_BREAK to "TWENTY_ONE_PLUS_SEVEN")

class RoomSettingsRepository(private val db: PillDatabase) : SettingsRepository {
    private val dao = db.settingsDao()

    override val settings: Flow<Settings?> =
        dao.settings().map { e ->
            e?.let {
                Settings(
                    PACK_CODES.entries.first { (_, code) -> code == it.packType }.key,
                    LocalDate.ofEpochDay(it.packStartEpochDay),
                    LocalTime.ofSecondOfDay(it.reminderMinuteOfDay * SECONDS_PER_MINUTE),
                )
            }
        }

    override suspend fun save(settings: Settings) =
        dao.save(
            SettingsEntity(
                packType = PACK_CODES.getValue(settings.packType),
                packStartEpochDay = settings.packStart.toEpochDay(),
                reminderMinuteOfDay = settings.reminderTime.toSecondOfDay() / SECONDS_PER_MINUTE.toInt(),
            ),
        )

    /** Every table, so data added later is wiped too. clearAllTables blocks, hence IO. */
    override suspend fun deleteAll() = withContext(Dispatchers.IO) { db.clearAllTables() }

    private companion object {
        const val SECONDS_PER_MINUTE = 60L
    }
}
