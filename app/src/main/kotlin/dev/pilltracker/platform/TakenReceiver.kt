package dev.pilltracker.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.pilltracker.domain.IntakeRepository
import dev.pilltracker.domain.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** The notification's Taken action: logs today without opening the app, then dismisses (AC2.3). */
@AndroidEntryPoint
class TakenReceiver : BroadcastReceiver() {
    @Inject lateinit var intakes: IntakeRepository

    @Inject lateinit var notifier: Notifier

    @Inject lateinit var clock: Clock

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val done = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                intakes.markTaken(LocalDate.now(clock), clock.instant())
                notifier.clearReminder()
            } finally {
                done.finish()
            }
        }
    }
}
