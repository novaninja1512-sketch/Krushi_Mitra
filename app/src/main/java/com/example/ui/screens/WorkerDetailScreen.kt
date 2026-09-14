package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.TransactionTypes
import com.example.data.model.WorkerDetails
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.KrushiTopBar
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusAbsentRed
import com.example.ui.theme.StatusHalfDayOrange
import com.example.ui.theme.StatusPresentGreen
import com.example.util.DateUtils
import com.example.viewmodel.FarmViewModel

@Composable
fun WorkerDetailScreen(
    workerId: String,
    viewModel: FarmViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val workerDetails by viewModel.getWorkerDetailsFlow(workerId).collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val worker = workerDetails?.worker

    Scaffold(
        topBar = {
            KrushiTopBar(
                title = worker?.name ?: stringResource(R.string.worker_detail),
                subtitle = stringResource(R.string.worker_detail),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                actions = {
                    if (worker != null) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Worker")
                        }
                        IconButton(
                            onClick = {
                                viewModel.setWorkerArchived(worker.id, !worker.archived)
                            }
                        ) {
                            Icon(
                                Icons.Default.Archive,
                                contentDescription = if (worker.archived) "Restore" else "Archive"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (workerDetails == null || worker == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Worker not found")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onNavigateBack) { Text("Back") }
            }
        } else {
            val details = workerDetails!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("worker_detail_scroll"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Card
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        modifier = Modifier.size(52.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = worker.name.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = worker.name,
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Wage: ${DateUtils.formatCurrency(worker.dailyWageRate)} / day",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }

                                if (worker.archived) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            text = "Archived",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (worker.mobileNumber.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Phone: ${worker.mobileNumber}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    FilledTonalButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${worker.mobileNumber}")
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call")
                                    }
                                }
                            }

                            Text(
                                text = "Joined: ${DateUtils.formatForDisplay(worker.joiningDate)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            if (worker.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Notes: ${worker.notes}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                // Attendance Stats Section
                item {
                    Text(
                        text = stringResource(R.string.worker_attendance_history),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Present",
                            value = "${details.presentCount}d",
                            containerColor = StatusPresentGreen.copy(alpha = 0.15f),
                            contentColor = StatusPresentGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Half Day",
                            value = "${details.halfDayCount}d",
                            containerColor = StatusHalfDayOrange.copy(alpha = 0.15f),
                            contentColor = StatusHalfDayOrange,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Absent",
                            value = "${details.absentCount}d",
                            containerColor = StatusAbsentRed.copy(alpha = 0.15f),
                            contentColor = StatusAbsentRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Financial Summary Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.worker_payment_history),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Button(onClick = { showPaymentDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.payment_record))
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Total Advances",
                            value = DateUtils.formatCurrency(details.totalAdvance),
                            containerColor = StatusHalfDayOrange.copy(alpha = 0.12f),
                            contentColor = StatusHalfDayOrange,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Total Salary Paid",
                            value = DateUtils.formatCurrency(details.totalSalary),
                            containerColor = StatusPresentGreen.copy(alpha = 0.12f),
                            contentColor = StatusPresentGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Total Earned",
                            value = DateUtils.formatCurrency(details.totalEarnedPaise),
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        val balanceColor = if (details.remainingBalancePaise > 0L) StatusHalfDayOrange else StatusPresentGreen
                        StatCard(
                            title = if (details.remainingBalancePaise >= 0L) "Balance Due" else "Advance Excess",
                            value = DateUtils.formatCurrency(kotlin.math.abs(details.remainingBalancePaise)),
                            containerColor = balanceColor.copy(alpha = 0.15f),
                            contentColor = balanceColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Transactions List
                val transactions = details.recentTransactions
                if (transactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = "No payments recorded for this worker.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                } else {
                    items(transactions, key = { it.id }) { tr ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StatusBadge(status = tr.type)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = DateUtils.formatForDisplay(tr.date),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                    if (tr.notes.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = tr.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = DateUtils.formatCurrency(tr.amount),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (tr.type == TransactionTypes.ADVANCE) StatusHalfDayOrange else StatusPresentGreen
                                        )
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteTransaction(tr.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Section: Recent Tasks
                val tasks = details.recentTasks
                item {
                    Text(
                        text = "Assigned Tasks (${tasks.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (tasks.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = "No tasks assigned to this worker yet.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                } else {
                    items(tasks.take(6), key = { it.id }) { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (task.isCompleted) StatusPresentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.description,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "${task.taskType} • ${DateUtils.formatForDisplay(task.date)}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Delete Worker button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Delete Worker", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showEditDialog && worker != null) {
        WorkerFormDialog(
            worker = worker,
            onDismiss = { showEditDialog = false },
            onSave = { id, name, mobile, wage, joinDate, notes ->
                viewModel.saveWorker(
                    id = id,
                    name = name,
                    mobileNumber = mobile,
                    dailyWageRate = wage,
                    joiningDate = joinDate,
                    notes = notes,
                    onSuccess = { showEditDialog = false }
                )
            }
        )
    }

    if (showPaymentDialog && worker != null) {
        PaymentRecordDialog(
            preselectedWorkerId = worker.id,
            workers = listOf(worker),
            onDismiss = { showPaymentDialog = false },
            onSave = { workerId, type, amount, date, notes ->
                viewModel.recordTransaction(
                    workerId = workerId,
                    type = type,
                    amount = amount,
                    date = date,
                    notes = notes,
                    onSuccess = { showPaymentDialog = false }
                )
            }
        )
    }

    if (showDeleteConfirm && worker != null) {
        ConfirmActionDialog(
            title = "Delete Worker",
            message = "Are you sure you want to permanently delete '${worker.name}'? This will also remove attendance and payment records.",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteWorker(worker.id, onSuccess = onNavigateBack)
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
