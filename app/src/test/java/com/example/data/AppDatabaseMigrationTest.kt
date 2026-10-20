package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.neonmarshal.RoomNeonMarshalCampaignRepository
import com.example.neonmarshal.bridge.SemanticWorldEvent
import com.example.neonmarshal.bridge.TacticalSessionResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression coverage for the Stage-1 data bugs:
 *  - fresh installs had no neonmarshal_session_results table (entity was
 *    missing from AppDatabase, the table only existed in MIGRATION_8_9);
 *  - v9 databases created by old builds also lacked the table and must be
 *    repaired by MIGRATION_9_10.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppDatabaseMigrationTest {

    /** Legacy v9 schema replica: AppDatabase entities before the NeonMarshal entity was registered. */
    @Database(
        entities = [
            PlayerCharacter::class,
            PlayerStatsEntity::class,
            InventoryItemEntity::class,
            InventoryItem::class,
            UnlockedSkillEntity::class,
            Skill::class,
            PlayerSaveState::class,
            CharacterStatusEffectEntity::class
        ],
        version = 9,
        exportSchema = false
    )
    abstract class AppDatabaseV9 : RoomDatabase()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        context.deleteDatabase(LEGACY_DB_NAME)
    }

    @Test
    fun freshInstall_commitAndRestoreThroughRepository() {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = RoomNeonMarshalCampaignRepository(db) { 100L }

            assertNull(repository.restoreLastResult())

            val result = sampleResult(sessionId = "fresh-1")
            repository.commitResult(result)

            assertEquals(result, repository.restoreLastResult())
        } finally {
            db.close()
        }
    }

    @Test
    fun restoreLastResult_returnsMostRecentlySavedSession() {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            var now = 0L
            val repository = RoomNeonMarshalCampaignRepository(db) { now }
            val older = sampleResult(sessionId = "session-old")
            val newer = sampleResult(sessionId = "session-new")

            now = 10L
            repository.commitResult(older)
            now = 20L
            repository.commitResult(newer)

            assertEquals(newer, repository.restoreLastResult())
        } finally {
            db.close()
        }
    }

    @Test
    fun migration9To10_repairsLegacyV9DatabaseWithoutNeonMarshalTable() {
        context.deleteDatabase(LEGACY_DB_NAME)

        // Build a legacy v9 database exactly like old installs: schema from
        // the old entity list, so neonmarshal_session_results does not exist.
        val legacy = Room.databaseBuilder(context, AppDatabaseV9::class.java, LEGACY_DB_NAME)
            .allowMainThreadQueries()
            .build()
        legacy.openHelper.writableDatabase
        legacy.close()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, LEGACY_DB_NAME)
            .allowMainThreadQueries()
            .addMigrations(AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10)
            .build()
        try {
            val repository = RoomNeonMarshalCampaignRepository(db) { 7L }
            val result = sampleResult(sessionId = "migrated-1")

            repository.commitResult(result)

            assertEquals(result, repository.restoreLastResult())
        } finally {
            db.close()
        }
    }

    private fun sampleResult(sessionId: String) = TacticalSessionResult(
        sessionId = sessionId,
        worldRevision = 5L,
        outcome = "extracted",
        emittedEvents = listOf(
            SemanticWorldEvent(
                eventId = "event-1",
                worldId = "world-1",
                revision = 6L,
                kind = "front_pressure",
                payloadJson = "{}"
            )
        ),
        rewardCredits = 10,
        rewardXp = 20
    )

    private companion object {
        const val LEGACY_DB_NAME = "app_database_migration_test"
    }
}
