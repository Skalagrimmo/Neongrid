package com.example.neonmarshal

import com.example.neonmarshal.bridge.TacticalSessionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class NeonMarshalCampaignRepositoryTest {
    @Test
    fun committedResultCanBeRestoredAsDetachedState() {
        val repository = InMemoryNeonMarshalCampaignRepository()
        val original = TacticalSessionResult(
            sessionId = "run-1",
            worldRevision = 12L,
            outcome = "success",
            rewardCredits = 80,
            rewardXp = 40
        )

        repository.commitResult(original)
        val restored = repository.restoreLastResult()

        assertEquals(original, restored)
        assertNotSame(original, restored)
    }
}