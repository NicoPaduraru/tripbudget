package de.tripbudget.data.repository

import de.tripbudget.data.local.TripDao
import de.tripbudget.data.local.TripEntity
import de.tripbudget.data.local.TravelerEntity
import de.tripbudget.data.local.TripSummaryRow
import de.tripbudget.data.local.TripWithTravelers
import de.tripbudget.domain.model.NewTrip
import de.tripbudget.domain.model.Traveler
import de.tripbudget.domain.model.TripDetail
import de.tripbudget.domain.model.TripSummary
import de.tripbudget.domain.repository.TripRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/*
 * Die Room-Umsetzung von TripRepository.
 * Hauptaufgabe: zwischen Datenbank-Klassen und Domain-Modellen übersetzen ("Mapping").
 */
class OfflineTripRepository @Inject constructor(
    private val tripDao: TripDao,
    private val clock: Clock,
) : TripRepository {

    override fun observeTrips(): Flow<List<TripSummary>> =
        tripDao.observeTripSummaries().map { rows -> rows.map { it.toDomain() } }

    // "?." – nur umwandeln, wenn die Reise existiert; sonst bleibt es null
    override fun observeTripDetail(tripId: Long): Flow<TripDetail?> =
        tripDao.observeTripWithTravelers(tripId).map { it?.toDomain() }

    override suspend fun createTrip(trip: NewTrip): Long {
        val entity = TripEntity(
            name = trip.name,
            destination = trip.destination,
            startDate = trip.startDate,
            endDate = trip.endDate,
            budgetMinorUnits = trip.budgetMinorUnits,
            currencyCode = trip.currencyCode,
            createdAtMillis = clock.millis(),
        )
        return tripDao.insertTripWithTravelers(entity, trip.otherTravelerNames, trip.myName)
    }
}

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

private fun TripWithTravelers.toDomain() = TripDetail(
    id = trip.id,
    name = trip.name,
    destination = trip.destination,
    startDate = trip.startDate,
    endDate = trip.endDate,
    budgetMinorUnits = trip.budgetMinorUnits,
    currencyCode = trip.currencyCode,
    // @Relation garantiert keine Reihenfolge → selbst sortieren: "ich" zuerst, dann nach Anlage
    travelers = travelers
        .sortedWith(compareByDescending<TravelerEntity> { it.isMe }.thenBy { it.id })
        .map { Traveler(id = it.id, name = it.name, isMe = it.isMe) },
)
