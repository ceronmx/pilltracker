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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

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

@Database(entities = [IntakeEntity::class], version = 1)
abstract class PillDatabase : RoomDatabase() {
    abstract fun intakeDao(): IntakeDao

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
