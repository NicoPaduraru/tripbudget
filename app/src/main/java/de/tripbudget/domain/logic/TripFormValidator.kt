package de.tripbudget.domain.logic

import de.tripbudget.domain.model.NewTrip
import de.tripbudget.domain.model.TravelerDraft
import de.tripbudget.domain.util.MoneyFormatter
import de.tripbudget.domain.util.MoneyParser
import java.time.LocalDate

/*
 * Prüft das Formular "Reise anlegen".
 *
 * Diese Logik liegt bewusst in der Domain-Schicht und nicht im ViewModel:
 *   - sie ist reines Kotlin → mit schnellen Unit-Tests prüfbar
 *   - das ViewModel bleibt schlank und kümmert sich nur um den UI-Zustand
 */

/** Rohe Eingaben, genau so, wie sie im Formular stehen. */
data class TripFormInput(
    val name: String = "",
    val destination: String = "",
    val startDate: LocalDate? = null, // "?" = darf null sein (noch nicht gewählt)
    val endDate: LocalDate? = null,
    val budgetText: String = "",
    val currencyCode: String = "EUR",
    val myName: String = "",
    val myTravelerId: Long? = null,                     // ERWEITERUNG 1: beim Bearbeiten gesetzt
    val otherTravelers: List<TravelerDraft> = emptyList(),
    // ERWEITERUNG 1 (nur beim Bearbeiten): Zeitraum, den Ausgaben und Plan schon belegen
    val usedFrom: LocalDate? = null,
    val usedUntil: LocalDate? = null,
)

/** Alle möglichen Fehler. Die UI übersetzt sie in Texte (Deutsch/Englisch). */
enum class TripFormError {
    NAME_EMPTY,
    DATES_MISSING,
    END_BEFORE_START,
    BUDGET_INVALID,
    MY_NAME_EMPTY,
    DUPLICATE_TRAVELER,
    TOO_MANY_TRAVELERS,
    TRAVELER_NAME_EMPTY,     // ERWEITERUNG 1: bestehende Person leer umbenannt
    DATES_EXCLUDE_ENTRIES,   // ERWEITERUNG 1: Ausgaben/Plan lägen außerhalb des Zeitraums
}

/*
 * "sealed interface": Es gibt GENAU diese zwei Möglichkeiten, keine dritte.
 * Bei "when (result)" prüft der Compiler, dass wir beide Fälle behandeln.
 * In Java gibt es das seit Java 17 als "sealed interface ... permits ...".
 */
sealed interface TripFormResult {
    data class Valid(val trip: NewTrip) : TripFormResult
    data class Invalid(val errors: Set<TripFormError>) : TripFormResult
}

object TripFormValidator {

    /** Maximale Länge für Namen (Schutz vor riesigen Eingaben). */
    const val MAX_TEXT_LENGTH = 50
    const val MAX_TRAVELERS = 20

    fun validate(input: TripFormInput): TripFormResult {
        val errors = mutableSetOf<TripFormError>()

        // 1. Name der Reise
        val name = input.name.trim()
        if (name.isEmpty()) errors += TripFormError.NAME_EMPTY

        // 2. Zeitraum
        val start = input.startDate
        val end = input.endDate
        if (start == null || end == null) {
            errors += TripFormError.DATES_MISSING
        } else if (end.isBefore(start)) {
            errors += TripFormError.END_BEFORE_START
        } else if (excludesUsedDates(start, end, input)) {
            errors += TripFormError.DATES_EXCLUDE_ENTRIES
        }

        // 3. Budget (abhängig von der Währung: EUR 2 Nachkommastellen, JPY 0)
        val fractionDigits = MoneyFormatter.fractionDigitsOf(input.currencyCode)
        val budget = MoneyParser.parseToMinorUnits(input.budgetText, fractionDigits)
        if (budget == null || budget <= 0) errors += TripFormError.BUDGET_INVALID

        // 4. Mitreisende: ich + andere, keine leeren Namen, keine doppelten
        val myName = input.myName.trim()
        if (myName.isEmpty()) errors += TripFormError.MY_NAME_EMPTY
        val trimmed = input.otherTravelers.map { it.copy(name = it.name.trim()) }
        // Eine NEUE Person ohne Namen fällt einfach weg.
        // Eine BESTEHENDE Person ohne Namen ist ein Fehler: Wegfallen hieße löschen.
        if (trimmed.any { it.id != null && it.name.isEmpty() }) errors += TripFormError.TRAVELER_NAME_EMPTY
        val others = trimmed.filter { it.name.isNotEmpty() }
        val allNames = listOf(myName) + others.map { it.name }
        if (allNames.map { it.lowercase() }.toSet().size != allNames.size) {
            errors += TripFormError.DUPLICATE_TRAVELER
        }
        if (allNames.size > MAX_TRAVELERS) errors += TripFormError.TOO_MANY_TRAVELERS

        // Gibt es Fehler, oder fehlt etwas, das wir oben schon gemeldet haben? → ungültig
        if (errors.isNotEmpty() || start == null || end == null || budget == null) {
            return TripFormResult.Invalid(errors)
        }

        // Ab hier weiß der Compiler: start, end und budget sind NICHT null ("Smart Cast").
        return TripFormResult.Valid(
            NewTrip(
                name = name,
                destination = input.destination.trim(),
                startDate = start,
                endDate = end,
                budgetMinorUnits = budget,
                currencyCode = input.currencyCode,
                me = TravelerDraft(id = input.myTravelerId, name = myName),
                others = others,
            )
        )
    }

    /** true, wenn eine Ausgabe oder Aktivität vor dem Beginn oder nach dem Ende läge. */
    private fun excludesUsedDates(start: LocalDate, end: LocalDate, input: TripFormInput): Boolean {
        val tooLate = input.usedFrom != null && input.usedFrom.isBefore(start)
        val tooEarly = input.usedUntil != null && input.usedUntil.isAfter(end)
        return tooLate || tooEarly
    }
}
