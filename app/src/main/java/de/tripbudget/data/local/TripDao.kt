package de.tripbudget.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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
}
