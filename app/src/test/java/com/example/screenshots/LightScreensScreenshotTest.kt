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
class LightScreensScreenshotTest : ScreenshotTestBase() {

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
    fun captureHome() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                HomeScreen(viewModel = viewModel, onNavigate = {}, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/light/home.png")
    }

    @Test
    fun capturePlots() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                PlotsScreen(viewModel = viewModel, onNavigateToPlotDetail = {}, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/light/plots.png")
    }

    @Test
    fun capturePlotDetail() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                PlotDetailScreen(
                    plotId = "plot-1",
                    viewModel = viewModel,
                    onNavigateBack = {},
                    onNavigateToCrops = {}
                )
            }
        }
        capture("docs/screenshots/light/plot_detail.png")
    }

    @Test
    fun captureCrops() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                CropsScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/light/crops.png")
    }

    @Test
    fun captureWorkers() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                WorkersScreen(viewModel = viewModel, onNavigateToWorkerDetail = {}, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/light/workers.png")
    }

    @Test
    fun captureWorkerDetail() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                WorkerDetailScreen(
                    workerId = "worker-1",
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }
        capture("docs/screenshots/light/worker_detail.png")
    }

    @Test
    fun captureAttendance() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                AttendanceScreen(
                    viewModel = viewModel,
                    onNavigateToWorkers = {},
                    onOpenDrawer = {}
                )
            }
        }
        capture("docs/screenshots/light/attendance.png")
    }

    @Test
    fun capturePayments() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                PaymentsScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/light/payments.png")
    }

    @Test
    fun captureExpenses() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ExpensesScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/light/expenses.png")
    }

    @Test
    fun captureTasks() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                TasksScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/light/tasks.png")
    }

    @Test
    fun captureYield() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                YieldScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/light/yield.png")
    }

    @Test
    fun captureCloudSync() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                CloudSyncScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/light/cloud_sync.png")
    }
}
