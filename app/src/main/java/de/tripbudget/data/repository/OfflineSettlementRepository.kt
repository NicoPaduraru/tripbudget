package de.tripbudget.data.repository

import de.tripbudget.data.local.SettlementDao
import de.tripbudget.data.local.SettlementEntity
import de.tripbudget.domain.model.Settlement
import de.tripbudget.domain.model.Transfer
import de.tripbudget.domain.repository.SettlementRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** NEU IN PHASE 3: Room-Umsetzung von SettlementRepository. */
class OfflineSettlementRepository @Inject constructor(
    private val settlementDao: SettlementDao,
    private val clock: Clock,
) : SettlementRepository {

    override fun observeSettlements(tripId: Long): Flow<List<Settlement>> =
        settlementDao.observeSettlements(tripId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun markAsPaid(tripId: Long, transfer: Transfer): Long =
        settlementDao.insert(
            SettlementEntity(
                tripId = tripId,
                fromTravelerId = transfer.fromTravelerId,
                toTravelerId = transfer.toTravelerId,
                amountMinorUnits = transfer.amountMinorUnits,
                paidAtMillis = clock.millis(),
            )
        )

    override suspend fun undo(settlementId: Long) = settlementDao.deleteById(settlementId)
}

private fun SettlementEntity.toDomain() = Settlement(
    id = id,
    fromTravelerId = fromTravelerId,
    toTravelerId = toTravelerId,
    amountMinorUnits = amountMinorUnits,
    paidAtMillis = paidAtMillis,
)
