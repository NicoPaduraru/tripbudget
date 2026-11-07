package de.tripbudget.domain.repository

import de.tripbudget.domain.model.NewTrip
import de.tripbudget.domain.model.TripSummary
import kotlinx.coroutines.flow.Flow

/*
 * Ein INTERFACE: Es beschreibt, WAS man mit Reisen machen kann, nicht WIE.
 * Die Umsetzung mit Room steckt in data/repository/OfflineTripRepository.kt.
 *
 * Vorteil: Die ViewModels kennen nur dieses Interface. In Tests können wir
 * eine einfache Fake-Version ohne Datenbank einsetzen.
 *
 * Flow = ein Datenstrom. Jedes Mal, wenn sich die Reisen in der Datenbank
 * ändern, liefert der Flow automatisch die neue Liste. Die UI aktualisiert
 * sich dadurch von selbst.
 *
 * "suspend" = die Funktion kann warten (z. B. auf die Datenbank), ohne die
 * Oberfläche zu blockieren. Mehr dazu in der Lernanleitung (Coroutines).
 */
interface TripRepository {
    fun observeTrips(): Flow<List<TripSummary>>

    /** Speichert Reise + Mitreisende und liefert die neue Reise-ID. */
    suspend fun createTrip(trip: NewTrip): Long
}
