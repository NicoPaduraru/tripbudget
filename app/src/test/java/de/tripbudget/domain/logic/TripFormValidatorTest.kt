package de.tripbudget.domain.logic

import de.tripbudget.domain.model.TravelerDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TripFormValidatorTest {

    /** Neue Personen (ohne ID) aus Namen bauen. */
    private fun drafts(vararg names: String) = names.map { TravelerDraft(name = it) }

    /** Ein gültiges Formular als Ausgangspunkt. Jeder Test ändert nur EINE Sache (mit copy). */
    private val valid = TripFormInput(
        name = "Lissabon",
        destination = "Portugal",
        startDate = LocalDate.of(2027, 5, 12),
        endDate = LocalDate.of(2027, 5, 19),
        budgetText = "1500",
        currencyCode = "EUR",
        myName = "Nico",
        otherTravelers = drafts("Anna", "Ben"),
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
        assertEquals(TravelerDraft(id = null, name = "Nico"), trip.me)
        assertEquals(listOf("Anna", "Ben"), trip.others.map { it.name })
    }

    @Test
    fun `Leerzeichen werden entfernt`() {
        val result = TripFormValidator.validate(valid.copy(name = "  Lissabon  ", otherTravelers = drafts(" Anna ", "  ")))
        val trip = (result as TripFormResult.Valid).trip
        assertEquals("Lissabon", trip.name)
        assertEquals(listOf("Anna"), trip.others.map { it.name }) // leere neue Namen fallen weg
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
        assertEquals(setOf(TripFormError.DUPLICATE_TRAVELER), errorsOf(valid.copy(otherTravelers = drafts("nico"))))
    }

    @Test
    fun `zu viele Mitreisende`() {
        val many = (1..TripFormValidator.MAX_TRAVELERS).map { TravelerDraft(name = "Person $it") } // mit "Nico" = 21 Personen
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

    /* ---------------- ERWEITERUNG 1: Reise bearbeiten ---------------- */

    /** Eine bestehende Reise: IDs gesetzt, Ausgaben/Plan vom 13. bis 18. Mai. */
    private val editing = valid.copy(
        myTravelerId = 1,
        otherTravelers = listOf(TravelerDraft(2, "Anna"), TravelerDraft(3, "Ben")),
        usedFrom = LocalDate.of(2027, 5, 13),
        usedUntil = LocalDate.of(2027, 5, 18),
    )

    @Test
    fun `beim Bearbeiten bleiben die IDs erhalten`() {
        val input = editing.copy(otherTravelers = editing.otherTravelers + TravelerDraft(name = "Carla"))
        val trip = (TripFormValidator.validate(input) as TripFormResult.Valid).trip
        assertEquals(TravelerDraft(1, "Nico"), trip.me)
        assertEquals(listOf(2L, 3L, null), trip.others.map { it.id })
    }

    @Test
    fun `Zeitraum darf genau bis an die vorhandenen Einträge schrumpfen`() {
        val input = editing.copy(startDate = LocalDate.of(2027, 5, 13), endDate = LocalDate.of(2027, 5, 18))
        assertEquals(emptySet<TripFormError>(), errorsOf(input))
    }

    @Test
    fun `Zeitraum darf keine Ausgaben oder Aktivitäten ausschließen`() {
        val startTooLate = editing.copy(startDate = LocalDate.of(2027, 5, 14))
        val endTooEarly = editing.copy(endDate = LocalDate.of(2027, 5, 17))
        assertEquals(setOf(TripFormError.DATES_EXCLUDE_ENTRIES), errorsOf(startTooLate))
        assertEquals(setOf(TripFormError.DATES_EXCLUDE_ENTRIES), errorsOf(endTooEarly))
    }

    @Test
    fun `bestehende Person darf keinen leeren Namen bekommen`() {
        val input = editing.copy(otherTravelers = listOf(TravelerDraft(2, "  "), TravelerDraft(3, "Ben")))
        assertEquals(setOf(TripFormError.TRAVELER_NAME_EMPTY), errorsOf(input))
    }
}
