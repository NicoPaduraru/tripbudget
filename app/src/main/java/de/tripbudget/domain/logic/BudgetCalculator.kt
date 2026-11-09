package de.tripbudget.domain.logic

import de.tripbudget.domain.model.Expense
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/*
 * NEU IN PHASE 2: Wie steht das Budget?
 *
 *   ausgegeben  = Summe aller Ausgaben der Gruppe
 *   übrig       = Budget − ausgegeben (kann negativ sein = überzogen)
 *   Tagesbudget = übrig ÷ verbleibende Reisetage (heute mitgezählt)
 *
 * "today" ist ein Parameter statt LocalDate.now() im Code.
 * Grund: Tests können so jedes Datum vorgeben ("Dependency Injection" im Kleinen).
 */

enum class BudgetStatus { ON_TRACK, WARNING, OVER }

data class BudgetSummary(
    val budgetMinorUnits: Long,
    val spentMinorUnits: Long,
    val remainingMinorUnits: Long,
    val usedPercent: Int,
    val status: BudgetStatus,
    /** Verbleibende Reisetage inkl. heute; 0 = Reise vorbei. */
    val daysLeft: Int,
    /** Wie viel pro Tag noch ausgegeben werden kann; null, wenn die Reise vorbei ist. */
    val dailyBudgetMinorUnits: Long?,
    /** NEU IN PHASE 4: geplante Kosten, die noch nicht als Ausgabe erfasst sind. */
    val plannedOpenMinorUnits: Long = 0,
) {
    /** Was übrig bliebe, wenn alles Geplante bezahlt ist. Kann negativ sein. */
    val remainingAfterPlanMinorUnits: Long get() = remainingMinorUnits - plannedOpenMinorUnits
}

object BudgetCalculator {

    /** Ab diesem Prozentsatz wird gewarnt (orange). */
    const val WARNING_PERCENT = 80

    fun summarize(
        budgetMinorUnits: Long,
        expenses: List<Expense>,
        startDate: LocalDate,
        endDate: LocalDate,
        today: LocalDate,
        plannedOpenMinorUnits: Long = 0, // NEU IN PHASE 4 (Standardwert: alte Aufrufe bleiben gültig)
    ): BudgetSummary {
        val spent = expenses.sumOf { it.amountMinorUnits }
        val remaining = budgetMinorUnits - spent
        val usedPercent = if (budgetMinorUnits > 0) (spent * 100 / budgetMinorUnits).toInt() else 0

        val status = when {
            spent > budgetMinorUnits -> BudgetStatus.OVER
            usedPercent >= WARNING_PERCENT -> BudgetStatus.WARNING
            else -> BudgetStatus.ON_TRACK
        }

        val daysLeft = when {
            today.isAfter(endDate) -> 0                                   // Reise vorbei
            today.isBefore(startDate) -> daysBetweenInclusive(startDate, endDate) // noch nicht begonnen
            else -> daysBetweenInclusive(today, endDate)                  // läuft gerade
        }

        val dailyBudget = if (daysLeft > 0) maxOf(remaining, 0L) / daysLeft else null

        return BudgetSummary(
            budgetMinorUnits = budgetMinorUnits,
            spentMinorUnits = spent,
            remainingMinorUnits = remaining,
            usedPercent = usedPercent,
            status = status,
            daysLeft = daysLeft,
            dailyBudgetMinorUnits = dailyBudget,
            plannedOpenMinorUnits = plannedOpenMinorUnits,
        )
    }

    /** 12.05. bis 19.05. = 8 Tage (beide Tage zählen mit). */
    fun daysBetweenInclusive(from: LocalDate, to: LocalDate): Int =
        (ChronoUnit.DAYS.between(from, to) + 1).toInt()
}
