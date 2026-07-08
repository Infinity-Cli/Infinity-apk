package com.infinity.companion.data.relay

import android.content.Context
import com.infinity.companion.data.auth.AuthRepository
import com.infinity.companion.data.crypto.E2EECrypto
import com.infinity.companion.data.crypto.EncryptedPayload
import com.infinity.companion.data.pairing.RelayMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.parameter
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Listener for plaintext relay messages received from Infinity-api.
 */
fun interface RelayMessageListener {
    fun onMessage(sourceDeviceId: String, payload: String)
}

/**
 * Manager that maintains a WebSocket connection to Infinity-api `/ws` for
 * relaying encrypted payloads between paired devices.
 */
interface RelaySocketManager {

    /**
     * Flow of transient error messages emitted by the socket.
     */
    val errors: Flow<String>

    /**
     * Registers a listener that will receive every incoming relay message.
     */
    fun addListener(listener: RelayMessageListener)

    /**
     * Unregisters a previously added listener.
     */
    fun removeListener(listener: RelayMessageListener)

    /**
     * Opens the WebSocket using the stored access token and optional device id.
     */
    fun connect(deviceId: String? = null)

    /**
     * Closes the active WebSocket connection.
     */
    fun disconnect()

    /**
     * Sends a relay message to [targetDeviceId].
     */
    suspend fun send(targetDeviceId: String, payload: String): Result<Unit>

    /**
     * Flow of decrypted payloads received from the relay.
     */
    val decryptedMessages: Flow<String>

    /**
     * Sets the paired desktop's public key PEM used to encrypt outgoing messages.
     */
    fun setDesktopPublicKey(publicKeyPem: String)

    /**
     * Sets the target device id used for outgoing control messages.
     */
    fun setTargetDeviceId(deviceId: String)

    /**
     * Sends an encrypted [ControlMessage] to the paired desktop.
     */
    suspend fun sendControl(control: ControlMessage): Result<Unit>
}

/**
 * Default [RelaySocketManager] implementation backed by Ktor CIO WebSockets.
 *
 * For this step messages are handled as plaintext JSON strings; encryption will
 * be layered on top in a later step.
 */
class RelaySocketManagerImpl(
    private val authRepository: AuthRepository,
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val externalScope: CoroutineScope,
    private val e2eeCrypto: E2EECrypto = E2EECrypto.create()
) : RelaySocketManager {

    constructor(
        context: Context,
        authRepository: AuthRepository,
        baseUrl: String = AuthRepository.DEFAULT_BASE_URL,
        externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO + Job())
    ) : this(
        authRepository = authRepository,
        httpClient = HttpClient(CIO) {
            install(WebSockets)
        },
        baseUrl = baseUrl,
        externalScope = externalScope,
        e2eeCrypto = E2EECrypto.create()
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val errors = _errors.asSharedFlow()

    private val _decryptedMessages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val decryptedMessages = _decryptedMessages.asSharedFlow()

    private val listeners = mutableListOf<RelayMessageListener>()
    private var connectionJob: Job? = null
    @Volatile private var desktopPublicKeyPem: String? = null
    @Volatile private var targetDeviceId: String? = null

    override fun addListener(listener: RelayMessageListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    override fun removeListener(listener: RelayMessageListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    override fun connect(deviceId: String?) {
        disconnect()
        val token = authRepository.getAccessToken() ?: run {
            _errors.tryEmit("Not authenticated")
            return
        }

        connectionJob = externalScope.launch {
            try {
                httpClient.webSocket(
                    request = {
                        url {
                            protocol = resolveProtocol()
                            host = resolveHost()
                            port = resolvePort()
                            path("/ws")
                        }
                        parameter("token", token)
                        deviceId?.let { parameter("device_id", it) }
                    }
                ) {
                    while (isActive) {
                        val frame = try {
                            incoming.receive()
                        } catch (_: ClosedReceiveChannelException) {
                            break
                        }
                        if (frame is Frame.Text) {
                            dispatch(frame.readText())
                        }
                    }
                }
            } catch (_: CancellationException) {
                // Expected on disconnect.
            } catch (e: Throwable) {
                _errors.tryEmit(e.message ?: "WebSocket error")
            }
        }
    }

    override suspend fun send(targetDeviceId: String, payload: String): Result<Unit> = runCatching {
        val message = RelayMessage(
            targetDeviceId = targetDeviceId,
            encryptedPayload = Json.parseToJsonElement(payload)
        )
        val token = authRepository.getAccessToken() ?: error("Not authenticated")
        httpClient.webSocket(
            request = {
                url {
                    protocol = resolveProtocol()
                    host = resolveHost()
                    port = resolvePort()
                    path("/ws")
                }
                parameter("token", token)
            }
        ) {
            send(json.encodeToString(RelayMessage.serializer(), message))
        }
    }

    override fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
    }

    override fun setDesktopPublicKey(publicKeyPem: String) {
        desktopPublicKeyPem = publicKeyPem
    }

    override fun setTargetDeviceId(deviceId: String) {
        targetDeviceId = deviceId
    }

    override suspend fun sendControl(control: ControlMessage): Result<Unit> = runCatching {
        val keyPem = desktopPublicKeyPem ?: error("Desktop public key not set")
        val target = targetDeviceId ?: error("Target device id not set")
        val plaintext = json.encodeToString(ControlMessage.serializer(), control)
        val encrypted = e2eeCrypto.encrypt(plaintext, keyPem)
        val payload = json.encodeToString(EncryptedPayload.serializer(), encrypted)
        send(target, payload).getOrThrow()
    }

    private fun dispatch(raw: String) {
        val envelope = runCatching {
            json.decodeFromString<ServerRelayEnvelope>(raw)
        }.getOrNull()

        val encryptedPayloadString = envelope?.encryptedPayload?.toString() ?: raw

        val decrypted = runCatching {
            val payload = json.decodeFromString<EncryptedPayload>(encryptedPayloadString)
            e2eeCrypto.decrypt(payload)
        }.getOrNull()

        val sourceDeviceId = envelope?.sourceDeviceId ?: "unknown"
        val payload = decrypted ?: (envelope?.encryptedPayload?.toString() ?: raw)

        decrypted?.let { _decryptedMessages.tryEmit(it) }

        synchronized(listeners) {
            listeners.forEach { it.onMessage(sourceDeviceId, payload) }
        }
    }

    private fun resolveHost(): String {
        val url = io.ktor.http.Url(baseUrl)
        return url.host
    }

    private fun resolvePort(): Int {
        val url = io.ktor.http.Url(baseUrl)
        return url.port
    }

    private fun resolveProtocol(): URLProtocol {
        val url = io.ktor.http.Url(baseUrl)
        return when (url.protocol.name) {
            "https" -> URLProtocol.WSS
            else -> URLProtocol.WS
        }
    }
}

/**
 * Server-side envelope for relayed messages.
 */
@kotlinx.serialization.Serializable
internal data class ServerRelayEnvelope(
    val sourceDeviceId: String,
    val encryptedPayload: kotlinx.serialization.json.JsonElement
)
