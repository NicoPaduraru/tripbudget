package de.tripbudget.domain.logic

import de.tripbudget.domain.model.Expense
import de.tripbudget.domain.model.ExpenseCategory
import de.tripbudget.domain.model.Share
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BudgetCalculatorTest {

    private val start = LocalDate.of(2027, 5, 12)
    private val end = LocalDate.of(2027, 5, 19) // 8 Tage

    private fun expense(amount: Long) =
        Expense(0, "", ExpenseCategory.OTHER, start, 1, amount, listOf(Share(1, amount)))

    private fun summarize(budget: Long, spent: List<Long>, today: LocalDate) =
        BudgetCalculator.summarize(budget, spent.map { expense(it) }, start, end, today)

    @Test
    fun `Reisedauer zählt beide Tage`() {
        assertEquals(8, BudgetCalculator.daysBetweenInclusive(start, end))
        assertEquals(1, BudgetCalculator.daysBetweenInclusive(start, start))
    }

    @Test
    fun `ohne Ausgaben ist alles übrig`() {
        val s = summarize(80000, emptyList(), today = start)
        assertEquals(0L, s.spentMinorUnits)
        assertEquals(80000L, s.remainingMinorUnits)
        assertEquals(0, s.usedPercent)
        assertEquals(BudgetStatus.ON_TRACK, s.status)
    }

    @Test
    fun `vor der Reise wird über alle Tage verteilt`() {
        val s = summarize(80000, listOf(16000), today = start.minusDays(10))
        assertEquals(8, s.daysLeft)
        assertEquals(8000L, s.dailyBudgetMinorUnits) // 640 € / 8 Tage
    }

    @Test
    fun `während der Reise zählt heute mit`() {
        val s = summarize(80000, listOf(20000), today = LocalDate.of(2027, 5, 17)) // 17., 18., 19. = 3 Tage
        assertEquals(3, s.daysLeft)
        assertEquals(20000L, s.dailyBudgetMinorUnits) // 600 € / 3 Tage
    }

    @Test
    fun `nach der Reise gibt es kein Tagesbudget`() {
        val s = summarize(80000, listOf(20000), today = end.plusDays(1))
        assertEquals(0, s.daysLeft)
        assertNull(s.dailyBudgetMinorUnits)
    }

    @Test
    fun `Warnung ab 80 Prozent`() {
        assertEquals(BudgetStatus.ON_TRACK, summarize(10000, listOf(7999), start).status)
        assertEquals(BudgetStatus.WARNING, summarize(10000, listOf(8000), start).status)
        assertEquals(BudgetStatus.WARNING, summarize(10000, listOf(10000), start).status) // genau aufgebraucht
    }

    @Test
    fun `überzogenes Budget`() {
        val s = summarize(10000, listOf(6000, 6000), today = start)
        assertEquals(BudgetStatus.OVER, s.status)
        assertEquals(-2000L, s.remainingMinorUnits)
        assertEquals(120, s.usedPercent)
        assertEquals(0L, s.dailyBudgetMinorUnits) // nichts mehr übrig
    }
}
