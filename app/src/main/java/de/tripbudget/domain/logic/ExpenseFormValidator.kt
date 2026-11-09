package de.tripbudget.domain.logic

import de.tripbudget.domain.model.ExpenseCategory
import de.tripbudget.domain.model.NewExpense
import de.tripbudget.domain.util.MoneyFormatter
import de.tripbudget.domain.util.MoneyParser
import java.time.LocalDate

/*
 * Prüft das Formular "Ausgabe erfassen" – nach demselben Muster wie
 * TripFormValidator aus Phase 1. Wiederkehrende Muster machen Code
 * leichter verständlich: Wer eines kennt, versteht das andere sofort.
 */

data class ExpenseFormInput(
    val amountText: String = "",
    val category: ExpenseCategory? = null,
    val description: String = "",
    val date: LocalDate? = null,
    val paidByTravelerId: Long? = null,
    val sharedWithTravelerIds: Set<Long> = emptySet(),
)

enum class ExpenseFormError {
    AMOUNT_INVALID,
    CATEGORY_MISSING,
    DATE_MISSING,
    PAYER_MISSING,
    NO_PARTICIPANTS,
}

sealed interface ExpenseFormResult {
    data class Valid(val expense: NewExpense) : ExpenseFormResult
    data class Invalid(val errors: Set<ExpenseFormError>) : ExpenseFormResult
}

object ExpenseFormValidator {

    const val MAX_DESCRIPTION_LENGTH = 50

    /**
     * @param travelerIds alle Mitreisenden der Reise, in fester Reihenfolge ("ich" zuerst).
     *                    Die Reihenfolge bestimmt, wer beim Aufteilen den Restcent bekommt.
     */
    fun validate(
        input: ExpenseFormInput,
        currencyCode: String,
        travelerIds: List<Long>,
    ): ExpenseFormResult {
        val errors = mutableSetOf<ExpenseFormError>()

        val fractionDigits = MoneyFormatter.fractionDigitsOf(currencyCode)
        val amount = MoneyParser.parseToMinorUnits(input.amountText, fractionDigits)
        if (amount == null || amount <= 0) errors += ExpenseFormError.AMOUNT_INVALID

        val category = input.category
        if (category == null) errors += ExpenseFormError.CATEGORY_MISSING

        val date = input.date
        if (date == null) errors += ExpenseFormError.DATE_MISSING

        // Der Zahler muss zur Reise gehören (Schutz vor veralteten oder falschen IDs).
        val payer = input.paidByTravelerId
        if (payer == null || payer !in travelerIds) errors += ExpenseFormError.PAYER_MISSING

        // Nur Personen dieser Reise, in der Reihenfolge der Reise.
        val participants = travelerIds.filter { it in input.sharedWithTravelerIds }
        if (participants.isEmpty()) errors += ExpenseFormError.NO_PARTICIPANTS

        if (errors.isNotEmpty() || amount == null || category == null || date == null || payer == null) {
            return ExpenseFormResult.Invalid(errors)
        }

        return ExpenseFormResult.Valid(
            NewExpense(
                description = input.description.trim().take(MAX_DESCRIPTION_LENGTH),
                category = category,
                date = date,
                paidByTravelerId = payer,
                amountMinorUnits = amount,
                // Hier kommt der CostSplitter aus Phase 0 zum Einsatz!
                shares = CostSplitter.splitEvenly(amount, participants),
            )
        )
    }
}
