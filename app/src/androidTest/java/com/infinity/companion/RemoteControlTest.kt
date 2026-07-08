package com.infinity.companion

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.infinity.companion.data.crypto.E2EECrypto
import com.infinity.companion.data.relay.ControlMessage
import java.security.KeyPairGenerator
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for encrypted remote control message handling and the
 * dashboard Activity lifecycle wiring.
 */
@RunWith(AndroidJUnit4::class)
class RemoteControlTest {

    private fun generateKeyPair() = KeyPairGenerator
        .getInstance("RSA")
        .apply { initialize(2048) }
        .generateKeyPair()

    @Test
    fun e2eeRoundTrip() {
        val keyPair = generateKeyPair()
        val sender = E2EECrypto.createWithKeyPair(keyPair)
        val receiver = E2EECrypto.createWithKeyPair(keyPair)

        val message = ControlMessage(type = "approve", executionId = "exec-1")
        val plaintext = Json.encodeToString(ControlMessage.serializer(), message)

        val encrypted = sender.encrypt(plaintext, sender.getPublicKeyPem())
        val decrypted = receiver.decrypt(encrypted)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun wrongKeyDecryptionFails() {
        val keyPair = generateKeyPair()
        val firstCrypto = E2EECrypto.createWithKeyPair(keyPair)

        val message = ControlMessage(type = "approve", executionId = "exec-1")
        val plaintext = Json.encodeToString(ControlMessage.serializer(), message)

        val encrypted = firstCrypto.encrypt(plaintext, firstCrypto.getPublicKeyPem())

        val wrongKeyPair = generateKeyPair()
        val wrongCrypto = E2EECrypto.createWithKeyPair(wrongKeyPair)

        val result = runCatching { wrongCrypto.decrypt(encrypted) }
        assertTrue("Expected decryption with wrong private key to fail", result.isFailure)
    }

    @Test
    fun mainActivityLaunchesSuccessfully() {
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
