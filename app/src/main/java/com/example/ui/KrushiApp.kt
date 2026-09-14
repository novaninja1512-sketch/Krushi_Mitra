package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.ui.navigation.Screen
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.CropsScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PaymentsScreen
import com.example.ui.screens.PlotDetailScreen
import com.example.ui.screens.PlotsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.WorkerDetailScreen
import com.example.ui.screens.WorkersScreen
import com.example.ui.screens.YieldScreen
import com.example.viewmodel.FarmViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class DrawerMenuItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun KrushiApp(viewModel: FarmViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen to user feedback messages from ViewModel
    LaunchedEffect(viewModel) {
        viewModel.userMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerItems = listOf(
        DrawerMenuItem(Screen.Home.route, "Home Dashboard", "मुख्य पृष्ठ", Icons.Default.Home),
        DrawerMenuItem(Screen.Plots.route, "Farm Plots", "शेत तुकडे", Icons.Default.Landscape),
        DrawerMenuItem(Screen.Crops.route, "Crops Management", "पीक व्यवस्थापन", Icons.Default.Grass),
        DrawerMenuItem(Screen.Workers.route, "Worker Management", "मजूर व्यवस्थापन", Icons.Default.People),
        DrawerMenuItem(Screen.Attendance.route, "Daily Attendance", "दैनंदिन हजेरी", Icons.Default.AssignmentTurnedIn),
        DrawerMenuItem(Screen.Payments.route, "Worker Payments", "मजुरी व उचल", Icons.Default.MonetizationOn),
        DrawerMenuItem(Screen.Expenses.route, "Farm Expenses", "शेती खर्च", Icons.Default.ReceiptLong),
        DrawerMenuItem(Screen.Tasks.route, "Daily Tasks", "रोजची कामे", Icons.Default.DateRange),
        DrawerMenuItem(Screen.Yield.route, "Harvest & Yield", "उत्पादन व विक्री", Icons.Default.Agriculture),
        DrawerMenuItem(Screen.CloudSync.route, "Cloud & Account", "क्लाउड आणि खाते", Icons.Default.CloudSync)
    )

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(310.dp)
                    .testTag("app_navigation_drawer")
            ) {
                // Header in Drawer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.app_name_marathi),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.app_tagline),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }

                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Items list in Drawer
                drawerItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = {
                            Column {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        },
                        selected = isSelected,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            navigateTo(item.route)
                        },
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag("drawer_item_${item.route}")
                    )
                }
            }
        }
    ) {
        val showBottomBar = currentRoute in listOf(
            Screen.Home.route,
            Screen.Attendance.route,
            Screen.Tasks.route,
            Screen.Expenses.route
        )

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(modifier = Modifier.testTag("app_bottom_bar")) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text("Home") },
                            selected = currentRoute == Screen.Home.route,
                            onClick = { navigateTo(Screen.Home.route) },
                            modifier = Modifier.testTag("bottom_nav_home")
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null) },
                            label = { Text("Attendance") },
                            selected = currentRoute == Screen.Attendance.route,
                            onClick = { navigateTo(Screen.Attendance.route) },
                            modifier = Modifier.testTag("bottom_nav_attendance")
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            label = { Text("Tasks") },
                            selected = currentRoute == Screen.Tasks.route,
                            onClick = { navigateTo(Screen.Tasks.route) },
                            modifier = Modifier.testTag("bottom_nav_tasks")
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                            label = { Text("Expenses") },
                            selected = currentRoute == Screen.Expenses.route,
                            onClick = { navigateTo(Screen.Expenses.route) },
                            modifier = Modifier.testTag("bottom_nav_expenses")
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
                            label = { Text("All") },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.open() }
                            },
                            modifier = Modifier.testTag("bottom_nav_more")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigate = { route -> navigateTo(route) },
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                    }

                    composable(Screen.Plots.route) {
                        PlotsScreen(
                            viewModel = viewModel,
                            onNavigateToPlotDetail = { plotId ->
                                navController.navigate(Screen.PlotDetail.createRoute(plotId))
                            },
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                    }

                    composable(
                        route = Screen.PlotDetail.route,
                        arguments = listOf(navArgument("plotId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val plotId = backStackEntry.arguments?.getString("plotId").orEmpty()
                        PlotDetailScreen(
                            plotId = plotId,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToCrops = { navigateTo(Screen.Crops.route) }
                        )
                    }

                    composable(Screen.Crops.route) {
                        CropsScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                    }

                    composable(Screen.Workers.route) {
                        WorkersScreen(
                            viewModel = viewModel,
                            onNavigateToWorkerDetail = { workerId ->
                                navController.navigate(Screen.WorkerDetail.createRoute(workerId))
                            },
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                    }

                    composable(
                        route = Screen.WorkerDetail.route,
                        arguments = listOf(navArgument("workerId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val workerId = backStackEntry.arguments?.getString("workerId").orEmpty()
                        WorkerDetailScreen(
                            workerId = workerId,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Attendance.route) {
                        AttendanceScreen(
                            viewModel = viewModel,
                            onNavigateToWorkers = { navigateTo(Screen.Workers.route) },
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                    }

                    composable(Screen.Payments.route) {
                        PaymentsScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                    }

                    composable(Screen.Expenses.route) {
                        ExpensesScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                    }

                    composable(Screen.Tasks.route) {
                        TasksScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                    }

                    composable(Screen.Yield.route) {
                        YieldScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                    }

                    composable(Screen.CloudSync.route) {
                        CloudSyncScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                    }
                }
            }
        }
    }
}
