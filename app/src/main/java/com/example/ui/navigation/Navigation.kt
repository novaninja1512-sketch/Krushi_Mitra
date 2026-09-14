package com.example.ui.navigation

sealed class Screen(val route: String, val titleRes: Int) {
    object Home : Screen("home", com.example.R.string.nav_home)
    object Plots : Screen("plots", com.example.R.string.nav_plots)
    object PlotDetail : Screen("plot_detail/{plotId}", com.example.R.string.plot_detail) {
        fun createRoute(plotId: String) = "plot_detail/$plotId"
    }
    object Crops : Screen("crops", com.example.R.string.nav_crops)
    object Workers : Screen("workers", com.example.R.string.nav_workers)
    object WorkerDetail : Screen("worker_detail/{workerId}", com.example.R.string.worker_detail) {
        fun createRoute(workerId: String) = "worker_detail/$workerId"
    }
    object Attendance : Screen("attendance", com.example.R.string.nav_attendance)
    object Payments : Screen("payments", com.example.R.string.nav_payments)
    object Expenses : Screen("expenses", com.example.R.string.nav_expenses)
    object Tasks : Screen("tasks", com.example.R.string.nav_tasks)
    object Yield : Screen("yield", com.example.R.string.nav_yield)
    object CloudSync : Screen("cloud_sync", com.example.R.string.nav_cloud_sync)
}
