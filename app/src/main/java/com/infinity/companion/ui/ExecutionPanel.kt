package com.infinity.companion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.infinity.companion.R
import com.infinity.companion.ui.model.ExecutionUpdate

/**
 * Panel that shows the current execution status and progress.
 */
@Composable
fun ExecutionPanel(
    execution: ExecutionUpdate?,
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onStop: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .testTag("ExecutionPanel")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (execution == null) {
                Text(
                    text = "No active execution",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text(
                    text = "Status: ${execution.status}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Command: ${execution.command}",
                    style = MaterialTheme.typography.bodyMedium
                )
                execution.stdout?.let { output ->
                    Text(
                        text = output,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (execution.status.equals("running", ignoreCase = true)) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Button(
                            onClick = onPause,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.execution_pause))
                        }
                        Button(
                            onClick = onResume,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(stringResource(R.string.execution_resume))
                        }
                        Button(
                            onClick = onStop,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.execution_stop))
                        }
                    }
                }
            }
        }
    }
}
