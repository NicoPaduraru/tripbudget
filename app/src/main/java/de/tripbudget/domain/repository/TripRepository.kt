package de.tripbudget.domain.repository

import de.tripbudget.domain.model.NewTrip
import de.tripbudget.domain.model.TripDetail
import de.tripbudget.domain.model.TripEditInfo
import de.tripbudget.domain.model.TripSummary
import kotlinx.coroutines.flow.Flow

/*
 * Ein INTERFACE: Es beschreibt, WAS man mit Reisen machen kann, nicht WIE.
 * Die Umsetzung mit Room steckt in data/repository/OfflineTripRepository.kt.
 *
 * Flow = ein Datenstrom, der bei jeder Änderung in der Datenbank
 * automatisch neue Werte liefert.
 */
interface TripRepository {
    fun observeTrips(): Flow<List<TripSummary>>

    /** NEU IN PHASE 2: eine Reise mit Mitreisenden. null, wenn es die ID nicht (mehr) gibt. */
    fun observeTripDetail(tripId: Long): Flow<TripDetail?>

    /** Speichert Reise + Mitreisende und liefert die neue Reise-ID. */
    suspend fun createTrip(trip: NewTrip): Long

    /** ERWEITERUNG 1: alles fürs Bearbeiten-Formular. null, wenn es die Reise nicht gibt. */
    suspend fun getTripEditInfo(tripId: Long): TripEditInfo?

    /** ERWEITERUNG 1: Reise und Mitreisende ändern. Fehlende bestehende Personen werden entfernt. */
    suspend fun updateTrip(tripId: Long, trip: NewTrip)

    /** ERWEITERUNG 1: Reise mit allen Ausgaben, Plänen und Zahlungen löschen. */
    suspend fun deleteTrip(tripId: Long)
}
