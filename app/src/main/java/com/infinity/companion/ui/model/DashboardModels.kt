package com.infinity.companion.ui.model

import kotlinx.serialization.Serializable

/**
 * Update about a remote command execution received from Infinity-exe.
 */
@Serializable
data class ExecutionUpdate(
    val status: String,
    val command: String,
    val stdout: String? = null
)

/**
 * Terminal stdout line received from Infinity-exe.
 */
@Serializable
data class TerminalUpdate(
    val stdout: String
)

/**
 * Unified diff payload received from Infinity-exe.
 */
@Serializable
data class DiffUpdate(
    val diff: String
)

/**
 * Request for the user to approve or deny a sensitive command.
 */
@Serializable
data class ApprovalRequest(
    val requestId: String,
    val command: String
)

/**
 * Simple counters for the stats panel.
 */
@Serializable
data class DeviceStats(
    val notificationCount: Int = 0,
    val executionCount: Int = 0
)
