package com.infinity.companion.data.relay

import kotlinx.serialization.Serializable

/**
 * Remote control command sent from the Android companion to the paired desktop.
 *
 * Supported types: pause, resume, stop, approve, deny.
 */
@Serializable
data class ControlMessage(
    val type: String,
    val executionId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
