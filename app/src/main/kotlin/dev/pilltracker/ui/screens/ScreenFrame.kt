package dev.pilltracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.pilltracker.ui.components.GuineaPigBackground

/** Shared screen frame: system-bar insets, page padding, large title with an optional corner [action]. */
@Composable
internal fun ScreenFrame(
    title: String,
    action: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold { insets ->
        GuineaPigBackground(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(insets).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row {
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
                    action()
                }
                content()
            }
        }
    }
}
