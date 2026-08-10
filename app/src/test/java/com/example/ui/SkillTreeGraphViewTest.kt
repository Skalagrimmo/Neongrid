package com.example.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.PlayerStatsEntity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SkillTreeGraphViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleSkills = listOf(
        SkillNodeData(
            id = "tactical_lens",
            name = "Tactical Lens",
            description = "Highlights enemy line of sight.",
            tier = 1,
            cost = 1,
            prerequisites = emptyList()
        ),
        SkillNodeData(
            id = "emp_burst",
            name = "EMP Pulse",
            description = "Stuns nearby cyborgs.",
            tier = 2,
            cost = 2,
            prerequisites = listOf("tactical_lens")
        )
    )

    @Test
    fun skillTreeGraphContent_rendersGraphAndSelectedSkill() {
        var clickedSkillId: String? = null

        composeTestRule.setContent {
            SkillTreeGraphContent(
                allSkills = sampleSkills,
                unlockedSkillIds = setOf("tactical_lens"),
                characterStats = PlayerStatsEntity(id = 1, skillPoints = 4),
                selectedSkillId = "emp_burst",
                message = null,
                isError = false,
                onSelectSkill = { clickedSkillId = it },
                onUnlockSkill = {},
                onResetSkills = {}
            )
        }

        composeTestRule.onNodeWithTag("skill_tree_graph_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("skill_points_counter").assertIsDisplayed()
        composeTestRule.onNodeWithTag("selected_skill_detail_panel").assertIsDisplayed()

        // Verify tier skills are displayed
        composeTestRule.onNodeWithTag("skill_node_tactical_lens").assertIsDisplayed()
        composeTestRule.onNodeWithTag("skill_node_emp_burst").assertIsDisplayed()

        // Perform node click
        composeTestRule.onNodeWithTag("skill_node_tactical_lens").performClick()
        assert(clickedSkillId == "tactical_lens")
    }
}
