package dev.pilltracker

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.pilltracker.data.PillDatabase
import dev.pilltracker.data.RoomIntakeRepository
import dev.pilltracker.data.RoomSettingsRepository
import dev.pilltracker.domain.IntakeRepository
import dev.pilltracker.domain.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): PillDatabase = PillDatabase.create(context)

    @Provides fun intakes(db: PillDatabase): IntakeRepository = RoomIntakeRepository(db.intakeDao())

    @Provides fun settings(db: PillDatabase): SettingsRepository = RoomSettingsRepository(db)
}
