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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AreaUnits
import com.example.data.model.Plot
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KrushiTopBar
import com.example.ui.navigation.Screen
import com.example.util.DateUtils
import com.example.viewmodel.FarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlotsScreen(
    viewModel: FarmViewModel,
    onNavigateToPlotDetail: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val plots by viewModel.plots.collectAsStateWithLifecycle()
    val showArchived by viewModel.showArchivedPlots.collectAsStateWithLifecycle()
    var showAddPlotDialog by remember { mutableStateOf(false) }
    var plotToEdit by remember { mutableStateOf<Plot?>(null) }

    Scaffold(
        topBar = {
            KrushiTopBar(
                title = stringResource(R.string.plots_title),
                subtitle = stringResource(R.string.app_name_marathi),
                onNavigationClick = onOpenDrawer
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPlotDialog = true },
                modifier = Modifier.testTag("add_plot_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.plot_add))
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
                    text = "${plots.size} " + if (showArchived) "Total Plots" else "Active Plots",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                FilterChip(
                    selected = showArchived,
                    onClick = { viewModel.toggleShowArchivedPlots() },
                    label = { Text(stringResource(R.string.plot_show_archived)) },
                    modifier = Modifier.testTag("filter_archived_plots_chip")
                )
            }

            if (plots.isEmpty()) {
                EmptyStateCard(
                    message = stringResource(R.string.plot_empty),
                    icon = Icons.Default.Landscape,
                    actionLabel = stringResource(R.string.plot_add),
                    onActionClick = { showAddPlotDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(plots, key = { it.id }) { plot ->
                        PlotItemCard(
                            plot = plot,
                            onClick = { onNavigateToPlotDetail(plot.id) },
                            onEdit = { plotToEdit = plot },
                            onArchiveToggle = { viewModel.setPlotArchived(plot.id, !plot.archived) }
                        )
                    }
                }
            }
        }
    }

    if (showAddPlotDialog || plotToEdit != null) {
        PlotFormDialog(
            plot = plotToEdit,
            onDismiss = {
                showAddPlotDialog = false
                plotToEdit = null
            },
            onSave = { id, name, area, unit, soil, irrigation, notes ->
                viewModel.savePlot(
                    id = id,
                    name = name,
                    area = area,
                    areaUnit = unit,
                    soilType = soil,
                    irrigationType = irrigation,
                    notes = notes,
                    onSuccess = {
                        showAddPlotDialog = false
                        plotToEdit = null
                    }
                )
            }
        )
    }
}

@Composable
fun PlotItemCard(
    plot: Plot,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onArchiveToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("plot_card_${plot.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (plot.archived)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Landscape,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = plot.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (plot.archived) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = stringResource(R.string.plot_archived_tag),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Area: ${DateUtils.formatNumber(plot.area)} ${plot.areaUnit}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (plot.soilType.isNotEmpty()) {
                    Text(
                        text = "Soil: ${plot.soilType}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            if (plot.irrigationType.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = plot.irrigationType,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Plot", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onArchiveToggle, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Archive,
                        contentDescription = if (plot.archived) "Restore" else "Archive",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlotFormDialog(
    plot: Plot?,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, area: Double, unit: String, soil: String, irrigation: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(plot?.name ?: "") }
    var areaText by remember { mutableStateOf(plot?.area?.let { DateUtils.formatNumber(it) } ?: "") }
    var areaUnit by remember { mutableStateOf(plot?.areaUnit ?: AreaUnits.ACRE) }
    var soilType by remember { mutableStateOf(plot?.soilType ?: "") }
    var irrigationType by remember { mutableStateOf(plot?.irrigationType ?: "") }
    var notes by remember { mutableStateOf(plot?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var areaError by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (plot == null) stringResource(R.string.plot_add) else stringResource(R.string.plot_edit),
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
                    label = { Text(stringResource(R.string.plot_name) + " *") },
                    isError = nameError,
                    supportingText = { if (nameError) Text(stringResource(R.string.err_required_field)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plot_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = areaText,
                        onValueChange = {
                            areaText = it
                            val d = it.toDoubleOrNull()
                            areaError = d == null || d <= 0.0
                        },
                        label = { Text(stringResource(R.string.plot_area) + " *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = areaError,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("plot_area_input")
                    )

                    ExposedDropdownMenuBox(
                        expanded = unitDropdownExpanded,
                        onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = areaUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.plot_area_unit)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = unitDropdownExpanded,
                            onDismissRequest = { unitDropdownExpanded = false }
                        ) {
                            AreaUnits.all.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit) },
                                    onClick = {
                                        areaUnit = unit
                                        unitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = soilType,
                    onValueChange = { soilType = it },
                    label = { Text(stringResource(R.string.plot_soil_type)) },
                    placeholder = { Text("e.g. Black Cotton, Red, Loam") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = irrigationType,
                    onValueChange = { irrigationType = it },
                    label = { Text(stringResource(R.string.plot_irrigation_type)) },
                    placeholder = { Text("e.g. Drip, Well, Canal, Rainfed") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.plot_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validName = name.isNotBlank()
                    val areaVal = areaText.toDoubleOrNull()
                    val validArea = areaVal != null && areaVal > 0.0

                    nameError = !validName
                    areaError = !validArea

                    if (validName && validArea) {
                        onSave(plot?.id, name, areaVal, areaUnit, soilType, irrigationType, notes)
                    }
                },
                modifier = Modifier.testTag("save_plot_button")
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
