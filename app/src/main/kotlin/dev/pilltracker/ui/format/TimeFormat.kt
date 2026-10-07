package dev.pilltracker.ui.format

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Locale's own hour/minute pattern, in the device's 12/24h setting. */
fun timeFormatter(
    locale: Locale,
    is24Hour: Boolean,
): DateTimeFormatter {
    val skeleton = if (is24Hour) "Hm" else "hma"
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}

/** Reads locale from [LocalConfiguration] so previews and locale-overridden tests follow it. */
@Composable
fun rememberTimeFormatter(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return remember(locale, is24Hour) { timeFormatter(locale, is24Hour) }
}
