package com.infinity.companion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.infinity.companion.data.pairing.DeviceDto

/**
 * Card that displays a paired desktop device and its unpair action.
 */
@Composable
fun DeviceCard(
    device: DeviceDto?,
    onUnpair: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .testTag("DeviceCard")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (device == null) {
                Text(
                    text = "No paired device",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Type: ${device.type}",
                    style = MaterialTheme.typography.bodyMedium
                )
                val paired = !device.paired_device_id.isNullOrBlank()
                Text(
                    text = "Status: ${if (paired) "Paired" else "Unpaired"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                device.public_key?.let { key ->
                    Text(
                        text = "Fingerprint: ${key.take(16)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (paired) {
                    Button(
                        onClick = onUnpair,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Unpair")
                    }
                }
            }
        }
    }
}
