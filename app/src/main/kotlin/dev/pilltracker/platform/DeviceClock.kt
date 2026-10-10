package dev.pilltracker.platform

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.pilltracker.domain.HandleReminderUseCase
import dev.pilltracker.domain.IntakeRepository
import dev.pilltracker.domain.LogTakenUseCase
import dev.pilltracker.domain.Notifier
import dev.pilltracker.domain.ReminderScheduler
import dev.pilltracker.domain.SettingsRepository
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * The only place allowed to read system time (ADR 0002). Re-reads the zone on every call so
 * "today" follows the user across time-zone changes; Clock.systemDefaultZone() would freeze it.
 */
object DeviceClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun instant(): Instant = Instant.now()

    override fun withZone(zone: ZoneId): Clock = system(zone)
}

@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {
    @Provides fun clock(): Clock = DeviceClock

    @Provides fun reminders(impl: AlarmReminderScheduler): ReminderScheduler = impl

    @Provides fun notifier(impl: NotificationNotifier): Notifier = impl

    // :domain is pure Kotlin without javax.inject, so its use cases are wired here.
    @Provides fun handleReminder(
        settings: SettingsRepository,
        intakes: IntakeRepository,
        scheduler: ReminderScheduler,
        notifier: Notifier,
        clock: Clock,
    ) = HandleReminderUseCase(settings, intakes, scheduler, notifier, clock)

    @Provides fun logTaken(
        intakes: IntakeRepository,
        notifier: Notifier,
        scheduler: ReminderScheduler,
        clock: Clock,
    ) = LogTakenUseCase(intakes, notifier, scheduler, clock)

    /** Read when a screen is created, so a new system language applies on the next visit. */
    @Provides fun firstDayOfWeek(): DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
}
