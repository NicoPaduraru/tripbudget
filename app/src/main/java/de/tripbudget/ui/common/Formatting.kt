package de.tripbudget.ui.common

import androidx.annotation.StringRes
import de.tripbudget.R
import de.tripbudget.domain.logic.ExpenseFormError
import de.tripbudget.domain.logic.PlanFormError
import de.tripbudget.domain.logic.TripFormError
import de.tripbudget.domain.model.ExpenseCategory
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/*
 * Hilfsfunktionen für die Anzeige, abhängig von der App-Sprache (Locale).
 */

fun formatDateRange(start: LocalDate, end: LocalDate, locale: Locale): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    return if (start == end) {
        start.format(formatter)
    } else {
        "${start.format(formatter)} – ${end.format(formatter)}"
    }
}

/** "12.05.2027" (Deutsch) / "May 12, 2027" (Englisch) */
fun formatDate(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

/** NEU IN PHASE 4: "14:30" (Deutsch) / "2:30 PM" (Englisch) */
fun formatTime(time: LocalTime, locale: Locale): String =
    time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))

/** Überschrift für einen Tag in der Ausgabenliste, z. B. "Mittwoch, 12. Mai 2027". */
fun formatDayHeader(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))

/*
 * Domain-Werte → Texte aus strings.xml.
 * "when" muss ALLE Enum-Werte abdecken; fehlt einer, meldet der Compiler einen Fehler.
 * Zwei Funktionen dürfen gleich heißen (messageRes), wenn ihr Empfänger-Typ verschieden ist.
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
    TripFormError.TRAVELER_NAME_EMPTY -> R.string.error_traveler_name_empty
    TripFormError.DATES_EXCLUDE_ENTRIES -> R.string.error_dates_exclude_entries
}

@StringRes
fun ExpenseFormError.messageRes(): Int = when (this) {
    ExpenseFormError.AMOUNT_INVALID -> R.string.error_amount_invalid
    ExpenseFormError.CATEGORY_MISSING -> R.string.error_category_missing
    ExpenseFormError.DATE_MISSING -> R.string.error_date_missing
    ExpenseFormError.PAYER_MISSING -> R.string.error_payer_missing
    ExpenseFormError.NO_PARTICIPANTS -> R.string.error_no_participants
}

@StringRes
fun ExpenseCategory.labelRes(): Int = when (this) {
    ExpenseCategory.ACCOMMODATION -> R.string.category_accommodation
    ExpenseCategory.TRANSPORT -> R.string.category_transport
    ExpenseCategory.FOOD -> R.string.category_food
    ExpenseCategory.ACTIVITIES -> R.string.category_activities
    ExpenseCategory.SHOPPING -> R.string.category_shopping
    ExpenseCategory.OTHER -> R.string.category_other
}

@StringRes
fun PlanFormError.messageRes(): Int = when (this) {
    PlanFormError.TITLE_EMPTY -> R.string.error_plan_title_empty
    PlanFormError.DATE_MISSING -> R.string.error_date_missing
    PlanFormError.DATE_OUTSIDE_TRIP -> R.string.error_plan_date_outside_trip
    PlanFormError.COST_INVALID -> R.string.error_plan_cost_invalid
}
