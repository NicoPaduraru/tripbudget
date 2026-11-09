package de.tripbudget.domain.logic

import de.tripbudget.domain.model.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ExpenseFormValidatorTest {

    private val travelers = listOf(1L, 2L, 3L) // 1 = ich

    private val valid = ExpenseFormInput(
        amountText = "10",
        category = ExpenseCategory.FOOD,
        description = "  Abendessen  ",
        date = LocalDate.of(2027, 5, 13),
        paidByTravelerId = 2L,
        sharedWithTravelerIds = setOf(1L, 2L, 3L),
    )

    private fun validate(input: ExpenseFormInput, currency: String = "EUR") =
        ExpenseFormValidator.validate(input, currency, travelers)

    private fun errorsOf(input: ExpenseFormInput): Set<ExpenseFormError> =
        when (val result = validate(input)) {
            is ExpenseFormResult.Valid -> emptySet()
            is ExpenseFormResult.Invalid -> result.errors
        }

    @Test
    fun `gültige Ausgabe wird gleichmäßig aufgeteilt`() {
        val expense = (validate(valid) as ExpenseFormResult.Valid).expense
        assertEquals(1000L, expense.amountMinorUnits)
        assertEquals("Abendessen", expense.description)
        assertEquals(2L, expense.paidByTravelerId)
        // 10,00 € / 3 = 3,34 + 3,33 + 3,33 – Restcent an die erste Person der Reise
        assertEquals(listOf(1L to 334L, 2L to 333L, 3L to 333L), expense.shares.map { it.travelerId to it.amountMinorUnits })
    }

    @Test
    fun `Aufteilung folgt der Reihenfolge der Reise, nicht der Auswahl`() {
        val expense = (validate(valid.copy(sharedWithTravelerIds = setOf(3L, 1L))) as ExpenseFormResult.Valid).expense
        assertEquals(listOf(1L, 3L), expense.shares.map { it.travelerId })
    }

    @Test
    fun `Beschreibung ist optional`() {
        val expense = (validate(valid.copy(description = "")) as ExpenseFormResult.Valid).expense
        assertEquals("", expense.description)
    }

    @Test
    fun `Betrag ungültig`() {
        assertEquals(setOf(ExpenseFormError.AMOUNT_INVALID), errorsOf(valid.copy(amountText = "")))
        assertEquals(setOf(ExpenseFormError.AMOUNT_INVALID), errorsOf(valid.copy(amountText = "0,00")))
    }

    @Test
    fun `Betrag in Yen`() {
        val expense = (validate(valid.copy(amountText = "3000"), currency = "JPY") as ExpenseFormResult.Valid).expense
        assertEquals(3000L, expense.amountMinorUnits)
    }

    @Test
    fun `Kategorie und Datum fehlen`() {
        assertEquals(
            setOf(ExpenseFormError.CATEGORY_MISSING, ExpenseFormError.DATE_MISSING),
            errorsOf(valid.copy(category = null, date = null)),
        )
    }

    @Test
    fun `Zahler muss zur Reise gehören`() {
        assertEquals(setOf(ExpenseFormError.PAYER_MISSING), errorsOf(valid.copy(paidByTravelerId = null)))
        assertEquals(setOf(ExpenseFormError.PAYER_MISSING), errorsOf(valid.copy(paidByTravelerId = 99L)))
    }

    @Test
    fun `mindestens eine Person muss beteiligt sein`() {
        assertEquals(setOf(ExpenseFormError.NO_PARTICIPANTS), errorsOf(valid.copy(sharedWithTravelerIds = emptySet())))
        // Fremde IDs zählen nicht
        assertEquals(setOf(ExpenseFormError.NO_PARTICIPANTS), errorsOf(valid.copy(sharedWithTravelerIds = setOf(99L))))
    }

    @Test
    fun `Zahler muss nicht selbst beteiligt sein`() {
        // Beispiel: Ben bezahlt das Taxi nur für Anna und Clara
        val expense = (validate(valid.copy(sharedWithTravelerIds = setOf(1L, 3L))) as ExpenseFormResult.Valid).expense
        assertEquals(listOf(1L to 500L, 3L to 500L), expense.shares.map { it.travelerId to it.amountMinorUnits })
    }
}
