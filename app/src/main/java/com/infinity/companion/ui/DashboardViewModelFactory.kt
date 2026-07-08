package com.infinity.companion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.infinity.companion.data.pairing.PairingRepository
import com.infinity.companion.data.relay.RelaySocketManager

/**
 * Factory that creates a [DashboardViewModel] with runtime dependencies.
 */
class DashboardViewModelFactory(
    private val deviceId: String,
    private val pairingRepository: PairingRepository,
    private val relaySocketManager: RelaySocketManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DashboardViewModel::class.java))
        return DashboardViewModel(deviceId, pairingRepository, relaySocketManager) as T
    }
}
