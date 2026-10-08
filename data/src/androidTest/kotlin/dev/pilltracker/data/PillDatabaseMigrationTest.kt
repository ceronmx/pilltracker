package dev.pilltracker.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PillDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), PillDatabase::class.java)

    @Test fun v1To2KeepsSettingsAndDefaultsTheGuineaPig() {
        helper.createDatabase(DB, 1).use {
            it.execSQL("INSERT INTO settings VALUES (0, 'TWENTY_EIGHT', 20000, 540)")
            it.execSQL("INSERT INTO intake VALUES (20000, 1)")
        }
        helper.runMigrationsAndValidate(DB, 2, true).use { db ->
            db.query("SELECT packType, reminderMinuteOfDay, guineaPig FROM settings").use {
                it.moveToFirst()
                assertEquals("TWENTY_EIGHT", it.getString(0))
                assertEquals(540, it.getInt(1))
                assertEquals("TRICOLOR", it.getString(2))
            }
            db.query("SELECT COUNT(*) FROM intake").use {
                it.moveToFirst()
                assertEquals(1, it.getInt(0))
            }
        }
    }

    private companion object {
        const val DB = "migration-test"
    }
}
