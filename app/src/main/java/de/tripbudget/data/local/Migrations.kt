package de.tripbudget.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/*
 * MIGRATION von Datenbank-Version 1 (Phase 1) auf Version 2 (Phase 2).
 *
 * Problem: Auf deinem Handy liegt schon eine Datenbank mit Reisen (Version 1).
 * Die neue App erwartet zusätzlich die Tabellen expenses und expense_shares.
 * Ohne Migration würde Room die App beim Start abstürzen lassen,
 * statt deine Daten stillschweigend zu löschen.
 *
 * Lösung: Wir sagen Room genau, wie aus Version 1 Version 2 wird.
 * Hier: zwei neue Tabellen + Indizes anlegen. Bestehende Reisen bleiben unverändert.
 *
 * ACHTUNG: Das SQL muss EXAKT zu den Entities passen (Spaltennamen, Typen,
 * NOT NULL, Fremdschlüssel, Indexnamen). Room prüft das beim Öffnen und
 * stürzt mit "Migration didn't properly handle …" ab, wenn etwas abweicht.
 *
 * Ab jetzt exportiert Room das Schema als JSON in app/schemas/.
 * Künftige Migrationen können wir damit automatisch erzeugen lassen (AutoMigration).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `expenses` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `tripId` INTEGER NOT NULL,
                `paidByTravelerId` INTEGER NOT NULL,
                `amountMinorUnits` INTEGER NOT NULL,
                `category` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `date` INTEGER NOT NULL,
                `createdAtMillis` INTEGER NOT NULL,
                FOREIGN KEY(`tripId`) REFERENCES `trips`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`paidByTravelerId`) REFERENCES `travelers`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_tripId` ON `expenses` (`tripId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_paidByTravelerId` ON `expenses` (`paidByTravelerId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `expense_shares` (
                `expenseId` INTEGER NOT NULL,
                `travelerId` INTEGER NOT NULL,
                `amountMinorUnits` INTEGER NOT NULL,
                PRIMARY KEY(`expenseId`, `travelerId`),
                FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`travelerId`) REFERENCES `travelers`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_shares_travelerId` ON `expense_shares` (`travelerId`)")
    }
}
