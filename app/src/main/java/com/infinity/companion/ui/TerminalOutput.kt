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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Scrollable monospace terminal log output.
 */
@Composable
fun TerminalOutput(
    lines: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .testTag("TerminalOutput")
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            items(lines, key = { it }) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.testTag("TerminalLine")
                )
            }
        }
    }
}
