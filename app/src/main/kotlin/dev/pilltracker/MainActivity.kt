package dev.pilltracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.pilltracker.ui.screens.HistoryScreen
import dev.pilltracker.ui.screens.HistoryViewModel
import dev.pilltracker.ui.screens.HomeScreen
import dev.pilltracker.ui.screens.HomeUiEvent
import dev.pilltracker.ui.screens.HomeViewModel
import dev.pilltracker.ui.screens.SetupScreen
import dev.pilltracker.ui.screens.SetupViewModel
import dev.pilltracker.ui.theme.PillTrackerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { PillTrackerTheme { PillTrackerNavHost() } }
    }
}

private enum class Route { Setup, Home, History }

// ponytail: always starts at Setup until the storage bead remembers that setup is done.
@Composable
private fun PillTrackerNavHost() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Route.Setup.name) {
        composable(Route.Setup.name) {
            // Setup is left behind for good: Home replaces it so Back exits the app.
            val vm: SetupViewModel = hiltViewModel()
            val state by vm.state.collectAsState()
            val toHome = { nav.navigate(Route.Home.name) { popUpTo(Route.Setup.name) { inclusive = true } } }
            SetupScreen(state, vm::onEvent, onEnable = toHome, onContinueWithout = toHome)
        }
        composable(Route.Home.name) {
            val vm: HomeViewModel = hiltViewModel()
            val state by vm.state.collectAsState()
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.onEvent(HomeUiEvent.Resumed) }
            HomeScreen(state, vm::onEvent, onHistory = { nav.navigate(Route.History.name) })
        }
        composable(Route.History.name) {
            val vm: HistoryViewModel = hiltViewModel()
            val state by vm.state.collectAsState()
            HistoryScreen(state, vm::onEvent)
        }
    }
}
