package de.tripbudget.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.tripbudget.data.local.ExpenseDao
import de.tripbudget.data.local.PlanDao
import de.tripbudget.data.local.SettlementDao
import de.tripbudget.data.local.TripBudgetDatabase
import de.tripbudget.data.local.TripDao
import javax.inject.Singleton

/*
 * PHASE 5b: Ein Hilt-MODUL ist ein "Rezeptbuch".
 *
 * Manche Dinge kann Hilt nicht selbst bauen, weil wir den Konstruktor nicht in der Hand haben:
 *   - Die Datenbank entsteht über Room.databaseBuilder(...), nicht über "TripBudgetDatabase()".
 *   - Die DAOs erzeugt Room; wir bekommen sie nur über database.tripDao().
 * Für solche Fälle schreiben wir eine @Provides-Funktion: "So stellst du X her."
 *
 * @InstallIn(SingletonComponent::class): Diese Rezepte gelten für die ganze App
 * und leben so lange wie die App.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /*
     * @Singleton: Hilt ruft diese Funktion nur EIN Mal auf und merkt sich das Ergebnis.
     * Ohne @Singleton gäbe es bei jeder Anfrage eine neue Datenbank – das wäre ein Fehler
     * (mehrere offene Verbindungen, doppelte Arbeit, jedes Mal Entschlüsseln).
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TripBudgetDatabase =
        TripBudgetDatabase.create(context)

    // DAOs sind leichte Objekte. Hilt holt sie bei Bedarf aus der (einen) Datenbank.
    @Provides
    fun provideTripDao(database: TripBudgetDatabase): TripDao = database.tripDao()

    @Provides
    fun provideExpenseDao(database: TripBudgetDatabase): ExpenseDao = database.expenseDao()

    @Provides
    fun provideSettlementDao(database: TripBudgetDatabase): SettlementDao = database.settlementDao()

    @Provides
    fun providePlanDao(database: TripBudgetDatabase): PlanDao = database.planDao()
}
