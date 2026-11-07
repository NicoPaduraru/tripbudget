package de.tripbudget.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/*
 * DAO = Data Access Object: alle Datenbank-Operationen für Reisen.
 * Wir schreiben nur die Signaturen (und das SQL), Room erzeugt den Rest.
 *
 * Es ist eine "abstract class" (statt interface), weil wir eine Methode MIT
 * Inhalt brauchen: insertTripWithTravelers, die zwei Schritte bündelt.
 */
@Dao
abstract class TripDao {

    /*
     * Kein "suspend", sondern Flow: Room beobachtet die Tabellen und liefert
     * bei jeder Änderung automatisch eine neue Liste.
     * Die Unterabfrage zählt die Mitreisenden jeder Reise.
     * Sortierung: die Reise mit dem spätesten Startdatum zuerst.
     */
    @Query(
        """
        SELECT trips.*,
               (SELECT COUNT(*) FROM travelers WHERE travelers.tripId = trips.id) AS travelerCount
        FROM trips
        ORDER BY startDate DESC
        """
    )
    abstract fun observeTripSummaries(): Flow<List<TripSummaryRow>>

    /** Liefert die von der Datenbank vergebene ID zurück. */
    @Insert
    abstract suspend fun insertTrip(trip: TripEntity): Long

    @Insert
    abstract suspend fun insertTravelers(travelers: List<TravelerEntity>)

    /*
     * @Transaction: Beide Schritte gelingen gemeinsam – oder keiner.
     * Stürzt die App zwischen den Schritten ab, gibt es nie eine Reise ohne Mitreisende.
     */
    @Transaction
    open suspend fun insertTripWithTravelers(trip: TripEntity, travelerNames: List<String>, myName: String): Long {
        val tripId = insertTrip(trip)
        val me = TravelerEntity(tripId = tripId, name = myName, isMe = true)
        val others = travelerNames.map { TravelerEntity(tripId = tripId, name = it, isMe = false) }
        insertTravelers(listOf(me) + others)
        return tripId
    }
}
