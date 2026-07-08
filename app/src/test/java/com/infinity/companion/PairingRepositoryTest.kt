package com.infinity.companion

import com.infinity.companion.data.auth.AuthRepository
import com.infinity.companion.data.pairing.DeviceDto
import com.infinity.companion.data.pairing.PairingCodeDto
import com.infinity.companion.data.pairing.PairingRepository
import com.infinity.companion.data.pairing.PairingRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val testToken = "test-access-token"

    private val desktopDevice = DeviceDto(
        id = "11111111-1111-1111-1111-111111111111",
        user_id = "22222222-2222-2222-2222-222222222222",
        name = "Desktop",
        type = "desktop",
        created_at = "2024-01-01T00:00:00Z",
        updated_at = "2024-01-01T00:00:00Z"
    )

    private val mobileDevice = DeviceDto(
        id = "33333333-3333-3333-3333-333333333333",
        user_id = "22222222-2222-2222-2222-222222222222",
        name = "Mobile",
        type = "mobile",
        paired_device_id = desktopDevice.id,
        created_at = "2024-01-01T00:00:00Z",
        updated_at = "2024-01-01T00:00:00Z"
    )

    private val pairingCode = PairingCodeDto(
        code = "123456",
        device_id = desktopDevice.id,
        expires_at = "2099-01-01T00:00:00Z"
    )

    private fun createRepository(
        engine: MockEngine,
        storage: InMemoryTokenStorage = InMemoryTokenStorage().apply { accessToken = testToken }
    ): PairingRepository {
        val authClient = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
        val pairingClient = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
        val authRepository = AuthRepository(storage, authClient, "https://api.example.com")
        return PairingRepositoryImpl(authRepository, pairingClient, "https://api.example.com")
    }

    @Test
    fun getPairingCode_returnsPairingCode() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath == "/devices/${desktopDevice.id}/pairing-code" -> {
                    respond(
                        content = """{"code":"${pairingCode.code}","device_id":"${pairingCode.device_id}","expires_at":"${pairingCode.expires_at}"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                    )
                }
                else -> respond("Not found", HttpStatusCode.NotFound)
            }
        }

        val repository = createRepository(engine)
        val result = repository.getPairingCode(desktopDevice.id)

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertEquals(pairingCode.code, response.code)
        assertEquals(desktopDevice.id, response.device_id)
    }

    @Test
    fun getPairingCode_authorizationHeaderContainsBearerToken() = runTest {
        var capturedAuthHeader: String? = null
        val engine = MockEngine { request ->
            capturedAuthHeader = request.headers["Authorization"]
            respond(
                content = """{"code":"000000","device_id":"${desktopDevice.id}","expires_at":"2099-01-01T00:00:00Z"}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString())
            )
        }

        val repository = createRepository(engine)
        repository.getPairingCode(desktopDevice.id)

        assertEquals("Bearer $testToken", capturedAuthHeader)
    }

    @Test
    fun requestPair_sendsCodeAndMobileDeviceId() = runTest {
        var capturedBody: String? = null
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath == "/devices/${desktopDevice.id}/pair" -> {
                    capturedBody = (request.body as? TextContent)?.text
                    respond(
                        content = """{"target":${json.encodeToString(DeviceDto.serializer(), desktopDevice.copy(paired_device_id = mobileDevice.id))},"mobile":${json.encodeToString(DeviceDto.serializer(), mobileDevice)}}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                    )
                }
                else -> respond("Not found", HttpStatusCode.NotFound)
            }
        }

        val repository = createRepository(engine)
        val result = repository.requestPair(
            deviceId = desktopDevice.id,
            code = pairingCode.code,
            mobileDeviceId = mobileDevice.id
        )

        assertTrue(result.isSuccess)
        assertTrue(capturedBody!!.contains("\"code\":\"${pairingCode.code}\""))
        assertTrue(capturedBody!!.contains("\"device_id\":\"${mobileDevice.id}\""))
        val response = result.getOrThrow()
        assertEquals(mobileDevice.id, response.target.paired_device_id)
        assertEquals(desktopDevice.id, response.mobile.paired_device_id)
    }

    @Test
    fun getDeviceStatus_returnsDeviceFromList() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath == "/devices" -> {
                    respond(
                        content = json.encodeToString(
                            ListSerializer(DeviceDto.serializer()),
                            listOf(desktopDevice, mobileDevice)
                        ),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                    )
                }
                else -> respond("Not found", HttpStatusCode.NotFound)
            }
        }

        val repository = createRepository(engine)
        val result = repository.getDeviceStatus(mobileDevice.id)

        assertTrue(result.isSuccess)
        val device = result.getOrThrow()
        assertEquals(mobileDevice.id, device.id)
        assertEquals(desktopDevice.id, device.paired_device_id)
    }

    @Test
    fun getDeviceStatus_returnsFailureWhenDeviceNotInList() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath == "/devices" -> {
                    respond(
                        content = "[]",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                    )
                }
                else -> respond("Not found", HttpStatusCode.NotFound)
            }
        }

        val repository = createRepository(engine)
        val result = repository.getDeviceStatus(mobileDevice.id)

        assertTrue(result.isFailure)
    }

    @Test
    fun getPairingCode_returnsFailureOn401() = runTest {
        val engine = MockEngine {
            respondError(HttpStatusCode.Unauthorized)
        }

        val repository = createRepository(engine)
        val result = repository.getPairingCode(desktopDevice.id)

        assertTrue(result.isFailure)
    }

    @Test
    fun unpair_returnsFailureOn404() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath == "/devices/${desktopDevice.id}/unpair" -> {
                    respond(
                        content = """{"error":"Device not found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                    )
                }
                else -> respond("Not found", HttpStatusCode.NotFound)
            }
        }

        val repository = createRepository(engine)
        val result = repository.unpair(desktopDevice.id)

        assertTrue(result.isFailure)
    }

    @Test
    fun unpair_succeedsOn200() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath == "/devices/${mobileDevice.id}/unpair" -> {
                    respond(
                        content = """{"success":true}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                    )
                }
                else -> respond("Not found", HttpStatusCode.NotFound)
            }
        }

        val repository = createRepository(engine)
        val result = repository.unpair(mobileDevice.id)

        assertTrue(result.isSuccess)
    }

    @Test
    fun operations_failWhenNotAuthenticated() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        val storage = InMemoryTokenStorage()
        val repository = createRepository(engine, storage)

        assertFalse(repository.getPairingCode(desktopDevice.id).isSuccess)
        assertFalse(repository.requestPair(desktopDevice.id, pairingCode.code).isSuccess)
        assertFalse(repository.getDeviceStatus(desktopDevice.id).isSuccess)
        assertFalse(repository.unpair(desktopDevice.id).isSuccess)
    }
}
