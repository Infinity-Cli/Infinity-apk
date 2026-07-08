package com.infinity.companion.data.pairing

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Infinity-api device record.
 *
 * Mirrors the `Device` type in Infinity-api/src/types.ts.
 */
@Serializable
data class DeviceDto(
    val id: String,
    val user_id: String,
    val name: String,
    val type: String = "unknown",
    val fcm_token: String? = null,
    val public_key: String? = null,
    val paired_device_id: String? = null,
    val paired_at: String? = null,
    val created_at: String,
    val updated_at: String
)

/**
 * Infinity-api pairing code record.
 *
 * Mirrors the `PairingCode` type in Infinity-api/src/types.ts.
 */
@Serializable
data class PairingCodeDto(
    val code: String,
    val device_id: String,
    val user_id: String? = null,
    val expires_at: String,
    val used_at: String? = null
)

/**
 * Body sent to `POST /devices/:id/pair`.
 *
 * Mirrors the extended `pairBodySchema` in Infinity-api/src/devices/index.ts.
 */
@Serializable
internal data class PairRequest(
    val code: String,
    val device_id: String? = null
)

/**
 * Response from `POST /devices/:id/pair`.
 */
@Serializable
data class PairResponse(
    val target: DeviceDto,
    val mobile: DeviceDto
)

/**
 * Response from `POST /devices/:id/unpair`.
 */
@Serializable
internal data class UnpairResponse(
    val success: Boolean
)

/**
 * Relay message sent over the WebSocket.
 *
 * Mirrors the `RelayMessage` type in Infinity-api/src/ws/index.ts.
 */
@Serializable
data class RelayMessage(
    val targetDeviceId: String,
    val encryptedPayload: JsonElement
)
