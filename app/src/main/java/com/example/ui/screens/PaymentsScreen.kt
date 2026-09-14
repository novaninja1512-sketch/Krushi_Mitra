package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.TransactionTypes
import com.example.data.model.Worker
import com.example.data.model.WorkerTransaction
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KrushiDatePickerField
import com.example.ui.components.KrushiTopBar
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusHalfDayOrange
import com.example.ui.theme.StatusPresentGreen
import com.example.util.DateUtils
import com.example.viewmodel.FarmViewModel

@Composable
fun PaymentsScreen(
    viewModel: FarmViewModel,
    onOpenDrawer: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val workers by viewModel.activeWorkers.collectAsStateWithLifecycle()
    var selectedTypeFilter by remember { mutableStateOf<String?>("ALL") }
    var showRecordDialog by remember { mutableStateOf(false) }

    val workerMap = remember(workers) { workers.associateBy { it.id } }

    val filteredTransactions = remember(transactions, selectedTypeFilter) {
        if (selectedTypeFilter == "ALL" || selectedTypeFilter == null) {
            transactions
        } else {
            transactions.filter { it.type == selectedTypeFilter }
        }
    }

    val totalAdvances = remember(transactions) {
        transactions.filter { it.type == TransactionTypes.ADVANCE }.sumOf { it.amount }
    }
    val totalSalaries = remember(transactions) {
        transactions.filter { it.type == TransactionTypes.SALARY }.sumOf { it.amount }
    }

    Scaffold(
        topBar = {
            KrushiTopBar(
                title = stringResource(R.string.payments_title),
                subtitle = stringResource(R.string.app_name_marathi),
                onNavigationClick = onOpenDrawer
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showRecordDialog = true },
                modifier = Modifier.testTag("record_payment_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.payment_record))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Total Advances",
                    value = DateUtils.formatCurrency(totalAdvances),
                    containerColor = StatusHalfDayOrange.copy(alpha = 0.12f),
                    contentColor = StatusHalfDayOrange,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Total Salaries",
                    value = DateUtils.formatCurrency(totalSalaries),
                    containerColor = StatusPresentGreen.copy(alpha = 0.12f),
                    contentColor = StatusPresentGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTypeFilter == "ALL",
                    onClick = { selectedTypeFilter = "ALL" },
                    label = { Text("All (${transactions.size})") },
                    modifier = Modifier.testTag("filter_all_payments")
                )
                FilterChip(
                    selected = selectedTypeFilter == TransactionTypes.ADVANCE,
                    onClick = { selectedTypeFilter = TransactionTypes.ADVANCE },
                    label = { Text("Advances") },
                    modifier = Modifier.testTag("filter_advance_payments")
                )
                FilterChip(
                    selected = selectedTypeFilter == TransactionTypes.SALARY,
                    onClick = { selectedTypeFilter = TransactionTypes.SALARY },
                    label = { Text("Salaries") },
                    modifier = Modifier.testTag("filter_salary_payments")
                )
            }

            if (filteredTransactions.isEmpty()) {
                EmptyStateCard(
                    message = stringResource(R.string.payment_empty),
                    icon = Icons.Default.MonetizationOn,
                    actionLabel = stringResource(R.string.payment_record),
                    onActionClick = { showRecordDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tr ->
                        val workerName = workerMap[tr.workerId]?.name ?: "Worker"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_card_${tr.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = workerName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StatusBadge(status = tr.type)
                                        Text(
                                            text = DateUtils.formatForDisplay(tr.date),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                    if (tr.notes.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
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
                                            color = if (tr.type == TransactionTypes.ADVANCE)
                                                StatusHalfDayOrange
                                            else StatusPresentGreen
                                        )
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteTransaction(tr.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRecordDialog) {
        PaymentRecordDialog(
            preselectedWorkerId = null,
            workers = workers,
            onDismiss = { showRecordDialog = false },
            onSave = { workerId, type, amount, date, notes ->
                viewModel.recordTransaction(
                    workerId = workerId,
                    type = type,
                    amount = amount,
                    date = date,
                    notes = notes,
                    onSuccess = { showRecordDialog = false }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentRecordDialog(
    preselectedWorkerId: String?,
    workers: List<Worker>,
    onDismiss: () -> Unit,
    onSave: (workerId: String, type: String, amount: Double, date: String, notes: String) -> Unit
) {
    var workerId by remember { mutableStateOf(preselectedWorkerId ?: workers.firstOrNull()?.id ?: "") }
    var type by remember { mutableStateOf(TransactionTypes.ADVANCE) }
    var amountText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(DateUtils.today()) }
    var notes by remember { mutableStateOf("") }

    var workerDropdownExpanded by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var workerError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.payment_record), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Worker selection (if not fixed)
                if (preselectedWorkerId == null) {
                    ExposedDropdownMenuBox(
                        expanded = workerDropdownExpanded,
                        onExpandedChange = { workerDropdownExpanded = !workerDropdownExpanded }
                    ) {
                        val selectedWorker = workers.find { it.id == workerId }
                        OutlinedTextField(
                            value = selectedWorker?.name ?: "Select Worker",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.payment_select_worker) + " *") },
                            isError = workerError,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workerDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("payment_worker_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = workerDropdownExpanded,
                            onDismissRequest = { workerDropdownExpanded = false }
                        ) {
                            workers.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w.name) },
                                    onClick = {
                                        workerId = w.id
                                        workerError = false
                                        workerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Payment Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.payment_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        TransactionTypes.all.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    type = t
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        val a = it.toDoubleOrNull()
                        amountError = a == null || a <= 0.0
                    },
                    label = { Text(stringResource(R.string.payment_amount) + " (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = { if (amountError) Text(stringResource(R.string.err_required_field)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )

                KrushiDatePickerField(
                    label = stringResource(R.string.payment_date),
                    selectedDateIso = date,
                    onDateSelected = { date = it }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.payment_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validWorker = workerId.isNotBlank()
                    val amountVal = amountText.toDoubleOrNull()
                    val validAmount = amountVal != null && amountVal > 0.0

                    workerError = !validWorker
                    amountError = !validAmount

                    if (validWorker && validAmount) {
                        onSave(workerId, type, amountVal, date, notes)
                    }
                },
                modifier = Modifier.testTag("save_payment_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
