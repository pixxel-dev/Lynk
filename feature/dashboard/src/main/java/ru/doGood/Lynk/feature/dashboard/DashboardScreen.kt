package ru.doGood.Lynk.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lynk.core.domain.backlog.BacklogItem
import com.example.lynk.core.domain.approval.ApprovalRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardListScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showAuditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Dashboard Backlog") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Button(
                onClick = { 
                    viewModel.runAudit()
                    showAuditDialog = true 
                },
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Text("Run Architectural Audit")
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.backlogItems) { item ->
                    BacklogItemRow(
                        item = item,
                        onClick = { onNavigateToDetail(item.id) }
                    )
                }
            }
        }
    }

    if (showAuditDialog) {
        AlertDialog(
            onDismissRequest = { showAuditDialog = false },
            title = { Text("Audit Results") },
            text = {
                Column {
                    state.auditResults.forEach { result ->
                        Text("Rule: ${result.ruleName}")
                        Text("Passed: ${result.isPassed}")
                        Text("Message: ${result.message}")
                        Text("---")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAuditDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun BacklogItemRow(item: BacklogItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
            Text(text = "Priority: ${item.priority}", style = MaterialTheme.typography.bodyMedium)
            if (item.isCompleted) {
                Text(text = "Completed", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardDetailScreen(
    id: String,
    viewModel: DashboardViewModel = viewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val item = state.backlogItems.find { it.id == id }
    val request = state.approvalRequests.find { it.id == id }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Detail: $id") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (item != null) {
                Text("Task: ${item.title}", style = MaterialTheme.typography.headlineMedium)
                Text("Description: ${item.description}", style = MaterialTheme.typography.bodyLarge)
                Button(
                    onClick = { viewModel.completeBacklogItem(item.id); onBack() },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Mark Completed")
                }
            } else if (request != null) {
                Text("Approval: ${request.details}", style = MaterialTheme.typography.headlineMedium)
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    Button(onClick = { viewModel.approveRequest(request.id); onBack() }) {
                        Text("Approve")
                    }
                    Button(
                        onClick = { viewModel.rejectRequest(request.id); onBack() },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Reject")
                    }
                }
            } else {
                Text("Item not found", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
