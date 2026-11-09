package de.tripbudget.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** NEU IN PHASE 4: Datenbank-Operationen für den Reiseplan. */
@Dao
interface PlanDao {

    // Ganztägige Aktivitäten (time IS NULL) zuerst, dann nach Uhrzeit
    @Query(
        """
        SELECT * FROM plan_items
        WHERE tripId = :tripId
        ORDER BY date ASC, time IS NOT NULL, time ASC, title ASC
        """
    )
    fun observePlanItems(tripId: Long): Flow<List<PlanItemEntity>>

    /** suspend + Rückgabe direkt (kein Flow): einmal lesen, nicht beobachten. */
    @Query("SELECT * FROM plan_items WHERE id = :planItemId")
    suspend fun getById(planItemId: Long): PlanItemEntity?

    @Insert
    suspend fun insert(item: PlanItemEntity): Long

    @Query("DELETE FROM plan_items WHERE id = :planItemId")
    suspend fun deleteById(planItemId: Long)
}
