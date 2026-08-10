package com.example.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SkillTreeViewModelTest {

    private lateinit var appDatabase: AppDatabase
    private lateinit var gameDatabase: GameDatabase
    private lateinit var repository: DataRepository
    private lateinit var viewModel: SkillTreeViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        appDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        gameDatabase = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = DataRepository(
            characterDao = appDatabase.characterDao(),
            inventoryDao = appDatabase.inventoryDao(),
            skillDao = appDatabase.skillDao(),
            saveStateDao = gameDatabase.saveStateDao()
        )

        viewModel = SkillTreeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        appDatabase.close()
        gameDatabase.close()
    }

    @Test
    fun skillTreeViewModel_hasPredefinedSkillsGraph() {
        val skills = viewModel.allSkills
        assertTrue(skills.isNotEmpty())

        val tier1 = skills.filter { it.tier == 1 }
        val tier2 = skills.filter { it.tier == 2 }
        val tier3 = skills.filter { it.tier == 3 }

        assertTrue(tier1.isNotEmpty())
        assertTrue(tier2.isNotEmpty())
        assertTrue(tier3.isNotEmpty())

        // Verify tier 2 skills have tier 1 prerequisites
        val empBurst = skills.find { it.id == "emp_burst" }
        assertNotNull(empBurst)
        assertTrue(empBurst!!.prerequisites.contains("tactical_lens"))
    }

    @Test
    fun selectSkill_updatesUiState() {
        viewModel.selectSkill("emp_burst")
        assertEquals("emp_burst", viewModel.uiState.value.selectedSkillId)
    }
}
