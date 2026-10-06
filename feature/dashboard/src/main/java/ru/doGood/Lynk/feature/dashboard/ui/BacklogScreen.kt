package ru.doGood.Lynk.feature.dashboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Task
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lynk.core.domain.audit.AuditResult
import com.example.lynk.core.domain.backlog.BacklogItem
import ru.doGood.Lynk.feature.dashboard.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacklogScreen(
    backlogItems: List<BacklogItem>,
    auditResults: List<AuditResult>,
    onItemClick: (BacklogItem) -> Unit,
    onRunAuditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAuditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Button(
            onClick = {
                onRunAuditClick()
                showAuditDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Icon(Icons.Rounded.Assessment, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.run_architectural_audit))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            items(backlogItems, key = { it.id }) { item ->
                BacklogItemCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }

    if (showAuditDialog) {
        AlertDialog(
            onDismissRequest = { showAuditDialog = false },
            title = { Text(stringResource(R.string.audit_results_title)) },
            text = {
                LazyColumn {
                    items(auditResults) { result ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (result.isPassed) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = stringResource(R.string.audit_rule_format, result.ruleName),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = stringResource(R.string.audit_passed_format, result.isPassed))
                                Text(text = stringResource(R.string.audit_message_format, result.message))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAuditDialog = false }) {
                    Text(stringResource(R.string.btn_ok))
                }
            }
        )
    }
}

@Composable
fun BacklogItemCard(
    item: BacklogItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.Task,
                contentDescription = null,
                tint = if (item.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.priority_format, item.priority),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
