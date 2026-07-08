package com.infinity.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.infinity.companion.data.auth.AuthRepository
import com.infinity.companion.data.pairing.PairingRepositoryImpl
import com.infinity.companion.data.relay.RelaySocketManagerImpl
import com.infinity.companion.ui.DashboardScreen
import com.infinity.companion.ui.DashboardViewModel
import com.infinity.companion.ui.DashboardViewModelFactory
import com.infinity.companion.ui.theme.InfinityTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authRepository = AuthRepository(this)
        val pairingRepository = PairingRepositoryImpl(this, authRepository)
        val relaySocketManager = RelaySocketManagerImpl(this, authRepository)
        // The mobile device id is not known at startup; the dashboard will stay
        // in an unpaired state until a real device id is provided.
        val deviceId = ""

        val factory = DashboardViewModelFactory(
            deviceId = deviceId,
            pairingRepository = pairingRepository,
            relaySocketManager = relaySocketManager
        )
        val viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]

        setContent {
            InfinityTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
    }
}
