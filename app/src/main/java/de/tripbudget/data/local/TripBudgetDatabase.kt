package de.tripbudget.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/*
 * Die Datenbank selbst. Sie kennt alle Tabellen (entities) und alle DAOs.
 *
 * version = 1: Ändern wir später die Tabellen (z. B. in Phase 2 die Ausgaben),
 * erhöhen wir die Version und schreiben eine "Migration".
 *
 * exportSchema = false: vorerst kein Export des Schemas als JSON-Datei.
 * Das richten wir ein, sobald wir die erste Migration brauchen.
 */
@Database(
    entities = [TripEntity::class, TravelerEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class TripBudgetDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao

    /*
     * companion object = was in Java "static" wäre.
     * Hier: ein SINGLETON. Es darf nur EINE Datenbank-Verbindung in der App geben.
     */
    companion object {
        // @Volatile: Änderungen an dieser Variable sind sofort für alle Threads sichtbar.
        @Volatile
        private var instance: TripBudgetDatabase? = null

        fun getInstance(context: Context): TripBudgetDatabase =
            // Gibt es schon eine Instanz? Dann nimm sie. Sonst: sicher (synchronized) eine anlegen.
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TripBudgetDatabase::class.java,
                    "tripbudget.db",
                ).build().also { instance = it }
            }
    }
}
