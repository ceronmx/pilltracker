package dev.pilltracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.pilltracker.domain.SettingsRepository
import dev.pilltracker.ui.screens.HomeScreen
import dev.pilltracker.ui.screens.HomeUiEvent
import dev.pilltracker.ui.screens.HomeViewModel
import dev.pilltracker.ui.screens.SettingsSheet
import dev.pilltracker.ui.screens.SettingsViewModel
import dev.pilltracker.ui.screens.SetupScreen
import dev.pilltracker.ui.screens.SetupUiEvent
import dev.pilltracker.ui.screens.SetupViewModel
import dev.pilltracker.ui.theme.PillTrackerTheme
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Setup done = settings saved (AC1.4). Decided once, from the first read; blank until then.
            val start by produceState<Route?>(null) {
                value = if (settings.settings.first() == null) Route.Setup else Route.Home
            }
            PillTrackerTheme { start?.let { PillTrackerNavHost(it) } }
        }
    }
}

private enum class Route { Setup, Home }

@Composable
private fun PillTrackerNavHost(start: Route) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = start.name) {
        composable(Route.Setup.name) {
            // Setup is left behind for good: Home replaces it so Back exits the app.
            SetupRoute { nav.navigate(Route.Home.name) { popUpTo(Route.Setup.name) { inclusive = true } } }
        }
        composable(Route.Home.name) {
            val vm: HomeViewModel = hiltViewModel()
            val state by vm.state.collectAsState()
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.onEvent(HomeUiEvent.Resumed) }
            var showSettings by rememberSaveable { mutableStateOf(false) }
            HomeScreen(state, vm::onEvent, onSettings = { showSettings = true })
            if (showSettings) {
                SettingsRoute(onClose = { showSettings = false }) {
                    // All data deleted (AC7.3): back to Setup, with no way back to Home.
                    nav.navigate(Route.Setup.name) { popUpTo(Route.Home.name) { inclusive = true } }
                }
            }
        }
    }
}

@Composable
private fun SettingsRoute(
    onClose: () -> Unit,
    onReset: () -> Unit,
) {
    val vm: SettingsViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    LaunchedEffect(state.resetDone) { if (state.resetDone) onReset() }
    SettingsSheet(state, vm::onEvent, onClose)
}

/** Enable asks for notifications only (ADR 0004); denial shows the notice with retry (AC1.2, AC1.3). */
@Composable
private fun SetupRoute(onDone: () -> Unit) {
    val vm: SetupViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val finish = {
        vm.onEvent(SetupUiEvent.Save)
        onDone()
    }
    val request =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) finish() else vm.onEvent(SetupUiEvent.NotificationsDenied)
        }
    // Back from the app's notification settings: carry on by itself once allowed.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (state.permissionDenied && notificationsAllowed(context)) finish()
    }
    SetupScreen(
        state,
        vm::onEvent,
        onEnable = {
            when {
                notificationsAllowed(context) -> finish() // API < 33, or already granted
                // Retry: the system stops showing the dialog after repeated denials; Settings always works.
                state.permissionDenied -> context.startActivity(appNotificationSettings(context))
                else -> request.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onContinueWithout = finish,
    )
}

private fun notificationsAllowed(context: Context) = NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun appNotificationSettings(context: Context) =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
