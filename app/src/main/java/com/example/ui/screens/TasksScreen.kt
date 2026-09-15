package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DailyTask
import com.example.data.model.Plot
import com.example.data.model.TaskTypes
import com.example.data.model.TaskWithDetails
import com.example.data.model.Worker
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KrushiDatePickerField
import com.example.ui.components.KrushiTopBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusPresentGreen
import com.example.util.DateUtils
import com.example.viewmodel.FarmViewModel

@Composable
fun TasksScreen(
    viewModel: FarmViewModel,
    onOpenDrawer: () -> Unit
) {
    val tasksWithDetails by viewModel.tasksWithDetails.collectAsStateWithLifecycle()
    val plots by viewModel.activePlots.collectAsStateWithLifecycle()
    val workers by viewModel.activeWorkers.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<TaskWithDetails?>(null) }

    val today = DateUtils.today()
    val filteredTasks = remember(tasksWithDetails, selectedFilter) {
        when (selectedFilter) {
            "TODAY" -> tasksWithDetails.filter { it.task.date == today }
            "PENDING" -> tasksWithDetails.filter { !it.task.isCompleted }
            "COMPLETED" -> tasksWithDetails.filter { it.task.isCompleted }
            else -> tasksWithDetails
        }
    }

    Scaffold(
        topBar = {
            KrushiTopBar(
                title = stringResource(R.string.tasks_title),
                subtitle = stringResource(R.string.app_name_marathi),
                onNavigationClick = onOpenDrawer
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.task_add))
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All (${tasksWithDetails.size})") },
                    modifier = Modifier.testTag("filter_all_tasks")
                )
                FilterChip(
                    selected = selectedFilter == "TODAY",
                    onClick = { selectedFilter = "TODAY" },
                    label = { Text("Today") },
                    modifier = Modifier.testTag("filter_today_tasks")
                )
                FilterChip(
                    selected = selectedFilter == "PENDING",
                    onClick = { selectedFilter = "PENDING" },
                    label = { Text("Pending") },
                    modifier = Modifier.testTag("filter_pending_tasks")
                )
                FilterChip(
                    selected = selectedFilter == "COMPLETED",
                    onClick = { selectedFilter = "COMPLETED" },
                    label = { Text("Done") },
                    modifier = Modifier.testTag("filter_done_tasks")
                )
            }

            if (filteredTasks.isEmpty()) {
                EmptyStateCard(
                    message = stringResource(R.string.task_empty),
                    icon = Icons.Default.DateRange,
                    actionLabel = stringResource(R.string.task_add),
                    onActionClick = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTasks, key = { it.task.id }) { taskWithDetails ->
                        TaskCardItem(
                            taskWithDetails = taskWithDetails,
                            onToggleComplete = { viewModel.toggleTaskCompletion(taskWithDetails.task) },
                            onDelete = { taskToDelete = taskWithDetails }
                        )
                    }
                }
            }
        }
    }

    if (taskToDelete != null) {
        val task = taskToDelete!!.task
        ConfirmActionDialog(
            title = "Delete Task",
            message = "Are you sure you want to delete this task: \"${task.description}\"?",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteTask(task.id)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }

    if (showAddDialog) {
        TaskFormDialog(
            plots = plots,
            workers = workers,
            onDismiss = { showAddDialog = false },
            onSave = { date, plotId, type, desc, duration, workerIds, notes ->
                viewModel.saveTask(
                    id = null,
                    date = date,
                    plotId = plotId,
                    taskType = type,
                    description = desc,
                    durationHours = duration,
                    workerIds = workerIds,
                    notes = notes,
                    onSuccess = { showAddDialog = false }
                )
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCardItem(
    taskWithDetails: TaskWithDetails,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val task = taskWithDetails.task
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Toggle Complete",
                        tint = if (task.isCompleted) StatusPresentGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusBadge(status = task.taskType)
                        Text(
                            text = "• ${DateUtils.formatForDisplay(task.date)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        if (task.durationHours > 0.0) {
                            Text(
                                text = "• ${task.durationHours} hrs",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                }
            }

            if (taskWithDetails.plot != null || taskWithDetails.workers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (taskWithDetails.plot != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Plot: ${taskWithDetails.plot.name}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    taskWithDetails.workers.forEach { worker ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = worker.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskFormDialog(
    plots: List<Plot>,
    workers: List<Worker>,
    preselectedPlotId: String? = null,
    onDismiss: () -> Unit,
    onSave: (date: String, plotId: String?, type: String, desc: String, duration: Double, workerIds: List<String>, notes: String) -> Unit
) {
    var date by remember { mutableStateOf(DateUtils.today()) }
    var taskType by remember { mutableStateOf<String>(TaskTypes.WEEDING) }
    var description by remember { mutableStateOf("") }
    var selectedPlotId by remember { mutableStateOf<String?>(preselectedPlotId ?: plots.firstOrNull()?.id) }
    var durationText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val selectedWorkerIds = remember { mutableStateListOf<String>() }

    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var plotDropdownExpanded by remember { mutableStateOf(false) }
    var descError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.task_add), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Task Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = taskType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.task_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        TaskTypes.all.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    taskType = t
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.task_description) + " *") },
                    placeholder = { Text("e.g. Apply weedicide in soybean plot") },
                    isError = descError,
                    supportingText = { if (descError) Text(stringResource(R.string.err_required_field)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_desc_input")
                )

                KrushiDatePickerField(
                    label = stringResource(R.string.task_date),
                    selectedDateIso = date,
                    onDateSelected = { date = it }
                )

                // Plot Selector
                ExposedDropdownMenuBox(
                    expanded = plotDropdownExpanded,
                    onExpandedChange = { plotDropdownExpanded = !plotDropdownExpanded }
                ) {
                    val currentPlot = plots.find { it.id == selectedPlotId }
                    OutlinedTextField(
                        value = currentPlot?.name ?: "General (No specific plot)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Plot (Optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = plotDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = plotDropdownExpanded,
                        onDismissRequest = { plotDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("General (No specific plot)") },
                            onClick = {
                                selectedPlotId = null
                                plotDropdownExpanded = false
                            }
                        )
                        plots.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    selectedPlotId = p.id
                                    plotDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text(stringResource(R.string.task_duration_hours)) },
                    placeholder = { Text("e.g. 4") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Multi-Worker Assignment
                if (workers.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Assign Workers (${DateUtils.formatWorkerCount(selectedWorkerIds.size)}):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            TextButton(
                                onClick = {
                                    selectedWorkerIds.clear()
                                    selectedWorkerIds.addAll(workers.map { it.id })
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("All", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(
                                onClick = { selectedWorkerIds.clear() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        workers.forEach { w ->
                            val isChecked = selectedWorkerIds.contains(w.id)
                            FilterChip(
                                selected = isChecked,
                                onClick = {
                                    if (isChecked) selectedWorkerIds.remove(w.id)
                                    else selectedWorkerIds.add(w.id)
                                },
                                label = { Text(w.name, style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = if (isChecked) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.task_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validDesc = description.isNotBlank()
                    descError = !validDesc

                    if (validDesc) {
                        val duration = durationText.toDoubleOrNull() ?: 0.0
                        onSave(
                            date,
                            selectedPlotId,
                            taskType,
                            description,
                            duration,
                            selectedWorkerIds.toList(),
                            notes
                        )
                    }
                },
                modifier = Modifier.testTag("save_task_button")
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
