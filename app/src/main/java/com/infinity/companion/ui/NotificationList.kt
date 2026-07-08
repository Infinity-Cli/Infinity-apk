package com.infinity.companion.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Simple scrollable list of push/WebSocket notifications.
 */
@Composable
fun NotificationList(
    notifications: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .testTag("NotificationList")
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            items(notifications) { notification ->
                Text(
                    text = notification,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
