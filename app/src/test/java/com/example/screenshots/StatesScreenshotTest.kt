package com.example.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.auth.UserProfile
import com.example.data.sync.SyncState
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
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
class StatesScreenshotTest : ScreenshotTestBase() {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun capture(subPath: String) {
        composeTestRule.waitForIdle()
        val target = File(baseDir, subPath)
        target.parentFile?.mkdirs()
        composeTestRule.onRoot(useUnmergedTree = true).captureRoboImage(filePath = target.absolutePath)
    }

    @Test
    fun captureEmptyHome() {
        // No seed data
        setAuthUser(UserProfile(testUserId, "kailash.patil@krushimitra.in", "Kailash Patil"))
        setSyncEngineState(SyncState.Synced(System.currentTimeMillis()))
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                HomeScreen(viewModel = viewModel, onNavigate = {}, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/states/empty_home.png")

        val emptyHome = File(baseDir, "docs/screenshots/states/empty_home.png")
        val emptyGeneral = File(baseDir, "docs/screenshots/states/empty.png")
        if (emptyHome.exists()) {
            emptyHome.copyTo(emptyGeneral, overwrite = true)
        }
    }

    @Test
    fun captureEmptyPlots() {
        setAuthUser(UserProfile(testUserId, "kailash.patil@krushimitra.in", "Kailash Patil"))
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                PlotsScreen(viewModel = viewModel, onNavigateToPlotDetail = {}, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/states/empty_plots.png")
    }

    @Test
    fun captureEmptyWorkers() {
        setAuthUser(UserProfile(testUserId, "kailash.patil@krushimitra.in", "Kailash Patil"))
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                WorkersScreen(viewModel = viewModel, onNavigateToWorkerDetail = {}, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/states/empty_workers.png")
    }

    @Test
    fun captureEmptyCrops() {
        setAuthUser(UserProfile(testUserId, "kailash.patil@krushimitra.in", "Kailash Patil"))
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                CropsScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/states/empty_crops.png")
    }

    @Test
    fun captureEmptyTasks() {
        setAuthUser(UserProfile(testUserId, "kailash.patil@krushimitra.in", "Kailash Patil"))
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                TasksScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/states/empty_tasks.png")
    }

    @Test
    fun captureEmptyExpenses() {
        setAuthUser(UserProfile(testUserId, "kailash.patil@krushimitra.in", "Kailash Patil"))
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ExpensesScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/states/empty_expenses.png")
    }

    @Test
    fun captureEmptyPayments() {
        setAuthUser(UserProfile(testUserId, "kailash.patil@krushimitra.in", "Kailash Patil"))
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                PaymentsScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/states/empty_payments.png")
    }

    @Test
    fun captureEmptyYield() {
        setAuthUser(UserProfile(testUserId, "kailash.patil@krushimitra.in", "Kailash Patil"))
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                YieldScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/states/empty_yield.png")
    }

    @Test
    fun captureSyncingState() {
        seedRealisticData()
        setAuthUser(UserProfile(testUserId, "kailash.patil@krushimitra.in", "Kailash Patil"))
        setSyncEngineState(SyncState.Syncing)
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                CloudSyncScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/states/syncing.png")
    }

    @Test
    fun captureOfflineState() {
        seedRealisticData()
        setAuthUser(UserProfile(testUserId, "kailash.patil@krushimitra.in", "Kailash Patil"))
        setSyncEngineState(SyncState.Offline)
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                CloudSyncScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/states/offline.png")
    }

    @Test
    fun captureSyncErrorState() {
        seedRealisticData()
        setAuthUser(UserProfile(testUserId, "kailash.patil@krushimitra.in", "Kailash Patil"))
        setSyncEngineState(SyncState.Error("Network timeout: Unable to reach Supabase sync cluster"))
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                CloudSyncScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/states/sync_error.png")
    }

    @Test
    fun captureSignedOutState() {
        setAuthUser(null)
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                CloudSyncScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        capture("docs/screenshots/states/signed_out.png")
    }
}
