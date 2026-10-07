package dev.pilltracker

import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test

/** Setup -> Home -> History through the real activity, Hilt graph and NavHost. */
class NavigationTest {
    @get:Rule(order = 0)
    val notifications: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    private fun str(id: Int) = rule.activity.getString(id)

    @Test fun enableLeadsHomeThenTakenThenHistory() {
        rule.onNodeWithText(str(R.string.enable_reminders)).performClick()
        rule.onNodeWithText(str(R.string.taken)).performClick()
        rule.onNodeWithText(str(R.string.taken_today)).assertExists()
        rule.onNodeWithText(str(R.string.history)).performClick()
        rule.onNodeWithContentDescription(str(R.string.next_month)).assertExists()
    }
}
