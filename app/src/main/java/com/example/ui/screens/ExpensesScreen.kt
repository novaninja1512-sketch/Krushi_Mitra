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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
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
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategories
import com.example.data.model.Plot
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KrushiDatePickerField
import com.example.ui.components.KrushiTopBar
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.util.DateUtils
import com.example.viewmodel.FarmViewModel

@Composable
fun ExpensesScreen(
    viewModel: FarmViewModel,
    onOpenDrawer: () -> Unit
) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val plots by viewModel.activePlots.collectAsStateWithLifecycle()
    var selectedCategoryFilter by remember { mutableStateOf<String?>("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    val plotMap = remember(plots) { plots.associateBy { it.id } }

    val filteredExpenses = remember(expenses, selectedCategoryFilter) {
        if (selectedCategoryFilter == "ALL" || selectedCategoryFilter == null) {
            expenses
        } else {
            expenses.filter { it.category == selectedCategoryFilter }
        }
    }

    val totalExpense = remember(filteredExpenses) {
        filteredExpenses.sumOf { it.amount }
    }

    Scaffold(
        topBar = {
            KrushiTopBar(
                title = stringResource(R.string.expenses_title),
                subtitle = stringResource(R.string.app_name_marathi),
                onNavigationClick = onOpenDrawer
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.expense_add))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Total Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
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
                            text = "Total Expenses",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                        Text(
                            text = DateUtils.formatCurrency(totalExpense),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == "ALL",
                        onClick = { selectedCategoryFilter = "ALL" },
                        label = { Text("All (${expenses.size})") },
                        modifier = Modifier.testTag("filter_all_expenses")
                    )
                }
                items(ExpenseCategories.all) { category ->
                    FilterChip(
                        selected = selectedCategoryFilter == category,
                        onClick = { selectedCategoryFilter = category },
                        label = { Text(category) },
                        modifier = Modifier.testTag("filter_expense_${category.lowercase()}")
                    )
                }
            }

            if (filteredExpenses.isEmpty()) {
                EmptyStateCard(
                    message = stringResource(R.string.expense_empty),
                    icon = Icons.Default.ReceiptLong,
                    actionLabel = stringResource(R.string.expense_add),
                    onActionClick = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredExpenses, key = { it.id }) { exp ->
                        val plot = exp.plotId?.let { plotMap[it] }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_card_${exp.id}"),
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
                                        text = exp.description,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        StatusBadge(status = exp.category)
                                        Text(
                                            text = "• " + DateUtils.formatForDisplay(exp.date),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        if (plot != null) {
                                            Text(
                                                text = "• ${plot.name}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = DateUtils.formatCurrency(exp.amount),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteExpense(exp.id) },
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

    if (showAddDialog) {
        ExpenseFormDialog(
            plots = plots,
            onDismiss = { showAddDialog = false },
            onSave = { category, amount, date, desc, plotId ->
                viewModel.recordExpense(
                    category = category,
                    amount = amount,
                    date = date,
                    description = desc,
                    plotId = plotId,
                    onSuccess = { showAddDialog = false }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormDialog(
    plots: List<Plot>,
    onDismiss: () -> Unit,
    onSave: (category: String, amount: Double, date: String, desc: String, plotId: String?) -> Unit
) {
    var category by remember { mutableStateOf(ExpenseCategories.DIESEL) }
    var amountText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(DateUtils.today()) }
    var description by remember { mutableStateOf("") }
    var selectedPlotId by remember { mutableStateOf<String?>(null) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var plotDropdownExpanded by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var descError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.expense_add), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.expense_category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        ExpenseCategories.all.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryDropdownExpanded = false
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
                    label = { Text(stringResource(R.string.expense_amount) + " (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = { if (amountError) Text(stringResource(R.string.err_required_field)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.expense_description) + " *") },
                    placeholder = { Text("e.g. 20L Tractor Diesel, 2 bags DAP") },
                    isError = descError,
                    supportingText = { if (descError) Text(stringResource(R.string.err_required_field)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_desc_input")
                )

                KrushiDatePickerField(
                    label = stringResource(R.string.expense_date),
                    selectedDateIso = date,
                    onDateSelected = { date = it }
                )

                // Optional Plot Selector
                ExposedDropdownMenuBox(
                    expanded = plotDropdownExpanded,
                    onExpandedChange = { plotDropdownExpanded = !plotDropdownExpanded }
                ) {
                    val currentPlot = plots.find { it.id == selectedPlotId }
                    OutlinedTextField(
                        value = currentPlot?.name ?: "None (General Farm)",
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
                            text = { Text("None (General Farm)") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountText.toDoubleOrNull()
                    val validAmount = amountVal != null && amountVal > 0.0
                    val validDesc = description.isNotBlank()

                    amountError = !validAmount
                    descError = !validDesc

                    if (validAmount && validDesc) {
                        onSave(category, amountVal, date, description, selectedPlotId)
                    }
                },
                modifier = Modifier.testTag("save_expense_button")
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
