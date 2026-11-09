package de.tripbudget.domain.logic

import de.tripbudget.domain.model.Expense
import de.tripbudget.domain.model.ExpenseCategory
import de.tripbudget.domain.model.PlanItem
import de.tripbudget.domain.model.Share
import de.tripbudget.domain.util.MoneyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/** NEU IN PHASE 4: Tests für Reiseplan, Formular und Budget mit geplanten Kosten. */
class PlanTest {

    private val start = LocalDate.of(2027, 5, 12)
    private val end = LocalDate.of(2027, 5, 14) // 3 Tage

    private fun item(
        id: Long,
        date: LocalDate,
        time: LocalTime? = null,
        title: String = "Aktivität $id",
        cost: Long? = null,
        expenseId: Long? = null,
    ) = PlanItem(id, date, time, title, "", cost, "", expenseId)

    /* ---------- PlanSchedule ---------- */

    @Test
    fun `jeder Reisetag erscheint, auch ohne Aktivitäten`() {
        val days = PlanSchedule.buildDays(start, end, emptyList())
        assertEquals(listOf(1, 2, 3), days.map { it.dayNumber })
        assertEquals(listOf(start, start.plusDays(1), end), days.map { it.date })
        assertTrue(days.all { it.items.isEmpty() })
    }

    @Test
    fun `ganztägig zuerst, dann nach Uhrzeit`() {
        val items = listOf(
            item(1, start, LocalTime.of(19, 30), "Abendessen"),
            item(2, start, null, "Strandtag"),
            item(3, start, LocalTime.of(9, 0), "Frühstück"),
        )
        val day1 = PlanSchedule.buildDays(start, end, items).first()
        assertEquals(listOf("Strandtag", "Frühstück", "Abendessen"), day1.items.map { it.title })
    }

    @Test
    fun `offene Kosten zählen übernommene Aktivitäten nicht mit`() {
        val items = listOf(
            item(1, start, cost = 4000),
            item(2, start, cost = 2500, expenseId = 7), // schon als Ausgabe erfasst
            item(3, start, cost = null),
        )
        assertEquals(4000L, PlanSchedule.openCostOf(items))
        assertEquals(4000L, PlanSchedule.buildDays(start, end, items).first().openCostMinorUnits)
    }

    /* ---------- PlanFormValidator ---------- */

    private val validInput = PlanFormInput(title = " Surfkurs ", date = start.plusDays(1), costText = "45,50")

    private fun validate(input: PlanFormInput) = PlanFormValidator.validate(input, "EUR", start, end)

    @Test
    fun `gültige Aktivität`() {
        val item = (validate(validInput) as PlanFormResult.Valid).item
        assertEquals("Surfkurs", item.title)
        assertEquals(4550L, item.plannedCostMinorUnits)
        assertNull(item.time)
    }

    @Test
    fun `Kosten sind optional, 0 bedeutet keine Kosten`() {
        assertNull((validate(validInput.copy(costText = "")) as PlanFormResult.Valid).item.plannedCostMinorUnits)
        assertNull((validate(validInput.copy(costText = "0")) as PlanFormResult.Valid).item.plannedCostMinorUnits)
    }

    @Test
    fun `ungültige Kosten, fehlender Titel`() {
        val result = validate(validInput.copy(title = "  ", costText = "abc")) as PlanFormResult.Invalid
        assertEquals(setOf(PlanFormError.TITLE_EMPTY, PlanFormError.COST_INVALID), result.errors)
    }

    @Test
    fun `Datum muss innerhalb der Reise liegen`() {
        val before = validate(validInput.copy(date = start.minusDays(1))) as PlanFormResult.Invalid
        assertEquals(setOf(PlanFormError.DATE_OUTSIDE_TRIP), before.errors)
        val missing = validate(validInput.copy(date = null)) as PlanFormResult.Invalid
        assertEquals(setOf(PlanFormError.DATE_MISSING), missing.errors)
        // erster und letzter Tag sind erlaubt
        assertTrue(validate(validInput.copy(date = start)) is PlanFormResult.Valid)
        assertTrue(validate(validInput.copy(date = end)) is PlanFormResult.Valid)
    }

    /* ---------- BudgetCalculator mit Plan ---------- */

    @Test
    fun `verfügbar nach Plan`() {
        val spent = Expense(1, "", ExpenseCategory.OTHER, start, 1, 30000, listOf(Share(1, 30000)))
        val summary = BudgetCalculator.summarize(100000, listOf(spent), start, end, start, plannedOpenMinorUnits = 24000)
        assertEquals(70000L, summary.remainingMinorUnits)
        assertEquals(46000L, summary.remainingAfterPlanMinorUnits)
    }

    /* ---------- MoneyFormatter.toInputText ---------- */

    @Test
    fun `Betrag als Eingabetext und zurück`() {
        assertEquals("45,50", MoneyFormatter.toInputText(4550, "EUR", Locale.GERMANY))
        assertEquals("45.50", MoneyFormatter.toInputText(4550, "EUR", Locale.US))
        assertEquals("0,05", MoneyFormatter.toInputText(5, "EUR", Locale.GERMANY))
        assertEquals("3000", MoneyFormatter.toInputText(3000, "JPY", Locale.GERMANY))
        // Rundreise: Was wir ins Feld schreiben, muss der Parser wieder genauso lesen
        for (value in listOf(1L, 99L, 100L, 4550L, 123456789L)) {
            val text = MoneyFormatter.toInputText(value, "EUR", Locale.GERMANY)
            assertEquals(value, de.tripbudget.domain.util.MoneyParser.parseToMinorUnits(text))
        }
    }
}
