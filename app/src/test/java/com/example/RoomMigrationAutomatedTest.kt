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

    @Test
    fun testMigration2To3_AdversarialPopulatedDatabase_ExactPaisePreservation() {
        val callback = object : SupportSQLiteOpenHelper.Callback(2) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                createV1Schema(db)
                DatabaseMigrations.MIGRATION_1_2.migrate(db)

                // Populate all 9 entities with realistic data including deleted records, pending sync, user IDs
                // and test exact rupee amounts: ₹0.01, ₹0.10, ₹0.50, ₹999.99, ₹1,250.50, ₹10,000.00
                db.execSQL("""
                    INSERT INTO plots (id, name, area, areaUnit, soilType, irrigationType, notes, archived, userId, createdAt, updatedAt, deletedAt, syncStatus)
                    VALUES ('plot-1', 'Main Plot', 5.0, 'Acre', 'Black', 'Drip', 'Good soil', 0, 'user-999', 1000, 2000, NULL, 'SYNCED')
                """)
                db.execSQL("""
                    INSERT INTO crop_assignments (id, plotId, cropName, variety, plantingDate, expectedHarvestDate, status, perennial, userId, createdAt, updatedAt, deletedAt, syncStatus)
                    VALUES ('crop-1', 'plot-1', 'Cotton', 'Bt', '2026-06-01', '2026-11-01', 'ACTIVE', 0, 'user-999', 1000, 2000, NULL, 'SYNCED')
                """)
                // Yield records with rate ₹0.01, ₹0.10, ₹0.50, ₹999.99
                db.execSQL("""
                    INSERT INTO yield_records (id, cropAssignmentId, date, quantity, unit, ratePerUnit, totalRevenue, notes, userId, createdAt, updatedAt, deletedAt, syncStatus)
                    VALUES ('yield-1', 'crop-1', '2026-10-01', 100.0, 'kg', 0.01, 1.00, 'Pennies test', 'user-999', 1000, 2000, NULL, 'SYNCED'),
                           ('yield-2', 'crop-1', '2026-10-02', 10.0, 'kg', 0.10, 1.00, 'Dimes test', 'user-999', 1000, 2000, NULL, 'SYNCED'),
                           ('yield-3', 'crop-1', '2026-10-03', 2.0, 'kg', 0.50, 1.00, 'Fifty paise test', 'user-999', 1000, 2000, NULL, 'SYNCED'),
                           ('yield-4', 'crop-1', '2026-10-04', 1.0, 'Quintal', 999.99, 999.99, 'Nine nine nine test', 'user-999', 1000, 2000, 2500, 'PENDING')
                """)
                // Workers with wage ₹1,250.50 and ₹10,000.00
                db.execSQL("""
                    INSERT INTO workers (id, name, mobileNumber, dailyWageRate, joiningDate, notes, archived, userId, createdAt, updatedAt, deletedAt, syncStatus)
                    VALUES ('worker-1', 'Kisan Leader', '9890001122', 1250.50, '2026-01-01', '', 0, 'user-999', 1000, 2000, NULL, 'SYNCED'),
                           ('worker-2', 'Master Agronomist', '9890001133', 10000.00, '2026-01-01', '', 0, 'user-999', 1000, 2000, 3000, 'SYNCED')
                """)
                // Attendance
                db.execSQL("""
                    INSERT INTO attendance (workerId, date, status, userId, createdAt, updatedAt, deletedAt, syncStatus)
                    VALUES ('worker-1', '2026-10-01', 'PRESENT', 'user-999', 1000, 2000, NULL, 'SYNCED')
                """)
                // Worker transactions with ₹1,250.50 and ₹0.50
                db.execSQL("""
                    INSERT INTO worker_transactions (id, workerId, type, amount, date, notes, userId, createdAt, updatedAt, deletedAt, syncStatus)
                    VALUES ('tx-1', 'worker-1', 'SALARY', 1250.50, '2026-10-02', 'Full day pay', 'user-999', 1000, 2000, NULL, 'SYNCED'),
                           ('tx-2', 'worker-1', 'ADVANCE', 0.50, '2026-10-03', 'Small advance', 'user-999', 1000, 2000, NULL, 'PENDING')
                """)
                // Tasks and Task-Worker assignments
                db.execSQL("""
                    INSERT INTO daily_tasks (id, date, plotId, taskType, description, durationHours, isCompleted, notes, userId, createdAt, updatedAt, deletedAt, syncStatus)
                    VALUES ('task-1', '2026-10-01', 'plot-1', 'Weeding', 'Manual weed removal', 4.0, 1, '', 'user-999', 1000, 2000, NULL, 'SYNCED')
                """)
                db.execSQL("""
                    INSERT INTO task_worker_assignments (taskId, workerId, userId, createdAt, updatedAt, deletedAt, syncStatus)
                    VALUES ('task-1', 'worker-1', 'user-999', 1000, 2000, NULL, 'SYNCED')
                """)
                // Expenses with ₹999.99 and ₹10,000.00
                db.execSQL("""
                    INSERT INTO expenses (id, date, category, amount, description, plotId, userId, createdAt, updatedAt, deletedAt, syncStatus)
                    VALUES ('exp-1', '2026-10-01', 'Pesticides', 999.99, 'Organic neem oil', 'plot-1', 'user-999', 1000, 2000, NULL, 'SYNCED'),
                           ('exp-2', '2026-10-02', 'Equipment Repair', 10000.00, 'Borewell pump repair', 'plot-1', 'user-999', 1000, 2000, NULL, 'PENDING')
                """)
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val helper = createHelper(2, callback)
        val db = helper.writableDatabase

        // Execute Migration 2 -> 3
        DatabaseMigrations.MIGRATION_2_3.migrate(db)

        // Verify exact paise conversions:
        // Worker 1: ₹1,250.50 -> 125050L
        val cW1 = db.query("SELECT dailyWageRate, syncStatus, userId FROM workers WHERE id = 'worker-1'")
        assertTrue(cW1.moveToFirst())
        assertEquals(125050L, cW1.getLong(0))
        assertEquals("SYNCED", cW1.getString(1))
        assertEquals("user-999", cW1.getString(2))
        cW1.close()

        // Worker 2 (deleted record): ₹10,000.00 -> 1000000L
        val cW2 = db.query("SELECT dailyWageRate, deletedAt FROM workers WHERE id = 'worker-2'")
        assertTrue(cW2.moveToFirst())
        assertEquals(1000000L, cW2.getLong(0))
        assertEquals(3000L, cW2.getLong(1))
        cW2.close()

        // Yield records: 0.01 -> 1L, 0.10 -> 10L, 0.50 -> 50L, 999.99 -> 99999L
        val cY1 = db.query("SELECT ratePerUnit, totalRevenue FROM yield_records WHERE id = 'yield-1'")
        assertTrue(cY1.moveToFirst())
        assertEquals(1L, cY1.getLong(0))
        assertEquals(100L, cY1.getLong(1))
        cY1.close()

        val cY2 = db.query("SELECT ratePerUnit FROM yield_records WHERE id = 'yield-2'")
        assertTrue(cY2.moveToFirst())
        assertEquals(10L, cY2.getLong(0))
        cY2.close()

        val cY3 = db.query("SELECT ratePerUnit FROM yield_records WHERE id = 'yield-3'")
        assertTrue(cY3.moveToFirst())
        assertEquals(50L, cY3.getLong(0))
        cY3.close()

        val cY4 = db.query("SELECT ratePerUnit, totalRevenue, deletedAt, syncStatus FROM yield_records WHERE id = 'yield-4'")
        assertTrue(cY4.moveToFirst())
        assertEquals(99999L, cY4.getLong(0))
        assertEquals(99999L, cY4.getLong(1))
        assertEquals(2500L, cY4.getLong(2))
        assertEquals("PENDING", cY4.getString(3))
        cY4.close()

        // Expenses: ₹999.99 -> 99999L, ₹10,000.00 -> 1000000L
        val cE1 = db.query("SELECT amount FROM expenses WHERE id = 'exp-1'")
        assertTrue(cE1.moveToFirst())
        assertEquals(99999L, cE1.getLong(0))
        cE1.close()

        val cE2 = db.query("SELECT amount, syncStatus FROM expenses WHERE id = 'exp-2'")
        assertTrue(cE2.moveToFirst())
        assertEquals(1000000L, cE2.getLong(0))
        assertEquals("PENDING", cE2.getString(1))
        cE2.close()

        // Worker Transactions: ₹1,250.50 -> 125050L, ₹0.50 -> 50L
        val cT1 = db.query("SELECT amount FROM worker_transactions WHERE id = 'tx-1'")
        assertTrue(cT1.moveToFirst())
        assertEquals(125050L, cT1.getLong(0))
        cT1.close()

        val cT2 = db.query("SELECT amount, syncStatus FROM worker_transactions WHERE id = 'tx-2'")
        assertTrue(cT2.moveToFirst())
        assertEquals(50L, cT2.getLong(0))
        assertEquals("PENDING", cT2.getString(1))
        cT2.close()

        db.close()
    }
}
