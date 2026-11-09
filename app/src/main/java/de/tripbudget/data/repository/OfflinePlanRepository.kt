package de.tripbudget.data.repository

import de.tripbudget.data.local.PlanDao
import de.tripbudget.data.local.PlanItemEntity
import de.tripbudget.domain.model.NewPlanItem
import de.tripbudget.domain.model.PlanItem
import de.tripbudget.domain.repository.PlanRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** NEU IN PHASE 4: Room-Umsetzung von PlanRepository. */
class OfflinePlanRepository @Inject constructor(
    private val planDao: PlanDao,
    private val clock: Clock,
) : PlanRepository {

    override fun observePlanItems(tripId: Long): Flow<List<PlanItem>> =
        planDao.observePlanItems(tripId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getPlanItem(planItemId: Long): PlanItem? = planDao.getById(planItemId)?.toDomain()

    override suspend fun addPlanItem(tripId: Long, item: NewPlanItem): Long =
        planDao.insert(
            PlanItemEntity(
                tripId = tripId,
                date = item.date,
                time = item.time,
                title = item.title,
                location = item.location,
                plannedCostMinorUnits = item.plannedCostMinorUnits,
                note = item.note,
                expenseId = null,
                createdAtMillis = clock.millis(),
            )
        )

    override suspend fun deletePlanItem(planItemId: Long) = planDao.deleteById(planItemId)
}

private fun PlanItemEntity.toDomain() = PlanItem(
    id = id,
    date = date,
    time = time,
    title = title,
    location = location,
    plannedCostMinorUnits = plannedCostMinorUnits,
    note = note,
    expenseId = expenseId,
)
