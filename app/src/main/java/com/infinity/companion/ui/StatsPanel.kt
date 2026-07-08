package com.infinity.companion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.infinity.companion.ui.model.DeviceStats

/**
 * Panel that displays notification and execution counters.
 */
@Composable
fun StatsPanel(
    stats: DeviceStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .testTag("StatsPanel")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Notifications: ${stats.notificationCount}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Executions: ${stats.executionCount}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
