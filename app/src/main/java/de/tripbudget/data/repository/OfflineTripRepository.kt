package de.tripbudget.data.repository

import de.tripbudget.data.local.TripDao
import de.tripbudget.data.local.TripEntity
import de.tripbudget.data.local.TripSummaryRow
import de.tripbudget.domain.model.NewTrip
import de.tripbudget.domain.model.TripSummary
import de.tripbudget.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/*
 * Die Room-Umsetzung des TripRepository-Interfaces.
 * Ihre Hauptaufgabe: zwischen Datenbank-Klassen (Entities) und
 * Domain-Modellen übersetzen ("Mapping").
 *
 * "Offline", weil alle Daten nur lokal auf dem Gerät liegen.
 */
class OfflineTripRepository(
    private val tripDao: TripDao,
    // Eine Funktion als Parameter: liefert die aktuelle Zeit.
    // In Tests können wir eine feste Zeit übergeben.
    private val clock: () -> Long = { System.currentTimeMillis() },
) : TripRepository {

    override fun observeTrips(): Flow<List<TripSummary>> =
        // map auf dem Flow: jede neue Liste aus der Datenbank wird umgewandelt
        tripDao.observeTripSummaries().map { rows -> rows.map { it.toDomain() } }

    override suspend fun createTrip(trip: NewTrip): Long {
        val entity = TripEntity(
            name = trip.name,
            destination = trip.destination,
            startDate = trip.startDate,
            endDate = trip.endDate,
            budgetMinorUnits = trip.budgetMinorUnits,
            currencyCode = trip.currencyCode,
            createdAtMillis = clock(),
        )
        return tripDao.insertTripWithTravelers(entity, trip.otherTravelerNames, trip.myName)
    }
}

/** Extension Function: Datenbank-Zeile → Domain-Modell. Nur in dieser Datei sichtbar. */
private fun TripSummaryRow.toDomain() = TripSummary(
    id = trip.id,
    name = trip.name,
    destination = trip.destination,
    startDate = trip.startDate,
    endDate = trip.endDate,
    budgetMinorUnits = trip.budgetMinorUnits,
    currencyCode = trip.currencyCode,
    travelerCount = travelerCount,
)
