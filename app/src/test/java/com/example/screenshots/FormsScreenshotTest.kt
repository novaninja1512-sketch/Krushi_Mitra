package com.example.screenshots

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
class FormsScreenshotTest : ScreenshotTestBase() {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        seedRealisticData()
        setAuthUser(UserProfile(testUserId, "kailash.patil@krushimitra.in", "Kailash Patil"))
        setSyncEngineState(SyncState.Synced(System.currentTimeMillis() - 120_000L))
    }

    private fun capture(subPath: String) {
        val target = File(baseDir, subPath)
        target.parentFile?.mkdirs()
        composeTestRule.onRoot(useUnmergedTree = true).captureRoboImage(filePath = target.absolutePath)
    }

    @Test
    fun captureAddPlotForm() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                PlotsScreen(viewModel = viewModel, onNavigateToPlotDetail = {}, onOpenDrawer = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add_plot_fab").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        capture("docs/screenshots/forms/add_plot.png")
    }

    @Test
    fun captureEditPlotForm() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                PlotsScreen(viewModel = viewModel, onNavigateToPlotDetail = {}, onOpenDrawer = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithContentDescription("Edit Plot").onFirst().performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        capture("docs/screenshots/forms/edit_plot.png")
    }

    @Test
    fun captureAddCropForm() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                CropsScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add_crop_fab").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        capture("docs/screenshots/forms/add_crop.png")
    }

    @Test
    fun captureEditCropForm() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                CropsScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithContentDescription("More").onFirst().performClick()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.onNodeWithText("Edit Crop").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        capture("docs/screenshots/forms/edit_crop.png")
    }

    @Test
    fun captureAddWorkerForm() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                WorkersScreen(viewModel = viewModel, onNavigateToWorkerDetail = {}, onOpenDrawer = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add_worker_fab").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        capture("docs/screenshots/forms/add_worker.png")
    }

    @Test
    fun captureEditWorkerForm() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                WorkersScreen(viewModel = viewModel, onNavigateToWorkerDetail = {}, onOpenDrawer = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithContentDescription("Edit").onFirst().performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        capture("docs/screenshots/forms/edit_worker.png")
    }

    @Test
    fun captureRecordPaymentForm() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                PaymentsScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("record_payment_fab").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        capture("docs/screenshots/forms/record_payment.png")

        val paymentTarget = File(baseDir, "docs/screenshots/forms/record_payment.png")
        val addPaymentTarget = File(baseDir, "docs/screenshots/forms/add_payment.png")
        if (paymentTarget.exists()) {
            paymentTarget.copyTo(addPaymentTarget, overwrite = true)
        }
    }

    @Test
    fun captureAddExpenseForm() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ExpensesScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add_expense_fab").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        capture("docs/screenshots/forms/add_expense.png")
    }

    @Test
    fun captureAddTaskForm() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                TasksScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add_task_fab").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        capture("docs/screenshots/forms/add_task.png")
    }

    @Test
    fun captureAddYieldForm() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                YieldScreen(viewModel = viewModel, onOpenDrawer = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add_yield_fab").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        capture("docs/screenshots/forms/add_yield.png")
    }
}
