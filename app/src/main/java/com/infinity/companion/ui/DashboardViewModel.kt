package com.infinity.companion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infinity.companion.data.pairing.DeviceDto
import com.infinity.companion.data.pairing.PairingRepository
import com.infinity.companion.data.relay.ControlMessage
import com.infinity.companion.data.relay.RelayMessageListener
import com.infinity.companion.data.relay.RelaySocketManager
import com.infinity.companion.ui.model.ApprovalRequest
import com.infinity.companion.ui.model.DeviceStats
import com.infinity.companion.ui.model.DiffUpdate
import com.infinity.companion.ui.model.ExecutionUpdate
import com.infinity.companion.ui.model.TerminalUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * UI state rendered by [DashboardScreen].
 */
data class DashboardUiState(
    val device: DeviceDto? = null,
    val selectedTab: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val executionUpdate: ExecutionUpdate? = null,
    val terminalLines: List<String> = emptyList(),
    val diffUpdate: DiffUpdate? = null,
    val approvalRequest: ApprovalRequest? = null,
    val notifications: List<String> = emptyList(),
    val stats: DeviceStats = DeviceStats()
)

/**
 * ViewModel that ties [PairingRepository] and [RelaySocketManager] together into
 * a single dashboard UI state.
 */
class DashboardViewModel(
    private val deviceId: String,
    private val pairingRepository: PairingRepository,
    private val relaySocketManager: RelaySocketManager
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val relayListener = RelayMessageListener { _, payload ->
        handlePayload(payload)
    }

    init {
        relaySocketManager.addListener(relayListener)
        relaySocketManager.connect(deviceId.takeIf { it.isNotBlank() })

        viewModelScope.launch {
            relaySocketManager.errors.collect { message ->
                _uiState.update { it.copy(error = message) }
            }
        }

        refreshDevice()
    }

    /**
     * Fetches the current device status from Infinity-api.
     */
    fun refreshDevice() {
        if (deviceId.isBlank()) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = pairingRepository.getDeviceStatus(deviceId)
            result.onSuccess { device ->
                device.paired_device_id?.let { pairedId ->
                    relaySocketManager.setTargetDeviceId(pairedId)
                    device.public_key?.let { key ->
                        relaySocketManager.setDesktopPublicKey(key)
                    }
                }
            }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(device = it, isLoading = false) },
                    onFailure = { state.copy(error = it.message, isLoading = false) }
                )
            }
        }
    }

    /**
     * Sends an approval response back to the paired desktop device.
     */
    fun approveCommand() {
        sendControl(
            ControlMessage(
                type = "approve",
                executionId = uiState.value.approvalRequest?.requestId
            )
        )
        _uiState.update { it.copy(approvalRequest = null) }
    }

    /**
     * Sends a denial response back to the paired desktop device.
     */
    fun denyCommand() {
        sendControl(
            ControlMessage(
                type = "deny",
                executionId = uiState.value.approvalRequest?.requestId
            )
        )
        _uiState.update { it.copy(approvalRequest = null) }
    }

    /**
     * Sends a pause command to the paired desktop device.
     */
    fun pauseExecution() {
        sendControl(ControlMessage(type = "pause"))
    }

    /**
     * Sends a resume command to the paired desktop device.
     */
    fun resumeExecution() {
        sendControl(ControlMessage(type = "resume"))
    }

    /**
     * Sends a stop command to the paired desktop device.
     */
    fun stopExecution() {
        sendControl(ControlMessage(type = "stop"))
    }

    private fun sendControl(control: ControlMessage) {
        viewModelScope.launch {
            val result = relaySocketManager.sendControl(control)
            result.onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }
        }
    }

    /**
     * Breaks the current device pairing.
     */
    fun unpair() {
        if (deviceId.isBlank()) return
        viewModelScope.launch {
            val result = pairingRepository.unpair(deviceId)
            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(device = null) },
                    onFailure = { state.copy(error = it.message) }
                )
            }
        }
    }

    /**
     * Switches the currently selected dashboard tab.
     */
    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    /**
     * Clears the transient error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        relaySocketManager.removeListener(relayListener)
        relaySocketManager.disconnect()
        super.onCleared()
    }

    private fun handlePayload(payload: String) {
        val parsed = runCatching {
            json.parseToJsonElement(payload).jsonObject
        }.getOrNull()

        if (parsed == null) {
            appendNotification(payload)
            return
        }

        when {
            "requestId" in parsed -> {
                runCatching {
                    json.decodeFromString(ApprovalRequest.serializer(), payload)
                }.getOrNull()?.let { request ->
                    _uiState.update { it.copy(approvalRequest = request) }
                }
            }
            "status" in parsed -> {
                runCatching {
                    json.decodeFromString(ExecutionUpdate.serializer(), payload)
                }.getOrNull()?.let { update ->
                    _uiState.update { state ->
                        state.copy(
                            executionUpdate = update,
                            stats = state.stats.copy(
                                executionCount = state.stats.executionCount + 1
                            )
                        )
                    }
                }
            }
            "diff" in parsed -> {
                runCatching {
                    json.decodeFromString(DiffUpdate.serializer(), payload)
                }.getOrNull()?.let { update ->
                    _uiState.update { it.copy(diffUpdate = update) }
                }
            }
            "stdout" in parsed -> {
                runCatching {
                    json.decodeFromString(TerminalUpdate.serializer(), payload)
                }.getOrNull()?.let { update ->
                    appendTerminal(update.stdout)
                }
            }
            else -> appendNotification(payload)
        }
    }

    private fun appendTerminal(line: String) {
        _uiState.update { state ->
            state.copy(terminalLines = state.terminalLines + line)
        }
    }

    private fun appendNotification(payload: String) {
        _uiState.update { state ->
            state.copy(
                notifications = state.notifications + payload,
                stats = state.stats.copy(
                    notificationCount = state.stats.notificationCount + 1
                )
            )
        }
    }
}
