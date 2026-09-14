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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AttendanceStatuses
import com.example.data.model.Worker
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KrushiTopBar
import com.example.ui.components.StatCard
import com.example.ui.theme.StatusAbsentRed
import com.example.ui.theme.StatusHalfDayOrange
import com.example.ui.theme.StatusPresentGreen
import com.example.util.DateUtils
import com.example.viewmodel.FarmViewModel

@Composable
fun AttendanceScreen(
    viewModel: FarmViewModel,
    onNavigateToWorkers: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val selectedDate by viewModel.selectedAttendanceDate.collectAsStateWithLifecycle()
    val workers by viewModel.activeWorkers.collectAsStateWithLifecycle()
    val attendanceList by viewModel.attendanceForSelectedDate.collectAsStateWithLifecycle()

    val attendanceMap = remember(attendanceList) {
        attendanceList.associateBy({ it.workerId }, { it.status })
    }

    val presentCount = remember(attendanceList) {
        attendanceList.count { it.status == AttendanceStatuses.PRESENT }
    }
    val halfDayCount = remember(attendanceList) {
        attendanceList.count { it.status == AttendanceStatuses.HALF_DAY }
    }
    val absentCount = remember(attendanceList) {
        attendanceList.count { it.status == AttendanceStatuses.ABSENT }
    }

    Scaffold(
        topBar = {
            KrushiTopBar(
                title = stringResource(R.string.attendance_title),
                subtitle = stringResource(R.string.app_name_marathi),
                onNavigationClick = onOpenDrawer
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Date Picker Control Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.setAttendanceDate(DateUtils.offsetDate(selectedDate, -1))
                        },
                        modifier = Modifier.testTag("prev_date_button")
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = DateUtils.formatForDisplay(selectedDate),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (selectedDate == DateUtils.today()) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        } else {
                            Text(
                                text = "Tap to jump to Today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.clickable {
                                    viewModel.setAttendanceDate(DateUtils.today())
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.setAttendanceDate(DateUtils.offsetDate(selectedDate, 1))
                        },
                        modifier = Modifier.testTag("next_date_button")
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                    }
                }
            }

            // Summary row & Mark All Present button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusPresentGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "P: $presentCount",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = StatusPresentGreen
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusHalfDayOrange.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "HD: $halfDayCount",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = StatusHalfDayOrange
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusAbsentRed.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "A: $absentCount",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = StatusAbsentRed
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { viewModel.markAllPresent() },
                    modifier = Modifier.testTag("mark_all_present_button")
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.attendance_mark_all_present))
                }
            }

            if (workers.isEmpty()) {
                EmptyStateCard(
                    message = "No active workers found. Add workers first to track attendance.",
                    icon = Icons.Default.People,
                    actionLabel = stringResource(R.string.worker_add),
                    onActionClick = onNavigateToWorkers
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(workers, key = { it.id }) { worker ->
                        val currentStatus = attendanceMap[worker.id]
                        WorkerAttendanceRow(
                            worker = worker,
                            currentStatus = currentStatus,
                            onStatusSelected = { newStatus ->
                                viewModel.recordWorkerAttendance(worker.id, newStatus)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkerAttendanceRow(
    worker: Worker,
    currentStatus: String?,
    onStatusSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attendance_row_${worker.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = worker.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Wage: ${DateUtils.formatCurrency(worker.dailyWageRate)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                if (currentStatus != null) {
                    Text(
                        text = currentStatus,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = when (currentStatus) {
                                AttendanceStatuses.PRESENT -> StatusPresentGreen
                                AttendanceStatuses.HALF_DAY -> StatusHalfDayOrange
                                AttendanceStatuses.ABSENT -> StatusAbsentRed
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3 Segmented Toggle Options: Present, Half Day, Absent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttendanceOptionButton(
                    label = stringResource(R.string.attendance_status_present),
                    isSelected = currentStatus == AttendanceStatuses.PRESENT,
                    color = StatusPresentGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusSelected(AttendanceStatuses.PRESENT) }
                )
                AttendanceOptionButton(
                    label = stringResource(R.string.attendance_status_half_day),
                    isSelected = currentStatus == AttendanceStatuses.HALF_DAY,
                    color = StatusHalfDayOrange,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusSelected(AttendanceStatuses.HALF_DAY) }
                )
                AttendanceOptionButton(
                    label = stringResource(R.string.attendance_status_absent),
                    isSelected = currentStatus == AttendanceStatuses.ABSENT,
                    color = StatusAbsentRed,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusSelected(AttendanceStatuses.ABSENT) }
                )
            }
        }
    }
}

@Composable
private fun AttendanceOptionButton(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag("attendance_btn_${label.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color else color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) color else color.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else color
                )
            )
        }
    }
}
