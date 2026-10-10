package dev.pilltracker.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.pilltracker.domain.LogTakenUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The notification's Taken action, logged without opening the app (AC2.3). */
@AndroidEntryPoint
class TakenReceiver : BroadcastReceiver() {
    @Inject lateinit var logTaken: LogTakenUseCase

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val done = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                logTaken()
            } finally {
                done.finish()
            }
        }
    }
}
