package de.tripbudget.domain.logic

import de.tripbudget.domain.model.Expense
import de.tripbudget.domain.model.ExpenseCategory
import de.tripbudget.domain.model.Settlement
import de.tripbudget.domain.model.Transfer
import de.tripbudget.domain.model.Traveler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/*
 * Das Lissabon-Beispiel aus Phase 0, jetzt mit erledigten Zahlungen.
 * Erwartung (von Hand nachgerechnet in der Lernanleitung Phase 0):
 *   Anna +154,16 €, Ben −33,33 €, Clara −120,83 €
 */
class GroupSettlementCalculatorTest {

    private val anna = Traveler(1, "Anna", isMe = true)
    private val ben = Traveler(2, "Ben")
    private val clara = Traveler(3, "Clara")
    private val all = listOf(anna, ben, clara)
    private val day = LocalDate.of(2027, 5, 12)

    private fun expense(id: Long, paidBy: Traveler, cents: Long, forWhom: List<Traveler> = all) =
        Expense(id, "", ExpenseCategory.OTHER, day, paidBy.id, cents, CostSplitter.splitEvenly(cents, forWhom.map { it.id }))

    private val lisbon = listOf(
        expense(1, anna, 30000),
        expense(2, ben, 9000),
        expense(3, clara, 2500, listOf(anna, clara)),
        expense(4, ben, 1000),
    )

    private fun paid(id: Long, from: Traveler, to: Traveler, cents: Long) =
        Settlement(id, from.id, to.id, cents, paidAtMillis = 0)

    @Test
    fun `Salden, bezahlt und Anteil pro Person`() {
        val summary = GroupSettlementCalculator.compute(all, lisbon, emptyList())
        val (a, b, c) = summary.balances // Destructuring einer Liste
        assertEquals(30000L, a.paidMinorUnits)
        assertEquals(14584L, a.shareMinorUnits) // 100 + 30 + 12,50 + 3,34
        assertEquals(15416L, a.balanceMinorUnits)
        assertEquals(10000L, b.paidMinorUnits)
        assertEquals(-3333L, b.balanceMinorUnits)
        assertEquals(-12083L, c.balanceMinorUnits)
    }

    @Test
    fun `Reihenfolge der Personen bleibt wie in der Reise`() {
        val summary = GroupSettlementCalculator.compute(all, lisbon, emptyList())
        assertEquals(listOf("Anna", "Ben", "Clara"), summary.balances.map { it.traveler.name })
    }

    @Test
    fun `Vorschlag ohne erledigte Zahlungen`() {
        val summary = GroupSettlementCalculator.compute(all, lisbon, emptyList())
        assertEquals(
            listOf(Transfer(3, 1, 12083), Transfer(2, 1, 3333)),
            summary.suggestions,
        )
        assertFalse(summary.isSettled)
    }

    @Test
    fun `nach einer erledigten Zahlung bleibt nur der Rest`() {
        val summary = GroupSettlementCalculator.compute(all, lisbon, listOf(paid(1, clara, anna, 12083)))
        assertEquals(0L, summary.balances[2].balanceMinorUnits) // Clara ist quitt
        assertEquals(listOf(Transfer(2, 1, 3333)), summary.suggestions)
    }

    @Test
    fun `alle Zahlungen erledigt - Gruppe ist quitt`() {
        val settlements = listOf(paid(1, clara, anna, 12083), paid(2, ben, anna, 3333))
        val summary = GroupSettlementCalculator.compute(all, lisbon, settlements)
        assertTrue(summary.isSettled)
        assertTrue(summary.balances.all { it.balanceMinorUnits == 0L })
        // "bezahlt" und "Anteil" ändern sich durch Ausgleichszahlungen nicht
        assertEquals(30000L, summary.balances[0].paidMinorUnits)
    }

    @Test
    fun `Teilzahlung verringert die Schuld`() {
        val summary = GroupSettlementCalculator.compute(all, lisbon, listOf(paid(1, clara, anna, 10000)))
        assertEquals(-2083L, summary.balances[2].balanceMinorUnits)
    }

    @Test
    fun `neue Ausgabe nach der Abrechnung erzeugt neue Vorschläge`() {
        val settlements = listOf(paid(1, clara, anna, 12083), paid(2, ben, anna, 3333))
        val later = lisbon + expense(5, ben, 3000) // Ben zahlt noch 30 € für alle
        val summary = GroupSettlementCalculator.compute(all, later, settlements)
        assertEquals(listOf(Transfer(1, 2, 1000), Transfer(3, 2, 1000)), summary.suggestions)
    }

    @Test
    fun `Reise ohne Ausgaben ist quitt`() {
        val summary = GroupSettlementCalculator.compute(all, emptyList(), emptyList())
        assertTrue(summary.isSettled)
        assertTrue(summary.balances.all { it.paidMinorUnits == 0L && it.balanceMinorUnits == 0L })
    }
}
