package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CropAssignment
import com.example.data.model.CropStatuses
import com.example.data.model.CropWithPlot
import com.example.data.model.Plot
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KrushiDatePickerField
import com.example.ui.components.KrushiTopBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.HarvestGold
import com.example.util.DateUtils
import com.example.viewmodel.FarmViewModel

@Composable
fun CropsScreen(
    viewModel: FarmViewModel,
    onOpenDrawer: () -> Unit
) {
    val cropsWithPlot by viewModel.cropsWithPlot.collectAsStateWithLifecycle()
    val plots by viewModel.activePlots.collectAsStateWithLifecycle()
    var selectedStatusFilter by remember { mutableStateOf<String?>("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var cropToEdit by remember { mutableStateOf<CropAssignment?>(null) }
    var cropToDelete by remember { mutableStateOf<CropWithPlot?>(null) }
    var cropForHarvest by remember { mutableStateOf<CropAssignment?>(null) }

    val filteredCrops = remember(cropsWithPlot, selectedStatusFilter) {
        if (selectedStatusFilter == "ALL" || selectedStatusFilter == null) {
            cropsWithPlot
        } else {
            cropsWithPlot.filter { it.crop.status == selectedStatusFilter }
        }
    }

    Scaffold(
        topBar = {
            KrushiTopBar(
                title = stringResource(R.string.crops_title),
                subtitle = stringResource(R.string.app_name_marathi),
                onNavigationClick = onOpenDrawer
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_crop_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.crop_add))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status Filters Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStatusFilter == "ALL",
                    onClick = { selectedStatusFilter = "ALL" },
                    label = { Text("All (${cropsWithPlot.size})") },
                    modifier = Modifier.testTag("filter_all_crops")
                )
                FilterChip(
                    selected = selectedStatusFilter == CropStatuses.ACTIVE,
                    onClick = { selectedStatusFilter = CropStatuses.ACTIVE },
                    label = { Text("Active") },
                    modifier = Modifier.testTag("filter_active_crops")
                )
                FilterChip(
                    selected = selectedStatusFilter == CropStatuses.PLANNED,
                    onClick = { selectedStatusFilter = CropStatuses.PLANNED },
                    label = { Text("Planned") },
                    modifier = Modifier.testTag("filter_planned_crops")
                )
                FilterChip(
                    selected = selectedStatusFilter == CropStatuses.HARVESTED,
                    onClick = { selectedStatusFilter = CropStatuses.HARVESTED },
                    label = { Text("Harvested") },
                    modifier = Modifier.testTag("filter_harvested_crops")
                )
            }

            if (filteredCrops.isEmpty()) {
                EmptyStateCard(
                    message = stringResource(R.string.crop_empty),
                    icon = Icons.Default.Grass,
                    actionLabel = stringResource(R.string.crop_add),
                    onActionClick = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCrops, key = { it.crop.id }) { cropWithPlot ->
                        CropCardItem(
                            cropWithPlot = cropWithPlot,
                            onEdit = { cropToEdit = cropWithPlot.crop },
                            onRecordHarvest = { cropForHarvest = cropWithPlot.crop },
                            onStatusChange = { newStatus ->
                                viewModel.updateCropStatus(cropWithPlot.crop.id, newStatus)
                            },
                            onDelete = { cropToDelete = cropWithPlot }
                        )
                    }
                }
            }
        }
    }

    if (cropToDelete != null) {
        val c = cropToDelete!!
        ConfirmActionDialog(
            title = "Delete Crop",
            message = "Are you sure you want to delete ${c.crop.cropName} (${c.plot?.name ?: "No Plot"})?",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteCrop(c.crop.id)
                cropToDelete = null
            },
            onDismiss = { cropToDelete = null }
        )
    }

    if (cropForHarvest != null) {
        YieldFormDialog(
            crops = cropsWithPlot,
            preselectedCropId = cropForHarvest!!.id,
            onDismiss = { cropForHarvest = null },
            onSave = { cropId, date, qty, unit, rate, revenue, notes ->
                viewModel.recordYield(
                    id = null,
                    cropAssignmentId = cropId,
                    date = date,
                    quantity = qty,
                    unit = unit,
                    ratePerUnit = rate,
                    totalRevenue = revenue,
                    notes = notes,
                    onSuccess = { cropForHarvest = null }
                )
            }
        )
    }

    if (showAddDialog || cropToEdit != null) {
        CropFormDialog(
            crop = cropToEdit,
            plots = plots,
            onDismiss = {
                showAddDialog = false
                cropToEdit = null
            },
            onSave = { id, plotId, cropName, variety, plantingDate, expectedHarvestDate, status, perennial ->
                viewModel.saveCrop(
                    id = id,
                    plotId = plotId,
                    cropName = cropName,
                    variety = variety,
                    plantingDate = plantingDate,
                    expectedHarvestDate = expectedHarvestDate,
                    status = status,
                    perennial = perennial,
                    onSuccess = {
                        showAddDialog = false
                        cropToEdit = null
                    }
                )
            }
        )
    }
}

@Composable
fun CropCardItem(
    cropWithPlot: CropWithPlot,
    onEdit: () -> Unit,
    onRecordHarvest: () -> Unit,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val crop = cropWithPlot.crop
    val daysLeft = DateUtils.daysFromToday(crop.expectedHarvestDate)
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("crop_card_${crop.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Grass,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = crop.cropName + if (crop.variety.isNotEmpty()) " (${crop.variety})" else "",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = crop.status)
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Record Harvest") },
                            onClick = {
                                menuExpanded = false
                                onRecordHarvest()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Crop") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        if (crop.status != CropStatuses.ACTIVE) {
                            DropdownMenuItem(
                                text = { Text("Set ACTIVE") },
                                onClick = {
                                    menuExpanded = false
                                    onStatusChange(CropStatuses.ACTIVE)
                                }
                            )
                        }
                        if (crop.status != CropStatuses.HARVESTED) {
                            DropdownMenuItem(
                                text = { Text("Set HARVESTED") },
                                onClick = {
                                    menuExpanded = false
                                    onStatusChange(CropStatuses.HARVESTED)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete Crop", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Landscape,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Plot: ${cropWithPlot.plot?.name ?: "Unknown"}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (crop.perennial) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "Perennial",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Planted: ${DateUtils.formatForDisplay(crop.plantingDate)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "Expected Harvest: ${DateUtils.formatForDisplay(crop.expectedHarvestDate)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                if (crop.status == CropStatuses.ACTIVE) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (daysLeft <= 7) HarvestGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = when {
                                daysLeft < 0 -> "Overdue"
                                daysLeft == 0L -> "Harvest Today"
                                else -> "${daysLeft}d left"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (daysLeft <= 7) HarvestGold else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropFormDialog(
    crop: CropAssignment?,
    plots: List<Plot>,
    preselectedPlotId: String? = null,
    onDismiss: () -> Unit,
    onSave: (id: String?, plotId: String, cropName: String, variety: String, plantingDate: String, expectedHarvestDate: String, status: String, perennial: Boolean) -> Unit
) {
    var plotId by remember { mutableStateOf(crop?.plotId ?: preselectedPlotId ?: plots.firstOrNull()?.id ?: "") }
    var cropName by remember { mutableStateOf(crop?.cropName ?: "") }
    var variety by remember { mutableStateOf(crop?.variety ?: "") }
    var plantingDate by remember { mutableStateOf(crop?.plantingDate ?: DateUtils.today()) }
    var expectedHarvestDate by remember {
        mutableStateOf(crop?.expectedHarvestDate ?: DateUtils.offsetDate(DateUtils.today(), 90))
    }
    var status by remember { mutableStateOf(crop?.status ?: CropStatuses.ACTIVE) }
    var perennial by remember { mutableStateOf(crop?.perennial ?: false) }

    var plotDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var cropNameError by remember { mutableStateOf(false) }
    var plotError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (crop == null) stringResource(R.string.crop_add) else stringResource(R.string.crop_edit),
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
                // Plot Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = plotDropdownExpanded,
                    onExpandedChange = { plotDropdownExpanded = !plotDropdownExpanded }
                ) {
                    val selectedPlot = plots.find { it.id == plotId }
                    OutlinedTextField(
                        value = selectedPlot?.name ?: "Select Farm Plot",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.crop_select_plot) + " *") },
                        isError = plotError,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = plotDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("crop_plot_selector")
                    )
                    ExposedDropdownMenu(
                        expanded = plotDropdownExpanded,
                        onDismissRequest = { plotDropdownExpanded = false }
                    ) {
                        plots.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.name} (${p.area} ${p.areaUnit})") },
                                onClick = {
                                    plotId = p.id
                                    plotError = false
                                    plotDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = cropName,
                    onValueChange = {
                        cropName = it
                        cropNameError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.crop_name) + " *") },
                    placeholder = { Text("e.g. Soybean, Cotton, Wheat, Sugarcane") },
                    isError = cropNameError,
                    supportingText = { if (cropNameError) Text(stringResource(R.string.err_required_field)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("crop_name_input")
                )

                OutlinedTextField(
                    value = variety,
                    onValueChange = { variety = it },
                    label = { Text(stringResource(R.string.crop_variety)) },
                    placeholder = { Text("e.g. JS-335, Desi, Hybrid 921") },
                    modifier = Modifier.fillMaxWidth()
                )

                KrushiDatePickerField(
                    label = stringResource(R.string.crop_planting_date),
                    selectedDateIso = plantingDate,
                    onDateSelected = { plantingDate = it }
                )

                KrushiDatePickerField(
                    label = stringResource(R.string.crop_expected_harvest),
                    selectedDateIso = expectedHarvestDate,
                    onDateSelected = { expectedHarvestDate = it }
                )

                // Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = statusDropdownExpanded,
                    onExpandedChange = { statusDropdownExpanded = !statusDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.crop_status)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        CropStatuses.all.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = {
                                    status = st
                                    statusDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Perennial Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { perennial = !perennial },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = perennial,
                        onCheckedChange = { perennial = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.crop_perennial),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validName = cropName.isNotBlank()
                    val validPlot = plotId.isNotBlank()

                    cropNameError = !validName
                    plotError = !validPlot

                    if (validName && validPlot) {
                        onSave(
                            crop?.id,
                            plotId,
                            cropName,
                            variety,
                            plantingDate,
                            expectedHarvestDate,
                            status,
                            perennial
                        )
                    }
                },
                modifier = Modifier.testTag("save_crop_button")
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
