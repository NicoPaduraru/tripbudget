package de.tripbudget.di

import android.content.Context
import de.tripbudget.data.local.TripBudgetDatabase
import de.tripbudget.data.repository.OfflineTripRepository
import de.tripbudget.domain.repository.TripRepository

/*
 * DEPENDENCY INJECTION "von Hand".
 *
 * Problem: Die ViewModels brauchen ein Repository, das Repository braucht
 * ein DAO, das DAO braucht die Datenbank, die Datenbank braucht einen Context.
 * Wer baut das alles zusammen?
 *
 * Antwort: dieser Container. Er wird einmal beim App-Start erzeugt und
 * hält alle "Abhängigkeiten". In Phase 5 ersetzen wir ihn durch Hilt,
 * das genau diese Arbeit automatisch erledigt. Wer es einmal von Hand
 * gebaut hat, versteht Hilt viel besser.
 */
class AppContainer(context: Context) {

    // "by lazy": wird erst beim ersten Zugriff erzeugt, danach wiederverwendet.
    private val database: TripBudgetDatabase by lazy {
        TripBudgetDatabase.getInstance(context)
    }

    val tripRepository: TripRepository by lazy {
        OfflineTripRepository(database.tripDao())
    }
}
