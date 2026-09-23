package com.android.systemui.core.common

import org.junit.Assert.assertTrue
import org.junit.Test

class ActionResultTest {
    @Test
    fun successIsRepresentedAsSuccess() {
        assertTrue(ActionResult.Success is ActionResult)
    }
}
