package de.tripbudget.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/*
 * DAO = Data Access Object: alle Datenbank-Operationen für Reisen.
 * "abstract class" statt interface, weil insertTripWithTravelers einen Inhalt hat.
 */
@Dao
abstract class TripDao {

    @Query(
        """
        SELECT trips.*,
               (SELECT COUNT(*) FROM travelers WHERE travelers.tripId = trips.id) AS travelerCount
        FROM trips
        ORDER BY startDate DESC
        """
    )
    abstract fun observeTripSummaries(): Flow<List<TripSummaryRow>>

    /*
     * NEU IN PHASE 2: eine Reise mit ihren Mitreisenden.
     * @Transaction ist bei @Relation nötig: Room führt intern ZWEI Abfragen aus
     * (trips, dann travelers) und sorgt so dafür, dass beide zusammenpassen.
     * Rückgabe nullable (?): Gibt es die ID nicht, kommt null.
     */
    @Transaction
    @Query("SELECT * FROM trips WHERE id = :tripId")
    abstract fun observeTripWithTravelers(tripId: Long): Flow<TripWithTravelers?>

    @Insert
    abstract suspend fun insertTrip(trip: TripEntity): Long

    @Insert
    abstract suspend fun insertTravelers(travelers: List<TravelerEntity>)

    /** Beide Schritte gelingen gemeinsam – oder keiner. */
    @Transaction
    open suspend fun insertTripWithTravelers(trip: TripEntity, travelerNames: List<String>, myName: String): Long {
        val tripId = insertTrip(trip)
        val me = TravelerEntity(tripId = tripId, name = myName, isMe = true)
        val others = travelerNames.map { TravelerEntity(tripId = tripId, name = it, isMe = false) }
        insertTravelers(listOf(me) + others)
        return tripId
    }

    /* ------------------- ERWEITERUNG 1: bearbeiten und löschen ------------------- */

    /** Wie observeTripWithTravelers, aber nur EINMAL lesen (kein Flow) – fürs Formular. */
    @Transaction
    @Query("SELECT * FROM trips WHERE id = :tripId")
    abstract suspend fun getTripWithTravelers(tripId: Long): TripWithTravelers?

    /*
     * Welche Personen hängen an Daten? UNION verbindet vier Abfragen zu einer Liste
     * und entfernt doppelte IDs: wer bezahlt hat, wer mitgezahlt hat,
     * wer überwiesen und wer Geld bekommen hat.
     */
    @Query(
        """
        SELECT paidByTravelerId FROM expenses WHERE tripId = :tripId
        UNION
        SELECT expense_shares.travelerId FROM expense_shares
            JOIN expenses ON expenses.id = expense_shares.expenseId
            WHERE expenses.tripId = :tripId
        UNION
        SELECT fromTravelerId FROM settlements WHERE tripId = :tripId
        UNION
        SELECT toTravelerId FROM settlements WHERE tripId = :tripId
        """
    )
    abstract suspend fun getTravelerIdsInUse(tripId: Long): List<Long>

    @Query("SELECT COUNT(*) FROM expenses WHERE tripId = :tripId")
    abstract suspend fun countExpenses(tripId: Long): Int

    /*
     * Frühestes und spätestes Datum von Ausgaben UND Plan.
     * UNION ALL hängt beide Spalten untereinander, MIN/MAX sucht darin.
     * Gibt es nichts, liefert SQL NULL → Long? (Datum als "epochDay", siehe Converters).
     */
    @Query(
        """
        SELECT MIN(d) FROM (
            SELECT date AS d FROM expenses WHERE tripId = :tripId
            UNION ALL
            SELECT date AS d FROM plan_items WHERE tripId = :tripId
        )
        """
    )
    abstract suspend fun getFirstUsedEpochDay(tripId: Long): Long?

    @Query(
        """
        SELECT MAX(d) FROM (
            SELECT date AS d FROM expenses WHERE tripId = :tripId
            UNION ALL
            SELECT date AS d FROM plan_items WHERE tripId = :tripId
        )
        """
    )
    abstract suspend fun getLastUsedEpochDay(tripId: Long): Long?

    // @Update findet die Zeile über den Primärschlüssel (id) und überschreibt alle Spalten
    @Update
    abstract suspend fun updateTrip(trip: TripEntity)

    @Update
    abstract suspend fun updateTravelers(travelers: List<TravelerEntity>)

    /** Löscht alle Personen der Reise, deren ID NICHT in keepIds steht. */
    @Query("DELETE FROM travelers WHERE tripId = :tripId AND id NOT IN (:keepIds)")
    abstract suspend fun deleteTravelersExcept(tripId: Long, keepIds: List<Long>)

    /*
     * Reise ändern – alles in EINER Transaktion:
     *   1. Reisedaten überschreiben
     *   2. entfernte Personen löschen
     *   3. bestehende Personen umbenennen
     *   4. neue Personen anlegen
     * Bewusst kein @Upsert: Mit SQLCipher wirft Upsert eine andere Exception-Klasse,
     * als Room erwartet. @Update + @Insert getrennt ist eindeutig und sicher.
     */
    @Transaction
    open suspend fun updateTripWithTravelers(
        trip: TripEntity,
        existingTravelers: List<TravelerEntity>,
        newTravelers: List<TravelerEntity>,
    ) {
        updateTrip(trip)
        deleteTravelersExcept(trip.id, existingTravelers.map { it.id })
        updateTravelers(existingTravelers)
        if (newTravelers.isNotEmpty()) insertTravelers(newTravelers)
    }

    /*
     * Eine Reise löschen. Die Fremdschlüssel mit CASCADE (Entities.kt) löschen automatisch mit:
     * Mitreisende, Ausgaben (→ deren Anteile), Zahlungen und Plan-Aktivitäten.
     */
    @Query("DELETE FROM trips WHERE id = :tripId")
    abstract suspend fun deleteTrip(tripId: Long)
}
