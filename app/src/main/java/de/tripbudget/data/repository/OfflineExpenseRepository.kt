package de.tripbudget.data.repository

import de.tripbudget.data.local.ExpenseDao
import de.tripbudget.data.local.ExpenseEntity
import de.tripbudget.data.local.ExpenseShareEntity
import de.tripbudget.data.local.ExpenseWithShares
import de.tripbudget.domain.model.Expense
import de.tripbudget.domain.model.NewExpense
import de.tripbudget.domain.model.Share
import de.tripbudget.domain.repository.ExpenseRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** NEU IN PHASE 2: Room-Umsetzung von ExpenseRepository. */
class OfflineExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val clock: Clock,
) : ExpenseRepository {

    override fun observeExpenses(tripId: Long): Flow<List<Expense>> =
        expenseDao.observeExpensesWithShares(tripId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun addExpense(tripId: Long, expense: NewExpense, fromPlanItemId: Long?): Long {
        val entity = ExpenseEntity(
            tripId = tripId,
            paidByTravelerId = expense.paidByTravelerId,
            amountMinorUnits = expense.amountMinorUnits,
            category = expense.category,
            description = expense.description,
            date = expense.date,
            createdAtMillis = clock.millis(),
        )
        val shares = expense.shares.map {
            ExpenseShareEntity(expenseId = 0, travelerId = it.travelerId, amountMinorUnits = it.amountMinorUnits)
        }
        return expenseDao.insertExpenseWithShares(entity, shares, fromPlanItemId)
    }
}

/*
 * Beim Umwandeln prüft der init-Block von Expense automatisch, dass die
 * Anteile zum Betrag passen. Wäre die Datenbank je inkonsistent, fiele es hier auf.
 */
private fun ExpenseWithShares.toDomain() = Expense(
    id = expense.id,
    description = expense.description,
    category = expense.category,
    date = expense.date,
    paidByTravelerId = expense.paidByTravelerId,
    amountMinorUnits = expense.amountMinorUnits,
    shares = shares
        .sortedBy { it.travelerId }
        .map { Share(travelerId = it.travelerId, amountMinorUnits = it.amountMinorUnits) },
)
