package com.infinity.companion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private enum class DashboardTab(val title: String) {
    DEVICE("Device"),
    EXECUTIONS("Executions"),
    TERMINAL("Terminal"),
    DIFF("Diff"),
    NOTIFICATIONS("Notifications"),
    STATS("Stats")
}

/**
 * Top-level dashboard with tabs for device status, executions, terminal, diff,
 * notifications, and stats.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Infinity Companion") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val tabs = DashboardTab.entries
            TabRow(selectedTabIndex = uiState.selectedTab) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = { Text(tab.title) }
                    )
                }
            }

            val contentModifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
                .testTag("DashboardContent")

            when (tabs[uiState.selectedTab]) {
                DashboardTab.DEVICE -> DeviceCard(
                    device = uiState.device,
                    onUnpair = viewModel::unpair,
                    modifier = contentModifier
                )
                DashboardTab.EXECUTIONS -> ExecutionPanel(
                    execution = uiState.executionUpdate,
                    onPause = viewModel::pauseExecution,
                    onResume = viewModel::resumeExecution,
                    onStop = viewModel::stopExecution,
                    modifier = contentModifier
                )
                DashboardTab.TERMINAL -> TerminalOutput(
                    lines = uiState.terminalLines,
                    modifier = contentModifier
                )
                DashboardTab.DIFF -> DiffView(
                    diffUpdate = uiState.diffUpdate,
                    modifier = contentModifier
                )
                DashboardTab.NOTIFICATIONS -> NotificationList(
                    notifications = uiState.notifications,
                    modifier = contentModifier
                )
                DashboardTab.STATS -> StatsPanel(
                    stats = uiState.stats,
                    modifier = contentModifier
                )
            }
        }
    }

    ApprovalDialog(
        request = uiState.approvalRequest,
        onApprove = viewModel::approveCommand,
        onDeny = viewModel::denyCommand
    )
}
