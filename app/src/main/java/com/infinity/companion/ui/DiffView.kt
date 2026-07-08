package com.infinity.companion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.infinity.companion.ui.model.DiffUpdate

/**
 * Renders a unified diff with add/remove highlighting.
 */
@Composable
fun DiffView(
    diffUpdate: DiffUpdate?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .testTag("DiffView")
    ) {
        if (diffUpdate == null) {
            Text(
                text = "No diff",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                items(diffUpdate.diff.lines()) { line ->
                    val background = when {
                        line.startsWith("+") -> Color(0xFFCCFFCC)
                        line.startsWith("-") -> Color(0xFFFFCCCC)
                        else -> Color.Transparent
                    }
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(background)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
