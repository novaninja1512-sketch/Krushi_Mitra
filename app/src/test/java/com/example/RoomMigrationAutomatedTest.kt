package com.example

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.data.database.DatabaseMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RoomMigrationAutomatedTest {

    private fun createHelper(version: Int, callback: SupportSQLiteOpenHelper.Callback): SupportSQLiteOpenHelper {
        val config = SupportSQLiteOpenHelper.Configuration.builder(RuntimeEnvironment.getApplication())
            .name(null) // In-memory database
            .callback(callback)
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config)
    }

    private fun createV1Schema(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `plots` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `area` REAL NOT NULL,
                `areaUnit` TEXT NOT NULL,
                `soilType` TEXT NOT NULL,
                `irrigationType` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `archived` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `workers` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `mobileNumber` TEXT NOT NULL,
                `dailyWageRate` REAL NOT NULL,
                `joiningDate` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `archived` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `worker_transactions` (
                `id` TEXT NOT NULL,
                `workerId` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `date` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`workerId`) REFERENCES `workers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `expenses` (
                `id` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `description` TEXT NOT NULL,
                `plotId` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`plotId`) REFERENCES `plots`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `crop_assignments` (
                `id` TEXT NOT NULL,
                `plotId` TEXT NOT NULL,
                `cropName` TEXT NOT NULL,
                `variety` TEXT NOT NULL,
                `plantingDate` TEXT NOT NULL,
                `expectedHarvestDate` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `perennial` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`plotId`) REFERENCES `plots`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `yield_records` (
                `id` TEXT NOT NULL,
                `cropAssignmentId` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `quantity` REAL NOT NULL,
                `unit` TEXT NOT NULL,
                `ratePerUnit` REAL NOT NULL,
                `totalRevenue` REAL NOT NULL,
                `notes` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`cropAssignmentId`) REFERENCES `crop_assignments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `attendance` (
                `workerId` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                PRIMARY KEY(`workerId`, `date`),
                FOREIGN KEY(`workerId`) REFERENCES `workers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `daily_tasks` (
                `id` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `plotId` TEXT,
                `taskType` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `durationHours` REAL NOT NULL,
                `isCompleted` INTEGER NOT NULL,
                `notes` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`plotId`) REFERENCES `plots`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `task_worker_assignments` (
                `taskId` TEXT NOT NULL,
                `workerId` TEXT NOT NULL,
                PRIMARY KEY(`taskId`, `workerId`),
                FOREIGN KEY(`taskId`) REFERENCES `daily_tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`workerId`) REFERENCES `workers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
    }

    @Test
    fun testMigration1To2_AddsSyncColumnsAndPreservesData() {
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                createV1Schema(db)
                // Insert v1 test records
                db.execSQL("INSERT INTO plots (id, name, area, areaUnit, soilType, irrigationType, notes, archived) VALUES ('p1', 'North Plot', 2.5, 'Acre', 'Black', 'Drip', '', 0)")
                db.execSQL("INSERT INTO workers (id, name, mobileNumber, dailyWageRate, joiningDate, notes, archived) VALUES ('w1', 'Ramesh', '9876543210', 450.0, '2026-01-01', '', 0)")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val helper = createHelper(1, callback)
        val db = helper.writableDatabase

        // Execute Migration 1 -> 2
        DatabaseMigrations.MIGRATION_1_2.migrate(db)

        // Verify columns were added and data preserved
        val cursorPlots = db.query("SELECT id, name, area, syncStatus, userId FROM plots WHERE id = 'p1'")
        assertTrue(cursorPlots.moveToFirst())
        assertEquals("North Plot", cursorPlots.getString(cursorPlots.getColumnIndexOrThrow("name")))
        assertEquals("PENDING", cursorPlots.getString(cursorPlots.getColumnIndexOrThrow("syncStatus")))
        cursorPlots.close()

        val cursorWorkers = db.query("SELECT id, name, dailyWageRate, syncStatus FROM workers WHERE id = 'w1'")
        assertTrue(cursorWorkers.moveToFirst())
        assertEquals("Ramesh", cursorWorkers.getString(cursorWorkers.getColumnIndexOrThrow("name")))
        assertEquals(450.0, cursorWorkers.getDouble(cursorWorkers.getColumnIndexOrThrow("dailyWageRate")), 0.001)
        assertEquals("PENDING", cursorWorkers.getString(cursorWorkers.getColumnIndexOrThrow("syncStatus")))
        cursorWorkers.close()

        db.close()
    }

    @Test
    fun testMigration2To3_ConvertsMonetaryFieldsToPaiseAndPreservesData() {
        val callback = object : SupportSQLiteOpenHelper.Callback(2) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                createV1Schema(db)
                DatabaseMigrations.MIGRATION_1_2.migrate(db)

                // Insert v2 records with Double monetary values in rupees
                db.execSQL("INSERT INTO plots (id, name, area, areaUnit, soilType, irrigationType, notes, archived, syncStatus) VALUES ('p1', 'South Plot', 1.5, 'Acre', 'Clay', 'Flood', '', 0, 'SYNCED')")
                db.execSQL("INSERT INTO workers (id, name, mobileNumber, dailyWageRate, joiningDate, notes, archived, syncStatus) VALUES ('w1', 'Suresh', '9988776655', 450.50, '2026-02-01', '', 0, 'SYNCED')")
                db.execSQL("INSERT INTO worker_transactions (id, workerId, type, amount, date, notes, syncStatus) VALUES ('t1', 'w1', 'ADVANCE', 500.25, '2026-03-01', 'Advance', 'SYNCED')")
                db.execSQL("INSERT INTO expenses (id, date, category, amount, description, plotId, syncStatus) VALUES ('e1', '2026-03-05', 'Fertilizer', 1250.75, 'Urea', 'p1', 'SYNCED')")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val helper = createHelper(2, callback)
        val db = helper.writableDatabase

        // Execute Migration 2 -> 3
        DatabaseMigrations.MIGRATION_2_3.migrate(db)

        // Verify workers dailyWageRate converted to paise: 450.50 -> 45050L
        val cursorWorker = db.query("SELECT id, name, dailyWageRate FROM workers WHERE id = 'w1'")
        assertTrue(cursorWorker.moveToFirst())
        assertEquals(45050L, cursorWorker.getLong(cursorWorker.getColumnIndexOrThrow("dailyWageRate")))
        cursorWorker.close()

        // Verify worker_transactions amount converted to paise: 500.25 -> 50025L
        val cursorTx = db.query("SELECT id, amount, type FROM worker_transactions WHERE id = 't1'")
        assertTrue(cursorTx.moveToFirst())
        assertEquals(50025L, cursorTx.getLong(cursorTx.getColumnIndexOrThrow("amount")))
        cursorTx.close()

        // Verify expenses amount converted to paise: 1250.75 -> 125075L
        val cursorExp = db.query("SELECT id, amount, category FROM expenses WHERE id = 'e1'")
        assertTrue(cursorExp.moveToFirst())
        assertEquals(125075L, cursorExp.getLong(cursorExp.getColumnIndexOrThrow("amount")))
        cursorExp.close()

        db.close()
    }

    @Test
    fun testMigration1To3_SequentialDirectMigration() {
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                createV1Schema(db)
                // Insert v1 test records
                db.execSQL("INSERT INTO workers (id, name, mobileNumber, dailyWageRate, joiningDate, notes, archived) VALUES ('w1', 'Ganesh', '9123456780', 350.0, '2026-01-15', '', 0)")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val helper = createHelper(1, callback)
        val db = helper.writableDatabase

        // Execute direct Migration 1 -> 3
        DatabaseMigrations.MIGRATION_1_3.migrate(db)

        // Verify worker migrated directly: 350.0 -> 35000L and syncStatus is present
        val cursor = db.query("SELECT id, name, dailyWageRate, syncStatus FROM workers WHERE id = 'w1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("Ganesh", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        assertEquals(35000L, cursor.getLong(cursor.getColumnIndexOrThrow("dailyWageRate")))
        assertEquals("PENDING", cursor.getString(cursor.getColumnIndexOrThrow("syncStatus")))
        cursor.close()

        db.close()
    }
}
