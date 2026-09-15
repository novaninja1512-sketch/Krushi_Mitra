package com.example.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.auth.UserProfile
import com.example.data.sync.SyncState
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class DarkScreensScreenshotTest : ScreenshotTestBase() {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        seedRealisticData()
        setAuthUser(UserProfile(testUserId, "kailash.patil@krushimitra.in", "Kailash Patil"))
        setSyncEngineState(SyncState.Synced(System.currentTimeMillis() - 120_000L))
    }

    private fun capture(subPath: String) {
        composeTestRule.waitForIdle()
        val target = File(baseDir, subPath)
        target.parentFile?.mkdirs()
        composeTestRule.onRoot(useUnmergedTree = true).captureRoboImage(filePath = target.absolutePath)
    }

    @Test
    fun captureDarkHome() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                HomeScreen(viewModel = viewModel, onNavigate = {}, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/dark/home.png")
    }

    @Test
    fun captureDarkPlots() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                PlotsScreen(viewModel = viewModel, onNavigateToPlotDetail = {}, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/dark/plots.png")
    }

    @Test
    fun captureDarkPlotDetail() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                PlotDetailScreen(
                    plotId = "plot-1",
                    viewModel = viewModel,
                    onNavigateBack = {},
                    onNavigateToCrops = {}
                )
            }
        }
        capture("docs/screenshots/dark/plot_detail.png")
    }

    @Test
    fun captureDarkCrops() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                CropsScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/dark/crops.png")
    }

    @Test
    fun captureDarkWorkers() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                WorkersScreen(viewModel = viewModel, onNavigateToWorkerDetail = {}, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/dark/workers.png")
    }

    @Test
    fun captureDarkWorkerDetail() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                WorkerDetailScreen(
                    workerId = "worker-1",
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }
        capture("docs/screenshots/dark/worker_detail.png")
    }

    @Test
    fun captureDarkAttendance() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                AttendanceScreen(
                    viewModel = viewModel,
                    onNavigateToWorkers = {},
                    onOpenDrawer = {}
                )
            }
        }
        capture("docs/screenshots/dark/attendance.png")
    }

    @Test
    fun captureDarkPayments() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                PaymentsScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/dark/payments.png")
    }

    @Test
    fun captureDarkExpenses() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                ExpensesScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/dark/expenses.png")
    }

    @Test
    fun captureDarkTasks() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                TasksScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/dark/tasks.png")
    }

    @Test
    fun captureDarkYield() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                YieldScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/dark/yield.png")
    }

    @Test
    fun captureDarkCloudSync() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                CloudSyncScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/dark/cloud_sync.png")
    }
}
