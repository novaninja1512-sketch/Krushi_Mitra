package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Worker
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KrushiDatePickerField
import com.example.ui.components.KrushiTopBar
import com.example.util.DateUtils
import com.example.viewmodel.FarmViewModel

@Composable
fun WorkersScreen(
    viewModel: FarmViewModel,
    onNavigateToWorkerDetail: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val workers by viewModel.workers.collectAsStateWithLifecycle()
    val showArchived by viewModel.showArchivedWorkers.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var workerToEdit by remember { mutableStateOf<Worker?>(null) }

    Scaffold(
        topBar = {
            KrushiTopBar(
                title = stringResource(R.string.workers_title),
                subtitle = stringResource(R.string.app_name_marathi),
                onNavigationClick = onOpenDrawer
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_worker_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.worker_add))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${workers.size} " + if (showArchived) "Total Workers" else "Active Workers",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                FilterChip(
                    selected = showArchived,
                    onClick = { viewModel.toggleShowArchivedWorkers() },
                    label = { Text(stringResource(R.string.worker_show_archived)) },
                    modifier = Modifier.testTag("filter_archived_workers_chip")
                )
            }

            if (workers.isEmpty()) {
                EmptyStateCard(
                    message = stringResource(R.string.worker_empty),
                    icon = Icons.Default.People,
                    actionLabel = stringResource(R.string.worker_add),
                    onActionClick = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(workers, key = { it.id }) { worker ->
                        WorkerCardItem(
                            worker = worker,
                            onClick = { onNavigateToWorkerDetail(worker.id) },
                            onEdit = { workerToEdit = worker },
                            onArchiveToggle = { viewModel.setWorkerArchived(worker.id, !worker.archived) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog || workerToEdit != null) {
        WorkerFormDialog(
            worker = workerToEdit,
            onDismiss = {
                showAddDialog = false
                workerToEdit = null
            },
            onSave = { id, name, mobile, wage, joinDate, notes ->
                viewModel.saveWorker(
                    id = id,
                    name = name,
                    mobileNumber = mobile,
                    dailyWageRate = wage,
                    joiningDate = joinDate,
                    notes = notes,
                    onSuccess = {
                        showAddDialog = false
                        workerToEdit = null
                    }
                )
            }
        )
    }
}

@Composable
fun WorkerCardItem(
    worker: Worker,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onArchiveToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("worker_card_${worker.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (worker.archived)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = worker.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = worker.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (worker.archived) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "Archived",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Daily Wage: ${DateUtils.formatCurrency(worker.dailyWageRate)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                if (worker.mobileNumber.isNotEmpty()) {
                    Text(
                        text = "Phone: ${worker.mobileNumber}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onArchiveToggle, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Archive,
                        contentDescription = if (worker.archived) "Restore" else "Archive",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun WorkerFormDialog(
    worker: Worker?,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, mobile: String, wage: Double, joinDate: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(worker?.name ?: "") }
    var mobile by remember { mutableStateOf(worker?.mobileNumber ?: "") }
    var wageText by remember { mutableStateOf(worker?.dailyWageRate?.let { DateUtils.formatPaiseToRupeesString(it) } ?: "350") }
    var joiningDate by remember { mutableStateOf(worker?.joiningDate ?: DateUtils.today()) }
    var notes by remember { mutableStateOf(worker?.notes ?: "") }

    var nameError by remember { mutableStateOf(false) }
    var wageError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (worker == null) stringResource(R.string.worker_add) else stringResource(R.string.worker_edit),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.worker_name) + " *") },
                    isError = nameError,
                    supportingText = { if (nameError) Text(stringResource(R.string.err_required_field)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_name_input")
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text(stringResource(R.string.worker_mobile)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_mobile_input")
                )

                OutlinedTextField(
                    value = wageText,
                    onValueChange = {
                        wageText = it
                        val w = it.toDoubleOrNull()
                        wageError = w == null || w < 0.0
                    },
                    label = { Text(stringResource(R.string.worker_daily_wage) + " *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = wageError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_wage_input")
                )

                KrushiDatePickerField(
                    label = stringResource(R.string.worker_joining_date),
                    selectedDateIso = joiningDate,
                    onDateSelected = { joiningDate = it }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.worker_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validName = name.isNotBlank()
                    val wageVal = wageText.toDoubleOrNull()
                    val validWage = wageVal != null && wageVal >= 0.0

                    nameError = !validName
                    wageError = !validWage

                    if (validName && validWage) {
                        onSave(worker?.id, name, mobile, wageVal, joiningDate, notes)
                    }
                },
                modifier = Modifier.testTag("save_worker_button")
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
