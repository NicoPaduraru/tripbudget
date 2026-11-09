package de.tripbudget.domain.logic

import de.tripbudget.domain.model.Expense
import de.tripbudget.domain.model.Settlement
import de.tripbudget.domain.model.Transfer
import de.tripbudget.domain.model.Traveler

/*
 * NEU IN PHASE 3: fasst die Bausteine aus Phase 0 zur kompletten Abrechnung zusammen.
 *
 *   BalanceCalculator   → Saldo jeder Person (inkl. erledigter Zahlungen)
 *   SettlementSuggester → wer zahlt wem wie viel, damit alle bei 0 sind
 *
 * Zusätzlich berechnen wir pro Person, wie viel sie bezahlt und wie viel sie
 * verbraucht hat. So kann die App erklären, WIE ein Saldo zustande kommt –
 * Menschen vertrauen einer Abrechnung mehr, wenn sie sie nachvollziehen können.
 */

/** Zahlen zu einer Person. */
data class PersonBalance(
    val traveler: Traveler,
    /** Summe aller Ausgaben, die diese Person bezahlt hat. */
    val paidMinorUnits: Long,
    /** Summe ihrer Anteile = was sie "verbraucht" hat. */
    val shareMinorUnits: Long,
    /** > 0 bekommt Geld, < 0 schuldet Geld – nach Abzug erledigter Zahlungen. */
    val balanceMinorUnits: Long,
)

data class SettlementSummary(
    val balances: List<PersonBalance>, // in der Reihenfolge der Reise ("ich" zuerst)
    val suggestions: List<Transfer>,
) {
    /** true, wenn niemand mehr jemandem etwas schuldet. */
    val isSettled: Boolean get() = suggestions.isEmpty()
}

object GroupSettlementCalculator {

    fun compute(
        travelers: List<Traveler>,
        expenses: List<Expense>,
        settlements: List<Settlement>,
    ): SettlementSummary {
        val balances = BalanceCalculator.calculate(
            travelers = travelers,
            expenses = expenses,
            settledTransfers = settlements.map { it.toTransfer() },
        )

        // groupBy + sumOf: pro Person aufsummieren
        val paidById: Map<Long, Long> = expenses
            .groupBy { it.paidByTravelerId }
            .mapValues { (_, list) -> list.sumOf { it.amountMinorUnits } }
        val shareById: Map<Long, Long> = expenses
            .flatMap { it.shares } // alle Anteile aller Ausgaben in EINER Liste
            .groupBy { it.travelerId }
            .mapValues { (_, list) -> list.sumOf { it.amountMinorUnits } }

        return SettlementSummary(
            balances = travelers.map { traveler ->
                PersonBalance(
                    traveler = traveler,
                    paidMinorUnits = paidById[traveler.id] ?: 0L,
                    shareMinorUnits = shareById[traveler.id] ?: 0L,
                    balanceMinorUnits = balances[traveler.id] ?: 0L,
                )
            },
            suggestions = SettlementSuggester.suggest(balances),
        )
    }
}
