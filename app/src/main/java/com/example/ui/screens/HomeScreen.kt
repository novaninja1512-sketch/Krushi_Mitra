package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ReceiptLong
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DailyTask
import com.example.data.model.TaskWithDetails
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KrushiTopBar
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.navigation.Screen
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.StatusPresentGreen
import com.example.util.DateUtils
import com.example.viewmodel.FarmViewModel

@Composable
fun HomeScreen(
    viewModel: FarmViewModel,
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val dashboard by viewModel.dashboardState.collectAsStateWithLifecycle()
    val todayDateFormatted = DateUtils.formatForDisplay(DateUtils.today())

    Scaffold(
        topBar = {
            KrushiTopBar(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.app_name_marathi) + " • " + stringResource(R.string.app_tagline),
                onNavigationClick = onOpenDrawer
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_screen_scroll"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.dashboard_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = todayDateFormatted,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // Quick Record Shortcuts
            item {
                Text(
                    text = stringResource(R.string.dashboard_quick_record),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = stringResource(R.string.nav_attendance),
                        icon = Icons.Default.AssignmentTurnedIn,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.Attendance.route) }
                    )
                    QuickActionCard(
                        title = stringResource(R.string.nav_tasks),
                        icon = Icons.Default.DateRange,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.Tasks.route) }
                    )
                    QuickActionCard(
                        title = stringResource(R.string.nav_expenses),
                        icon = Icons.Default.ReceiptLong,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.Expenses.route) }
                    )
                }
            }

            // Farm Summary Counters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = stringResource(R.string.dashboard_active_plots),
                        value = dashboard.activePlotsCount.toString(),
                        icon = Icons.Default.Landscape,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.Plots.route) }
                    )
                    StatCard(
                        title = stringResource(R.string.dashboard_active_crops),
                        value = dashboard.activeCrops.size.toString(),
                        icon = Icons.Default.Grass,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.Crops.route) }
                    )
                    StatCard(
                        title = stringResource(R.string.dashboard_workers_count),
                        value = dashboard.activeWorkersCount.toString(),
                        icon = Icons.Default.People,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.Workers.route) }
                    )
                }
            }

            // Section: Today's Tasks
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_today_tasks) + " (${dashboard.todayTasks.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = stringResource(R.string.action_view_all),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier
                            .clickable { onNavigate(Screen.Tasks.route) }
                            .padding(4.dp)
                    )
                }
            }

            if (dashboard.todayTasks.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = stringResource(R.string.dashboard_no_tasks_today),
                        icon = Icons.Default.DateRange,
                        actionLabel = stringResource(R.string.task_add),
                        onActionClick = { onNavigate(Screen.Tasks.route) }
                    )
                }
            } else {
                items(dashboard.todayTasks, key = { it.task.id }) { taskWithDetails ->
                    TodayTaskItem(
                        taskWithDetails = taskWithDetails,
                        onToggleComplete = { viewModel.toggleTaskCompletion(taskWithDetails.task) }
                    )
                }
            }

            // Section: Upcoming Harvests
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_upcoming_harvests),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = stringResource(R.string.action_view_all),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier
                            .clickable { onNavigate(Screen.Crops.route) }
                            .padding(4.dp)
                    )
                }
            }

            if (dashboard.upcomingHarvests.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_no_upcoming_harvests),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(dashboard.upcomingHarvests, key = { it.crop.id }) { cropWithPlot ->
                    val daysLeft = DateUtils.daysFromToday(cropWithPlot.crop.expectedHarvestDate)
                    HarvestCard(
                        cropWithPlot = cropWithPlot,
                        daysLeft = daysLeft,
                        onRecordYield = { onNavigate(Screen.Yield.route) }
                    )
                }
            }

            // Section: Recent Expenses
            if (dashboard.recentExpenses.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_recent_expenses),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = stringResource(R.string.action_view_all),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier
                                .clickable { onNavigate(Screen.Expenses.route) }
                                .padding(4.dp)
                        )
                    }
                }

                items(dashboard.recentExpenses, key = { it.id }) { exp ->
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
                                Text(
                                    text = exp.description,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${exp.category} • ${DateUtils.formatForDisplay(exp.date)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            Text(
                                text = DateUtils.formatCurrency(exp.amount),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // Bottom Spacing
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .clickable { onClick() }
            .testTag("quick_action_${title.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TodayTaskItem(
    taskWithDetails: TaskWithDetails,
    onToggleComplete: () -> Unit
) {
    val task = taskWithDetails.task
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_task_${task.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier.size(40.dp)
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
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(status = task.taskType)
                    if (taskWithDetails.plot != null) {
                        Text(
                            text = "• ${taskWithDetails.plot.name}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    if (taskWithDetails.workers.isNotEmpty()) {
                        Text(
                            text = "• ${taskWithDetails.workers.size} workers",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HarvestCard(
    cropWithPlot: com.example.data.model.CropWithPlot,
    daysLeft: Long,
    onRecordYield: () -> Unit
) {
    val crop = cropWithPlot.crop
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = crop.cropName + if (crop.variety.isNotEmpty()) " (${crop.variety})" else "",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Plot: ${cropWithPlot.plot?.name ?: "Unknown"} • Harvest: ${DateUtils.formatForDisplay(crop.expectedHarvestDate)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (daysLeft <= 7) HarvestGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = when {
                        daysLeft < 0 -> "Overdue"
                        daysLeft == 0L -> "Today"
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
