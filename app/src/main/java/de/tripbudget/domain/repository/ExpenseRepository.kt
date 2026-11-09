package de.tripbudget.domain.repository

import de.tripbudget.domain.model.Expense
import de.tripbudget.domain.model.NewExpense
import kotlinx.coroutines.flow.Flow

/*
 * NEU IN PHASE 2: Ausgaben einer Reise.
 * Eigenes Interface statt alles in TripRepository: Jede Klasse hat EINE
 * Aufgabe ("Single Responsibility Principle"). So bleiben die Dateien klein.
 */
interface ExpenseRepository {
    /** Alle Ausgaben einer Reise, neueste zuerst. */
    fun observeExpenses(tripId: Long): Flow<List<Expense>>

    /**
     * Speichert eine Ausgabe mit ihren Anteilen und liefert die neue ID.
     * PHASE 4: Kommt die Ausgabe aus dem Reiseplan, wird die Aktivität im selben
     * Schritt als "übernommen" markiert (fromPlanItemId).
     */
    suspend fun addExpense(tripId: Long, expense: NewExpense, fromPlanItemId: Long? = null): Long
}
