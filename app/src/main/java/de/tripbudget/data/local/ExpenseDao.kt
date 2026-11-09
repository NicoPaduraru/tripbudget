package de.tripbudget.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/*
 * NEU IN PHASE 2: Datenbank-Operationen für Ausgaben.
 * Aufgebaut wie TripDao – dasselbe Muster, andere Tabellen.
 */
@Dao
abstract class ExpenseDao {

    /*
     * Alle Ausgaben einer Reise mit ihren Anteilen, neueste zuerst.
     * ":tripId" ist ein Platzhalter. Room setzt den Wert sicher ein
     * (Prepared Statement) – SQL-Injection ist so ausgeschlossen.
     * Der Flow meldet sich bei Änderungen in expenses UND expense_shares.
     */
    @Transaction
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC, id DESC")
    abstract fun observeExpensesWithShares(tripId: Long): Flow<List<ExpenseWithShares>>

    @Insert
    abstract suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert
    abstract suspend fun insertShares(shares: List<ExpenseShareEntity>)

    /**
     * Speichert eine Ausgabe und ihre Anteile in EINER Transaktion.
     * Die Anteile kommen mit expenseId = 0 herein; die echte ID kennen wir erst
     * nach dem Einfügen der Ausgabe und setzen sie dann mit copy().
     */
    /*
     * PHASE 4: optional mit planItemId. Dann wird die geplante Aktivität im SELBEN
     * Schritt als übernommen markiert. Entweder beides passiert – oder nichts.
     * (Ein DAO darf auch andere Tabellen ansprechen, hier plan_items.)
     */
    @Transaction
    open suspend fun insertExpenseWithShares(
        expense: ExpenseEntity,
        shares: List<ExpenseShareEntity>,
        planItemId: Long?,
    ): Long {
        val expenseId = insertExpense(expense)
        insertShares(shares.map { it.copy(expenseId = expenseId) })
        if (planItemId != null) linkPlanItem(planItemId, expenseId)
        return expenseId
    }

    @Query("UPDATE plan_items SET expenseId = :expenseId WHERE id = :planItemId")
    abstract suspend fun linkPlanItem(planItemId: Long, expenseId: Long)
}
