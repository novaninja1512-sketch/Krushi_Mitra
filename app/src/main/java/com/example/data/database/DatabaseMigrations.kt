package com.example.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    /**
     * Migration 1 -> 2:
     * Adds multi-device cloud synchronization columns (userId, createdAt, updatedAt, deletedAt, syncStatus)
     * and indices to all 9 local tables.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val tables = listOf(
                "workers",
                "attendance",
                "worker_transactions",
                "plots",
                "crop_assignments",
                "yield_records",
                "daily_tasks",
                "task_worker_assignments",
                "expenses"
            )

            for (table in tables) {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `userId` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `deletedAt` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'PENDING'")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_${table}_userId` ON `$table` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_${table}_updatedAt` ON `$table` (`updatedAt`)")
            }
        }
    }

    /**
     * Migration 2 -> 3:
     * Financial Data Precision Hardening.
     * Converts monetary Double columns (in rupees) to Long columns (in paise = rupees * 100).
     * Uses CAST(ROUND(value * 100) AS INTEGER) to eliminate IEEE 754 precision drift and preserve exact values.
     * Recreates tables and indices safely while preserving all foreign keys and existing data.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = OFF")

            // 1. workers: dailyWageRate REAL -> INTEGER (paise)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `workers_new` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `mobileNumber` TEXT NOT NULL,
                    `dailyWageRate` INTEGER NOT NULL DEFAULT 0,
                    `joiningDate` TEXT NOT NULL,
                    `notes` TEXT NOT NULL,
                    `archived` INTEGER NOT NULL DEFAULT 0,
                    `userId` TEXT DEFAULT NULL,
                    `createdAt` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL DEFAULT 0,
                    `deletedAt` INTEGER DEFAULT NULL,
                    `syncStatus` TEXT NOT NULL DEFAULT 'PENDING',
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())

            db.execSQL("""
                INSERT INTO `workers_new` (
                    `id`, `name`, `mobileNumber`, `dailyWageRate`, `joiningDate`, `notes`,
                    `archived`, `userId`, `createdAt`, `updatedAt`, `deletedAt`, `syncStatus`
                )
                SELECT
                    `id`, `name`, `mobileNumber`,
                    CAST(ROUND(`dailyWageRate` * 100) AS INTEGER),
                    `joiningDate`, `notes`, `archived`, `userId`, `createdAt`, `updatedAt`, `deletedAt`, `syncStatus`
                FROM `workers`
            """.trimIndent())

            db.execSQL("DROP TABLE `workers`")
            db.execSQL("ALTER TABLE `workers_new` RENAME TO `workers`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workers_name` ON `workers` (`name`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workers_userId` ON `workers` (`userId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workers_updatedAt` ON `workers` (`updatedAt`)")

            // 2. worker_transactions: amount REAL -> INTEGER (paise)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `worker_transactions_new` (
                    `id` TEXT NOT NULL,
                    `workerId` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `amount` INTEGER NOT NULL,
                    `date` TEXT NOT NULL,
                    `notes` TEXT NOT NULL,
                    `userId` TEXT DEFAULT NULL,
                    `createdAt` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL DEFAULT 0,
                    `deletedAt` INTEGER DEFAULT NULL,
                    `syncStatus` TEXT NOT NULL DEFAULT 'PENDING',
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`workerId`) REFERENCES `workers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())

            db.execSQL("""
                INSERT INTO `worker_transactions_new` (
                    `id`, `workerId`, `type`, `amount`, `date`, `notes`,
                    `userId`, `createdAt`, `updatedAt`, `deletedAt`, `syncStatus`
                )
                SELECT
                    `id`, `workerId`, `type`,
                    CAST(ROUND(`amount` * 100) AS INTEGER),
                    `date`, `notes`, `userId`, `createdAt`, `updatedAt`, `deletedAt`, `syncStatus`
                FROM `worker_transactions`
            """.trimIndent())

            db.execSQL("DROP TABLE `worker_transactions`")
            db.execSQL("ALTER TABLE `worker_transactions_new` RENAME TO `worker_transactions`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_transactions_workerId` ON `worker_transactions` (`workerId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_transactions_date` ON `worker_transactions` (`date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_transactions_userId` ON `worker_transactions` (`userId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_transactions_updatedAt` ON `worker_transactions` (`updatedAt`)")

            // 3. yield_records: ratePerUnit REAL -> INTEGER (paise), totalRevenue REAL -> INTEGER (paise)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `yield_records_new` (
                    `id` TEXT NOT NULL,
                    `cropAssignmentId` TEXT NOT NULL,
                    `date` TEXT NOT NULL,
                    `quantity` REAL NOT NULL,
                    `unit` TEXT NOT NULL,
                    `ratePerUnit` INTEGER NOT NULL DEFAULT 0,
                    `totalRevenue` INTEGER NOT NULL DEFAULT 0,
                    `notes` TEXT NOT NULL,
                    `userId` TEXT DEFAULT NULL,
                    `createdAt` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL DEFAULT 0,
                    `deletedAt` INTEGER DEFAULT NULL,
                    `syncStatus` TEXT NOT NULL DEFAULT 'PENDING',
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`cropAssignmentId`) REFERENCES `crop_assignments`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())

            db.execSQL("""
                INSERT INTO `yield_records_new` (
                    `id`, `cropAssignmentId`, `date`, `quantity`, `unit`, `ratePerUnit`, `totalRevenue`, `notes`,
                    `userId`, `createdAt`, `updatedAt`, `deletedAt`, `syncStatus`
                )
                SELECT
                    `id`, `cropAssignmentId`, `date`, `quantity`, `unit`,
                    CAST(ROUND(`ratePerUnit` * 100) AS INTEGER),
                    CAST(ROUND(`totalRevenue` * 100) AS INTEGER),
                    `notes`, `userId`, `createdAt`, `updatedAt`, `deletedAt`, `syncStatus`
                FROM `yield_records`
            """.trimIndent())

            db.execSQL("DROP TABLE `yield_records`")
            db.execSQL("ALTER TABLE `yield_records_new` RENAME TO `yield_records`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_yield_records_cropAssignmentId` ON `yield_records` (`cropAssignmentId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_yield_records_date` ON `yield_records` (`date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_yield_records_userId` ON `yield_records` (`userId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_yield_records_updatedAt` ON `yield_records` (`updatedAt`)")

            // 4. expenses: amount REAL -> INTEGER (paise)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `expenses_new` (
                    `id` TEXT NOT NULL,
                    `date` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `amount` INTEGER NOT NULL,
                    `description` TEXT NOT NULL,
                    `plotId` TEXT DEFAULT NULL,
                    `userId` TEXT DEFAULT NULL,
                    `createdAt` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL DEFAULT 0,
                    `deletedAt` INTEGER DEFAULT NULL,
                    `syncStatus` TEXT NOT NULL DEFAULT 'PENDING',
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`plotId`) REFERENCES `plots`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                )
            """.trimIndent())

            db.execSQL("""
                INSERT INTO `expenses_new` (
                    `id`, `date`, `category`, `amount`, `description`, `plotId`,
                    `userId`, `createdAt`, `updatedAt`, `deletedAt`, `syncStatus`
                )
                SELECT
                    `id`, `date`, `category`,
                    CAST(ROUND(`amount` * 100) AS INTEGER),
                    `description`, `plotId`, `userId`, `createdAt`, `updatedAt`, `deletedAt`, `syncStatus`
                FROM `expenses`
            """.trimIndent())

            db.execSQL("DROP TABLE `expenses`")
            db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_date` ON `expenses` (`date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_category` ON `expenses` (`category`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_plotId` ON `expenses` (`plotId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_userId` ON `expenses` (`userId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_updatedAt` ON `expenses` (`updatedAt`)")

            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    /**
     * Migration 1 -> 3:
     * Direct migration path from version 1 to version 3.
     * Sequentially applies MIGRATION_1_2 and MIGRATION_2_3.
     */
    val MIGRATION_1_3 = object : Migration(1, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MIGRATION_1_2.migrate(db)
            MIGRATION_2_3.migrate(db)
        }
    }
}
