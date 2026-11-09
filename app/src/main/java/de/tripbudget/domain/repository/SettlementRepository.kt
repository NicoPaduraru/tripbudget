package de.tripbudget.domain.repository

import de.tripbudget.domain.model.Settlement
import de.tripbudget.domain.model.Transfer
import kotlinx.coroutines.flow.Flow

/** NEU IN PHASE 3: erledigte Ausgleichszahlungen einer Reise. */
interface SettlementRepository {
    /** Alle erledigten Zahlungen, neueste zuerst. */
    fun observeSettlements(tripId: Long): Flow<List<Settlement>>

    /** Markiert eine vorgeschlagene Überweisung als bezahlt. */
    suspend fun markAsPaid(tripId: Long, transfer: Transfer): Long

    /** Macht "bezahlt" rückgängig (z. B. versehentlich angetippt). */
    suspend fun undo(settlementId: Long)
}
