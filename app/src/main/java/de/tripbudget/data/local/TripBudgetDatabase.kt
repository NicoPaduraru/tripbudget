package de.tripbudget.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.tripbudget.data.security.DatabaseEncryptor
import de.tripbudget.data.security.DatabasePassphraseStore
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/*
 * Die Datenbank.
 *
 * Versionsgeschichte:
 *   1 (Phase 1): trips, travelers
 *   2 (Phase 2): + expenses, expense_shares   → MIGRATION_1_2, von Hand geschrieben
 *   3 (Phase 3): + settlements                → AutoMigration, von Room erzeugt
 *   4 (Phase 4): + plan_items                 → AutoMigration
 *
 * PHASE 5: Die Datei ist jetzt mit SQLCipher VERSCHLÜSSELT (AES-256).
 * Die Tabellen bleiben gleich, deshalb gibt es KEINE neue Version.
 * Room merkt davon nichts: Wir tauschen nur die "Open Helper Factory" aus –
 * die Schicht, die die Datei öffnet.
 */
@Database(
    entities = [
        TripEntity::class,
        TravelerEntity::class,
        ExpenseEntity::class,
        ExpenseShareEntity::class,
        SettlementEntity::class,
        PlanItemEntity::class,
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ],
)
@TypeConverters(Converters::class)
abstract class TripBudgetDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun settlementDao(): SettlementDao
    abstract fun planDao(): PlanDao

    companion object {
        const val DATABASE_NAME = "tripbudget.db"

        /*
         * PHASE 5b: Früher stand hier ein "Singleton von Hand" (getInstance mit
         * @Volatile und synchronized). Das übernimmt jetzt Hilt: DatabaseModule
         * ruft create() genau EINMAL auf (@Singleton) und gibt allen dieselbe Datenbank.
         */
        fun create(context: Context): TripBudgetDatabase {
            // 1. Die SQLCipher-Bibliothek (in C geschrieben) laden – einmal pro App-Start
            System.loadLibrary("sqlcipher")

            // 2. Passwort holen (beim ersten Mal: zufällig erzeugen, mit Keystore schützen)
            val passphrase = DatabasePassphraseStore(context).getOrCreatePassphrase()

            // 3. Eine alte, unverschlüsselte Datenbank (Phase 1–4) einmalig umwandeln
            DatabaseEncryptor.encryptIfPlaintext(context, DATABASE_NAME, passphrase)

            // 4. Room mit dem verschlüsselnden Open Helper von SQLCipher.
            //    copyOf(): SQLCipher darf seine Kopie des Passworts nach Gebrauch überschreiben.
            return Room.databaseBuilder(context, TripBudgetDatabase::class.java, DATABASE_NAME)
                .openHelperFactory(SupportOpenHelperFactory(passphrase.copyOf()))
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
