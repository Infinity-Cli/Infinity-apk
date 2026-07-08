package com.infinity.companion.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.infinity.companion.R
import com.infinity.companion.ui.model.ApprovalRequest

/**
 * Dialog that asks the user to approve or deny a sensitive command.
 */
@Composable
fun ApprovalDialog(
    request: ApprovalRequest?,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (request != null) {
        AlertDialog(
            onDismissRequest = { /* approvals must be explicit */ },
            title = { Text(stringResource(R.string.approval_dialog_title)) },
            text = { Text(request.command) },
            confirmButton = {
                Button(onClick = onApprove) {
                    Text(stringResource(R.string.approval_dialog_approve))
                }
            },
            dismissButton = {
                Button(onClick = onDeny) {
                    Text(stringResource(R.string.approval_dialog_deny))
                }
            },
            modifier = modifier.testTag("ApprovalDialog")
        )
    }
}
