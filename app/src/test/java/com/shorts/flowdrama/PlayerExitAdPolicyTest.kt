package com.shorts.flowdrama

import org.junit.Assert.*
import org.junit.Test

class PlayerExitAdPolicyTest {
    @Test fun shortVisitsAndUninitializedVisitsDoNotShowAnAd() {
        assertFalse(playerExitAdEligible(0, 100_000))
        assertFalse(playerExitAdEligible(1_000, 30_999))
        assertTrue(playerExitAdEligible(1_000, 31_000))
    }
}
