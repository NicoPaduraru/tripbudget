package de.tripbudget.domain.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TripFormValidatorTest {

    /** Ein gültiges Formular als Ausgangspunkt. Jeder Test ändert nur EINE Sache (mit copy). */
    private val valid = TripFormInput(
        name = "Lissabon",
        destination = "Portugal",
        startDate = LocalDate.of(2027, 5, 12),
        endDate = LocalDate.of(2027, 5, 19),
        budgetText = "1500",
        currencyCode = "EUR",
        myName = "Nico",
        otherTravelers = listOf("Anna", "Ben"),
    )

    /** Hilfsfunktion: liefert die Fehler oder eine leere Menge bei gültiger Eingabe. */
    private fun errorsOf(input: TripFormInput): Set<TripFormError> =
        when (val result = TripFormValidator.validate(input)) {
            is TripFormResult.Valid -> emptySet()
            is TripFormResult.Invalid -> result.errors
        }

    @Test
    fun `gültiges Formular ergibt eine neue Reise`() {
        val result = TripFormValidator.validate(valid)
        assertTrue(result is TripFormResult.Valid)
        val trip = (result as TripFormResult.Valid).trip
        assertEquals("Lissabon", trip.name)
        assertEquals(150000L, trip.budgetMinorUnits)
        assertEquals("Nico", trip.myName)
        assertEquals(listOf("Anna", "Ben"), trip.otherTravelerNames)
    }

    @Test
    fun `Leerzeichen werden entfernt`() {
        val result = TripFormValidator.validate(valid.copy(name = "  Lissabon  ", otherTravelers = listOf(" Anna ", "  ")))
        val trip = (result as TripFormResult.Valid).trip
        assertEquals("Lissabon", trip.name)
        assertEquals(listOf("Anna"), trip.otherTravelerNames) // leere Namen fallen weg
    }

    @Test
    fun `Name fehlt`() {
        assertEquals(setOf(TripFormError.NAME_EMPTY), errorsOf(valid.copy(name = "   ")))
    }

    @Test
    fun `Zeitraum fehlt`() {
        assertEquals(setOf(TripFormError.DATES_MISSING), errorsOf(valid.copy(endDate = null)))
    }

    @Test
    fun `Ende vor Beginn`() {
        val input = valid.copy(startDate = LocalDate.of(2027, 5, 19), endDate = LocalDate.of(2027, 5, 12))
        assertEquals(setOf(TripFormError.END_BEFORE_START), errorsOf(input))
    }

    @Test
    fun `eintägige Reise ist erlaubt`() {
        val day = LocalDate.of(2027, 5, 12)
        assertEquals(emptySet<TripFormError>(), errorsOf(valid.copy(startDate = day, endDate = day)))
    }

    @Test
    fun `Budget ungültig oder 0`() {
        assertEquals(setOf(TripFormError.BUDGET_INVALID), errorsOf(valid.copy(budgetText = "abc")))
        assertEquals(setOf(TripFormError.BUDGET_INVALID), errorsOf(valid.copy(budgetText = "0")))
    }

    @Test
    fun `Budget in Yen ohne Nachkommastellen`() {
        val result = TripFormValidator.validate(valid.copy(budgetText = "200000", currencyCode = "JPY"))
        assertEquals(200000L, (result as TripFormResult.Valid).trip.budgetMinorUnits)
        assertEquals(setOf(TripFormError.BUDGET_INVALID), errorsOf(valid.copy(budgetText = "1,5", currencyCode = "JPY")))
    }

    @Test
    fun `eigener Name fehlt`() {
        assertEquals(setOf(TripFormError.MY_NAME_EMPTY), errorsOf(valid.copy(myName = "")))
    }

    @Test
    fun `doppelte Namen ohne Rücksicht auf Groß- und Kleinschreibung`() {
        assertEquals(setOf(TripFormError.DUPLICATE_TRAVELER), errorsOf(valid.copy(otherTravelers = listOf("nico"))))
    }

    @Test
    fun `zu viele Mitreisende`() {
        val many = (1..TripFormValidator.MAX_TRAVELERS).map { "Person $it" } // mit "Nico" = 21 Personen
        assertEquals(setOf(TripFormError.TOO_MANY_TRAVELERS), errorsOf(valid.copy(otherTravelers = many)))
    }

    @Test
    fun `mehrere Fehler gleichzeitig`() {
        val input = TripFormInput() // komplett leeres Formular
        assertEquals(
            setOf(
                TripFormError.NAME_EMPTY,
                TripFormError.DATES_MISSING,
                TripFormError.BUDGET_INVALID,
                TripFormError.MY_NAME_EMPTY,
            ),
            errorsOf(input),
        )
    }
}
