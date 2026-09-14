package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.Attendance
import com.example.data.model.AttendanceStatuses
import com.example.data.model.Plot
import com.example.data.model.Worker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
            dailyWageRate = 450.0
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
}
