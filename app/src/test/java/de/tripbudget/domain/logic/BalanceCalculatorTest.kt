package de.tripbudget.domain.logic

import de.tripbudget.domain.model.Expense
import de.tripbudget.domain.model.Share
import de.tripbudget.domain.model.Transfer
import de.tripbudget.domain.model.Traveler
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertThrows

class BalanceCalculatorTest {

    private val anna = Traveler(1, "Anna")
    private val ben = Traveler(2, "Ben")
    private val clara = Traveler(3, "Clara")
    private val all = listOf(anna, ben, clara)

    /** Hilfsfunktion, damit die Tests kurz und lesbar bleiben. */
    private fun expense(paidBy: Traveler, cents: Long, forWhom: List<Traveler> = all) =
        Expense(0, "Test", paidBy.id, cents, CostSplitter.splitEvenly(cents, forWhom.map { it.id }))

    @Test
    fun `ohne Ausgaben sind alle ausgeglichen`() {
        val balances = BalanceCalculator.calculate(all, emptyList())
        assertEquals(mapOf(1L to 0L, 2L to 0L, 3L to 0L), balances)
    }

    @Test
    fun `Zahler bekommt den Anteil der anderen zurück`() {
        // Anna zahlt 90 € für alle → jeder verbraucht 30 €.
        val balances = BalanceCalculator.calculate(all, listOf(expense(anna, 9000)))
        assertEquals(6000L, balances[anna.id])  // bekommt 60 €
        assertEquals(-3000L, balances[ben.id])  // schuldet 30 €
        assertEquals(-3000L, balances[clara.id])
    }

    @Test
    fun `Ausgabe nur für einen Teil der Gruppe`() {
        // Ben zahlt 20 € Taxi nur für sich und Clara.
        val balances = BalanceCalculator.calculate(all, listOf(expense(ben, 2000, listOf(ben, clara))))
        assertEquals(0L, balances[anna.id])
        assertEquals(1000L, balances[ben.id])
        assertEquals(-1000L, balances[clara.id])
    }

    @Test
    fun `erledigte Ausgleichszahlung verringert die Schulden`() {
        val expenses = listOf(expense(anna, 9000))
        val paid = listOf(Transfer(fromTravelerId = ben.id, toTravelerId = anna.id, amountCents = 3000))
        val balances = BalanceCalculator.calculate(all, expenses, paid)
        assertEquals(3000L, balances[anna.id])
        assertEquals(0L, balances[ben.id])
        assertEquals(-3000L, balances[clara.id])
    }

    @Test
    fun `Summe aller Salden ist immer 0`() {
        val expenses = listOf(
            expense(anna, 30001),
            expense(ben, 777, listOf(ben, clara)),
            expense(clara, 1999),
        )
        val balances = BalanceCalculator.calculate(all, expenses)
        assertEquals(0L, balances.values.sum())
    }

    @Test
    fun `unbekannte Person führt zu einem Fehler`() {
        val stranger = Expense(0, "Fremd", 99, 100, listOf(Share(99, 100)))
        assertThrows(IllegalArgumentException::class.java) { BalanceCalculator.calculate(all, listOf(stranger)) }
    }
}
