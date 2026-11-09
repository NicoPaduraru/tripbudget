package de.tripbudget.domain.logic

import de.tripbudget.domain.model.NewPlanItem
import de.tripbudget.domain.util.MoneyFormatter
import de.tripbudget.domain.util.MoneyParser
import java.time.LocalDate
import java.time.LocalTime

/*
 * NEU IN PHASE 4: prüft das Formular "Aktivität planen".
 * Drittes Formular, drittes Mal dasselbe Muster (Input → Validator → Valid/Invalid).
 * Neu: Felder, die leer bleiben DÜRFEN, aber wenn ausgefüllt, gültig sein MÜSSEN (Kosten).
 */

data class PlanFormInput(
    val title: String = "",
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val location: String = "",
    val costText: String = "",
    val note: String = "",
)

enum class PlanFormError {
    TITLE_EMPTY,
    DATE_MISSING,
    DATE_OUTSIDE_TRIP,
    COST_INVALID,
}

sealed interface PlanFormResult {
    data class Valid(val item: NewPlanItem) : PlanFormResult
    data class Invalid(val errors: Set<PlanFormError>) : PlanFormResult
}

object PlanFormValidator {

    const val MAX_TEXT_LENGTH = 50
    const val MAX_NOTE_LENGTH = 200

    fun validate(
        input: PlanFormInput,
        currencyCode: String,
        tripStart: LocalDate,
        tripEnd: LocalDate,
    ): PlanFormResult {
        val errors = mutableSetOf<PlanFormError>()

        val title = input.title.trim()
        if (title.isEmpty()) errors += PlanFormError.TITLE_EMPTY

        // Ein Plan gehört zu einem Reisetag – Daten außerhalb der Reise ergeben keinen Sinn
        val date = input.date
        if (date == null) {
            errors += PlanFormError.DATE_MISSING
        } else if (date.isBefore(tripStart) || date.isAfter(tripEnd)) {
            errors += PlanFormError.DATE_OUTSIDE_TRIP
        }

        // Kosten sind optional: leer = null. Ausgefüllt muss es eine gültige Zahl ≥ 0 sein.
        val costText = input.costText.trim()
        val cost: Long? = if (costText.isEmpty()) {
            null
        } else {
            val parsed = MoneyParser.parseToMinorUnits(costText, MoneyFormatter.fractionDigitsOf(currencyCode))
            if (parsed == null) errors += PlanFormError.COST_INVALID
            parsed
        }

        if (errors.isNotEmpty() || date == null) return PlanFormResult.Invalid(errors)

        return PlanFormResult.Valid(
            NewPlanItem(
                date = date,
                time = input.time,
                title = title.take(MAX_TEXT_LENGTH),
                location = input.location.trim().take(MAX_TEXT_LENGTH),
                plannedCostMinorUnits = cost?.takeIf { it > 0 }, // 0 € speichern wir als "keine Kosten"
                note = input.note.trim().take(MAX_NOTE_LENGTH),
            )
        )
    }
}
