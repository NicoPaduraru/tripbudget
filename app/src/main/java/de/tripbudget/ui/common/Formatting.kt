package de.tripbudget.ui.common

import androidx.annotation.StringRes
import de.tripbudget.R
import de.tripbudget.domain.logic.TripFormError
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/*
 * Hilfsfunktionen für die Anzeige. Sie kennen die App-Sprache (Locale),
 * deshalb wirkt dasselbe Datum je nach Sprache anders:
 *   Deutsch:  12.05.2027 – 19.05.2027
 *   Englisch: May 12, 2027 – May 19, 2027
 */

fun formatDateRange(start: LocalDate, end: LocalDate, locale: Locale): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    return if (start == end) {
        start.format(formatter)
    } else {
        "${start.format(formatter)} – ${end.format(formatter)}"
    }
}

/*
 * Übersetzt einen Domain-Fehler in einen Text aus strings.xml.
 * R.string.xxx ist eine Zahl (ID); der eigentliche Text hängt von der Sprache ab.
 * "when" muss ALLE Enum-Werte abdecken – vergessen wir einen, meldet der Compiler einen Fehler.
 */
@StringRes
fun TripFormError.messageRes(): Int = when (this) {
    TripFormError.NAME_EMPTY -> R.string.error_name_empty
    TripFormError.DATES_MISSING -> R.string.error_dates_missing
    TripFormError.END_BEFORE_START -> R.string.error_end_before_start
    TripFormError.BUDGET_INVALID -> R.string.error_budget_invalid
    TripFormError.MY_NAME_EMPTY -> R.string.error_my_name_empty
    TripFormError.DUPLICATE_TRAVELER -> R.string.error_duplicate_traveler
    TripFormError.TOO_MANY_TRAVELERS -> R.string.error_too_many_travelers
}
