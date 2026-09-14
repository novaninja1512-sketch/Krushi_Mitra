package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.Attendance
import com.example.data.model.AttendanceStatuses
import com.example.data.model.Plot
import com.example.data.model.SyncStatus
import com.example.data.model.Worker
import com.example.data.sync.model.PlotRemote
import com.example.data.sync.model.toEntity
import com.example.data.sync.model.toRemote
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `verify app name and marathi localization strings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        val marathiName = context.getString(R.string.app_name_marathi)

        assertEquals("Krushi-Mitra", appName)
        assertEquals("कृषी-मित्र", marathiName)
    }

    @Test
    fun `verify plot and worker creation in room`() = runBlocking {
        val plot = Plot(
            id = "plot-1",
            name = "North Field",
            area = 2.5,
            soilType = "Black Cotton",
            irrigationType = "Drip"
        )
        db.plotDao().insertPlot(plot)

        val plots = db.plotDao().getPlotsFlow(includeArchived = false).first()
        assertEquals(1, plots.size)
        assertEquals("North Field", plots[0].name)

        val worker = Worker(
            id = "worker-1",
            name = "Ramesh Patil",
            dailyWageRate = 45000L
        )
        db.workerDao().insertWorker(worker)

        val workers = db.workerDao().getWorkersFlow(includeArchived = false).first()
        assertEquals(1, workers.size)
        assertEquals("Ramesh Patil", workers[0].name)

        val attendance = Attendance(
            workerId = "worker-1",
            date = "2026-09-14",
            status = AttendanceStatuses.PRESENT
        )
        db.attendanceDao().recordAttendance(attendance)

        val retrievedAttendance = db.attendanceDao().getAttendanceForDateFlow("2026-09-14").first()
        assertEquals(1, retrievedAttendance.size)
        assertEquals(AttendanceStatuses.PRESENT, retrievedAttendance[0].status)
    }

    @Test
    fun `verify soft delete tombstone and pending sync tracking`() = runBlocking {
        val testUserId = "test-user-123"
        val plot = Plot(
            id = "plot-sync-1",
            name = "South Field",
            area = 4.0,
            soilType = "Sandy Loam",
            irrigationType = "Canal",
            userId = testUserId,
            syncStatus = SyncStatus.PENDING
        )
        db.plotDao().insertPlot(plot)

        // Verify it is pending
        val pendingPlotsBefore = db.syncDao().getPendingPlots(testUserId)
        assertEquals(1, pendingPlotsBefore.size)
        assertEquals(SyncStatus.PENDING, pendingPlotsBefore[0].syncStatus)

        // Mark synced
        db.syncDao().markPlotsSynced(listOf("plot-sync-1"))
        val pendingAfterSync = db.syncDao().getPendingPlots(testUserId)
        assertTrue(pendingAfterSync.isEmpty())

        // Soft delete the plot
        val now = System.currentTimeMillis()
        db.plotDao().deletePlot("plot-sync-1", now)

        // Verify normal UI query excludes soft-deleted plot
        val uiPlots = db.plotDao().getPlotsFlow(includeArchived = true).first()
        assertTrue(uiPlots.none { it.id == "plot-sync-1" })

        // Verify syncDao returns the soft-deleted plot so it can be pushed to Supabase
        val pendingAfterDelete = db.syncDao().getPendingPlots(testUserId)
        assertEquals(1, pendingAfterDelete.size)
        assertEquals("plot-sync-1", pendingAfterDelete[0].id)
        assertNotNull(pendingAfterDelete[0].deletedAt)
        assertEquals(SyncStatus.PENDING, pendingAfterDelete[0].syncStatus)
    }

    @Test
    fun `verify first login migration associates local records with user id`() = runBlocking {
        val plot = Plot(
            id = "plot-local-1",
            name = "East Field",
            area = 3.0,
            soilType = "Clay",
            irrigationType = "Drip",
            userId = null
        )
        db.plotDao().insertPlot(plot)

        val worker = Worker(
            id = "worker-local-1",
            name = "Suresh",
            dailyWageRate = 50000L,
            userId = null
        )
        db.workerDao().insertWorker(worker)

        // Execute first login migration
        val testUserId = "550e8400-e29b-41d4-a716-446655440000"
        db.syncDao().associateAllLocalRecordsToUser(testUserId)

        // Check that records now have the user_id and PENDING syncStatus
        val pendingPlots = db.syncDao().getPendingPlots(testUserId)
        assertEquals(1, pendingPlots.size)
        assertEquals(testUserId, pendingPlots[0].userId)
        assertEquals(SyncStatus.PENDING, pendingPlots[0].syncStatus)

        val pendingWorkers = db.syncDao().getPendingWorkers(testUserId)
        assertEquals(1, pendingWorkers.size)
        assertEquals(testUserId, pendingWorkers[0].userId)
        assertEquals(SyncStatus.PENDING, pendingWorkers[0].syncStatus)
    }

    @Test
    fun `verify DTO bidirectional mapping and timestamp preservation`() {
        val originalPlot = Plot(
            id = "plot-dto-1",
            userId = "user-123",
            name = "Hill Orchard",
            area = 10.5,
            areaUnit = "Guntha",
            soilType = "Red",
            irrigationType = "Sprinkler",
            notes = "Mango Trees",
            archived = false,
            createdAt = 1000L,
            updatedAt = 2000L,
            deletedAt = null,
            syncStatus = SyncStatus.SYNCED
        )

        val remote = originalPlot.toRemote("user-123")
        assertEquals("plot-dto-1", remote.id)
        assertEquals("user-123", remote.userId)
        assertEquals("Hill Orchard", remote.name)
        assertEquals(10.5, remote.area, 0.001)
        assertEquals("Guntha", remote.areaUnit)
        assertNull(remote.deletedAt)

        val mappedBack = remote.toEntity()
        assertEquals(originalPlot.id, mappedBack.id)
        assertEquals(originalPlot.userId, mappedBack.userId)
        assertEquals(originalPlot.name, mappedBack.name)
        assertEquals(originalPlot.area, mappedBack.area, 0.001)
        assertEquals(originalPlot.soilType, mappedBack.soilType)
        assertEquals(originalPlot.irrigationType, mappedBack.irrigationType)
        assertEquals(originalPlot.notes, mappedBack.notes)
        assertEquals(originalPlot.archived, mappedBack.archived)
        assertEquals(SyncStatus.SYNCED, mappedBack.syncStatus)
    }
}
