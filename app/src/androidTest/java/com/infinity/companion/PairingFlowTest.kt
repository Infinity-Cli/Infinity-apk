package com.infinity.companion

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test that verifies the main dashboard Activity launches and
 * reaches at least the CREATED lifecycle state, proving the Compose UI and
 * ViewModel wiring do not crash at startup.
 */
@RunWith(AndroidJUnit4::class)
class PairingFlowTest {

    @Test
    fun dashboardActivityLaunchesSuccessfully() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(
                    "MainActivity should be created or resumed",
                    activity.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)
                )
            }
        }
    }
}
