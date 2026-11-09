package de.tripbudget.domain.repository

import de.tripbudget.domain.model.NewPlanItem
import de.tripbudget.domain.model.PlanItem
import kotlinx.coroutines.flow.Flow

/** NEU IN PHASE 4: der Reiseplan einer Reise. */
interface PlanRepository {
    /** Alle Aktivitäten, sortiert nach Datum und Uhrzeit. */
    fun observePlanItems(tripId: Long): Flow<List<PlanItem>>

    /** Eine einzelne Aktivität laden (z. B. zum Vorausfüllen einer Ausgabe). null = gibt es nicht. */
    suspend fun getPlanItem(planItemId: Long): PlanItem?

    suspend fun addPlanItem(tripId: Long, item: NewPlanItem): Long

    suspend fun deletePlanItem(planItemId: Long)
}
