package dev.pilltracker

import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test

/** Setup -> Home (Taken + month grid) through the real activity, Hilt graph and NavHost. */
class NavigationTest {
    @get:Rule(order = 0)
    val notifications: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    private fun str(id: Int) = rule.activity.getString(id)

    @Test fun enableLeadsHomeThenTakenWithMonthGrid() {
        // The start screen appears once settings are read from Room, which Compose tests don't wait for.
        rule.waitUntil(
            5_000,
        ) { rule.onAllNodesWithText(str(R.string.enable_reminders)).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(str(R.string.enable_reminders)).performClick()
        rule.onNodeWithText(str(R.string.taken)).performClick()
        rule.onNodeWithText(str(R.string.taken_today)).assertExists()
        rule.onNodeWithContentDescription(str(R.string.next_month)).assertExists()
    }

    /** Reset deletes everything and lands on Setup (AC7.3). */
    @Test fun resetFromSettingsReturnsToSetup() {
        rule.waitUntil(
            5_000,
        ) { rule.onAllNodesWithText(str(R.string.enable_reminders)).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(str(R.string.enable_reminders)).performClick()
        rule.onNodeWithContentDescription(str(R.string.settings)).performClick()
        rule.waitUntil(
            5_000,
        ) { rule.onAllNodesWithText(str(R.string.delete_all_data)).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(str(R.string.delete_all_data)).performClick()
        rule.onNodeWithText(str(R.string.delete)).performClick()
        rule.waitUntil(
            5_000,
        ) { rule.onAllNodesWithText(str(R.string.enable_reminders)).fetchSemanticsNodes().isNotEmpty() }
    }
}
