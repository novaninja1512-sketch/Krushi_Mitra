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
import androidx.compose.material.icons.filled.Agriculture
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
import com.example.data.model.CropWithPlot
import com.example.data.model.YieldUnits
import com.example.data.model.YieldWithCrop
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KrushiDatePickerField
import com.example.ui.components.KrushiTopBar
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.StatusPresentGreen
import com.example.util.DateUtils
import com.example.viewmodel.FarmViewModel

@Composable
fun YieldScreen(
    viewModel: FarmViewModel,
    onOpenDrawer: () -> Unit
) {
    val yieldsWithCrop by viewModel.yieldsWithCrop.collectAsStateWithLifecycle()
    val activeCrops by viewModel.activeCrops.collectAsStateWithLifecycle()
    val allCrops by viewModel.cropsWithPlot.collectAsStateWithLifecycle()
    var showRecordDialog by remember { mutableStateOf(false) }

    val totalRevenue = remember(yieldsWithCrop) {
        yieldsWithCrop.sumOf { it.yieldRecord.totalRevenue }
    }

    Scaffold(
        topBar = {
            KrushiTopBar(
                title = stringResource(R.string.yield_title),
                subtitle = stringResource(R.string.app_name_marathi),
                onNavigationClick = onOpenDrawer
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showRecordDialog = true },
                modifier = Modifier.testTag("add_yield_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.yield_record))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Revenue Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Harvest Revenue",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Text(
                            text = DateUtils.formatCurrency(totalRevenue),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Text(
                            text = "${yieldsWithCrop.size} Harvest Records",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Agriculture,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            if (yieldsWithCrop.isEmpty()) {
                EmptyStateCard(
                    message = stringResource(R.string.yield_empty),
                    icon = Icons.Default.Agriculture,
                    actionLabel = stringResource(R.string.yield_record),
                    onActionClick = { showRecordDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(yieldsWithCrop, key = { it.yieldRecord.id }) { item ->
                        val yr = item.yieldRecord
                        val crop = item.cropWithPlot?.crop
                        val plot = item.cropWithPlot?.plot

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("yield_card_${yr.id}"),
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
                                        text = crop?.cropName ?: "Harvest",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Quantity: ${DateUtils.formatNumber(yr.quantity)} ${yr.unit}" +
                                                if (yr.ratePerUnit > 0.0) " @ ${DateUtils.formatCurrency(yr.ratePerUnit)}/${yr.unit}" else "",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "${DateUtils.formatForDisplay(yr.date)}" +
                                                if (plot != null) " • Plot: ${plot.name}" else "",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    if (yr.notes.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = yr.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (yr.totalRevenue > 0.0) {
                                        Text(
                                            text = DateUtils.formatCurrency(yr.totalRevenue),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = StatusPresentGreen
                                            )
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteYield(yr.id) },
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
        val selectableCrops = if (activeCrops.isNotEmpty()) activeCrops else allCrops
        YieldFormDialog(
            crops = selectableCrops,
            onDismiss = { showRecordDialog = false },
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
                    onSuccess = { showRecordDialog = false }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YieldFormDialog(
    crops: List<CropWithPlot>,
    onDismiss: () -> Unit,
    onSave: (cropId: String, date: String, qty: Double, unit: String, rate: Double, revenue: Double, notes: String) -> Unit
) {
    var selectedCropId by remember { mutableStateOf(crops.firstOrNull()?.crop?.id ?: "") }
    var date by remember { mutableStateOf(DateUtils.today()) }
    var quantityText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf<String>(YieldUnits.QUINTAL) }
    var rateText by remember { mutableStateOf("") }
    var totalRevenueText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var cropDropdownExpanded by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }
    var cropError by remember { mutableStateOf(false) }
    var qtyError by remember { mutableStateOf(false) }

    // Auto calculate revenue when qty or rate changes
    fun recalculateRevenue(qStr: String, rStr: String) {
        val q = qStr.toDoubleOrNull() ?: 0.0
        val r = rStr.toDoubleOrNull() ?: 0.0
        if (q > 0.0 && r > 0.0) {
            totalRevenueText = DateUtils.formatNumber(q * r)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.yield_record), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Crop Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = cropDropdownExpanded,
                    onExpandedChange = { cropDropdownExpanded = !cropDropdownExpanded }
                ) {
                    val currentCrop = crops.find { it.crop.id == selectedCropId }
                    val labelText = if (currentCrop != null) {
                        "${currentCrop.crop.cropName} (${currentCrop.plot?.name ?: "Plot"})"
                    } else "Select Crop"

                    OutlinedTextField(
                        value = labelText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Crop *") },
                        isError = cropError,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cropDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("yield_crop_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = cropDropdownExpanded,
                        onDismissRequest = { cropDropdownExpanded = false }
                    ) {
                        crops.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.crop.cropName} - ${c.plot?.name ?: "No Plot"}") },
                                onClick = {
                                    selectedCropId = c.crop.id
                                    cropError = false
                                    cropDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = {
                            quantityText = it
                            val q = it.toDoubleOrNull()
                            qtyError = q == null || q <= 0.0
                            recalculateRevenue(it, rateText)
                        },
                        label = { Text(stringResource(R.string.yield_quantity) + " *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = qtyError,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("yield_quantity_input")
                    )

                    ExposedDropdownMenuBox(
                        expanded = unitDropdownExpanded,
                        onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.yield_unit)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = unitDropdownExpanded,
                            onDismissRequest = { unitDropdownExpanded = false }
                        ) {
                            YieldUnits.all.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        unitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = rateText,
                    onValueChange = {
                        rateText = it
                        recalculateRevenue(quantityText, it)
                    },
                    label = { Text("Rate per $unit (₹, Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalRevenueText,
                    onValueChange = { totalRevenueText = it },
                    label = { Text(stringResource(R.string.yield_total_revenue) + " (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                KrushiDatePickerField(
                    label = stringResource(R.string.yield_date),
                    selectedDateIso = date,
                    onDateSelected = { date = it }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.yield_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validCrop = selectedCropId.isNotBlank()
                    val qtyVal = quantityText.toDoubleOrNull()
                    val validQty = qtyVal != null && qtyVal > 0.0

                    cropError = !validCrop
                    qtyError = !validQty

                    if (validCrop && validQty) {
                        val rate = rateText.toDoubleOrNull() ?: 0.0
                        val rev = totalRevenueText.toDoubleOrNull() ?: (qtyVal * rate)
                        onSave(selectedCropId, date, qtyVal, unit, rate, rev, notes)
                    }
                },
                modifier = Modifier.testTag("save_yield_button")
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
