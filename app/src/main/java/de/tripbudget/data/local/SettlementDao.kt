package de.tripbudget.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/*
 * NEU IN PHASE 3: Datenbank-Operationen für erledigte Ausgleichszahlungen.
 * Diesmal ein einfaches "interface" – wir brauchen keine Methode mit eigenem Inhalt.
 */
@Dao
interface SettlementDao {

    @Query("SELECT * FROM settlements WHERE tripId = :tripId ORDER BY paidAtMillis DESC, id DESC")
    fun observeSettlements(tripId: Long): Flow<List<SettlementEntity>>

    @Insert
    suspend fun insert(settlement: SettlementEntity): Long

    @Query("DELETE FROM settlements WHERE id = :settlementId")
    suspend fun deleteById(settlementId: Long)
}
