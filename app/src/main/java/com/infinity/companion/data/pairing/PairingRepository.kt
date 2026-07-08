package com.infinity.companion.data.pairing

import android.content.Context
import com.infinity.companion.data.auth.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Repository contract for device pairing operations against Infinity-api.
 *
 * Implementations reuse [AuthRepository] for access-token retrieval so token
 * storage is not duplicated. Endpoints mirror Infinity-api/src/devices/index.ts.
 */
interface PairingRepository {

    /**
     * Creates a new numeric pairing code for the given device.
     */
    suspend fun getPairingCode(deviceId: String): Result<PairingCodeDto>

    /**
     * Pairs the target device with another device using a pairing code.
     */
    suspend fun requestPair(
        deviceId: String,
        code: String,
        mobileDeviceId: String? = null
    ): Result<PairResponse>

    /**
     * Fetches the current user's device list and returns the requested device.
     */
    suspend fun getDeviceStatus(deviceId: String): Result<DeviceDto>

    /**
     * Removes a device and breaks its pairing.
     */
    suspend fun unpair(deviceId: String): Result<Unit>

    /**
     * Updates the device's public key on Infinity-api.
     */
    suspend fun updatePublicKey(deviceId: String, publicKeyPem: String): Result<Unit>

    /**
     * Fetches the current user's paired device list.
     */
    suspend fun getPairedDevices(): Result<List<DeviceDto>>
}

/**
 * Default [PairingRepository] implementation that talks to Infinity-api.
 */
class PairingRepositoryImpl(
    private val authRepository: AuthRepository,
    private val httpClient: HttpClient,
    private val baseUrl: String
) : PairingRepository {

    constructor(
        context: Context,
        authRepository: AuthRepository,
        baseUrl: String = AuthRepository.DEFAULT_BASE_URL
    ) : this(
        authRepository = authRepository,
        httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        },
        baseUrl = baseUrl
    )

    override suspend fun getPairingCode(deviceId: String): Result<PairingCodeDto> = runCatching {
        val response = httpClient.get("$baseUrl/devices/$deviceId/pairing-code") {
            auth()
        }
        response.throwOnError()
        response.body<PairingCodeDto>()
    }

    override suspend fun requestPair(
        deviceId: String,
        code: String,
        mobileDeviceId: String?
    ): Result<PairResponse> = runCatching {
        val response = httpClient.post("$baseUrl/devices/$deviceId/pair") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(PairRequest(code = code, device_id = mobileDeviceId))
        }
        response.throwOnError()
        response.body<PairResponse>()
    }

    override suspend fun getDeviceStatus(deviceId: String): Result<DeviceDto> = runCatching {
        val response = httpClient.get("$baseUrl/devices") {
            auth()
        }
        response.throwOnError()
        val devices = response.body<List<DeviceDto>>()
        devices.find { it.id == deviceId }
            ?: error("Device $deviceId not found")
    }

    override suspend fun unpair(deviceId: String): Result<Unit> = runCatching {
        val response = httpClient.post("$baseUrl/devices/$deviceId/unpair") {
            auth()
        }
        response.throwOnError()
    }

    override suspend fun updatePublicKey(
        deviceId: String,
        publicKeyPem: String
    ): Result<Unit> = runCatching {
        val response = httpClient.patch("$baseUrl/devices/$deviceId") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(PublicKeyUpdateRequest(public_key = publicKeyPem))
        }
        response.throwOnError()
    }

    override suspend fun getPairedDevices(): Result<List<DeviceDto>> = runCatching {
        val response = httpClient.get("$baseUrl/devices") {
            auth()
        }
        response.throwOnError()
        response.body<List<DeviceDto>>()
    }

    private fun HttpRequestBuilder.auth() = bearerAuth(
        authRepository.getAccessToken()
            ?: error("Not authenticated")
    )

    private fun HttpResponse.throwOnError() {
        if (!status.isSuccess()) {
            error("Request failed with HTTP ${status.value}")
        }
    }
}

/**
 * Body sent to `PATCH /devices/:id` to update the public key.
 */
@Serializable
data class PublicKeyUpdateRequest(
    val public_key: String
)

