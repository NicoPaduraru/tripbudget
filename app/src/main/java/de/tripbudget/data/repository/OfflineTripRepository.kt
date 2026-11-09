package de.tripbudget.data.repository

import de.tripbudget.data.local.TripDao
import de.tripbudget.data.local.TripEntity
import de.tripbudget.data.local.TravelerEntity
import de.tripbudget.data.local.TripSummaryRow
import de.tripbudget.data.local.TripWithTravelers
import de.tripbudget.domain.model.NewTrip
import de.tripbudget.domain.model.Traveler
import de.tripbudget.domain.model.TripDetail
import de.tripbudget.domain.model.TripEditInfo
import de.tripbudget.domain.model.TripSummary
import de.tripbudget.domain.repository.TripRepository
import java.time.Clock
import java.time.LocalDate
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
        return tripDao.insertTripWithTravelers(entity, trip.others.map { it.name }, trip.me.name)
    }

    /* ---------------- ERWEITERUNG 1 ---------------- */

    override suspend fun getTripEditInfo(tripId: Long): TripEditInfo? {
        val trip = tripDao.getTripWithTravelers(tripId) ?: return null
        return TripEditInfo(
            trip = trip.toDomain(),
            travelerIdsInUse = tripDao.getTravelerIdsInUse(tripId).toSet(),
            hasExpenses = tripDao.countExpenses(tripId) > 0,
            firstUsedDate = tripDao.getFirstUsedEpochDay(tripId)?.let(LocalDate::ofEpochDay),
            lastUsedDate = tripDao.getLastUsedEpochDay(tripId)?.let(LocalDate::ofEpochDay),
        )
    }

    override suspend fun updateTrip(tripId: Long, trip: NewTrip) {
        // Die bestehende Zeile holen, damit createdAtMillis erhalten bleibt
        val current = tripDao.getTripWithTravelers(tripId) ?: return
        val entity = current.trip.copy(
            name = trip.name,
            destination = trip.destination,
            startDate = trip.startDate,
            endDate = trip.endDate,
            budgetMinorUnits = trip.budgetMinorUnits,
            currencyCode = trip.currencyCode,
        )
        // "ich" gibt es beim Bearbeiten immer schon – notfalls die ID aus der Datenbank nehmen
        val myId = trip.me.id ?: current.travelers.first { it.isMe }.id
        val me = TravelerEntity(id = myId, tripId = tripId, name = trip.me.name, isMe = true)

        // mapNotNull: nur Personen MIT ID (bestehende); "id?.let" läuft nur, wenn id nicht null ist
        val existing = trip.others.mapNotNull { draft ->
            draft.id?.let { id -> TravelerEntity(id = id, tripId = tripId, name = draft.name, isMe = false) }
        }
        val new = trip.others
            .filter { it.id == null }
            .map { TravelerEntity(tripId = tripId, name = it.name, isMe = false) }

        tripDao.updateTripWithTravelers(entity, existingTravelers = listOf(me) + existing, newTravelers = new)
    }

    override suspend fun deleteTrip(tripId: Long) = tripDao.deleteTrip(tripId)
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
