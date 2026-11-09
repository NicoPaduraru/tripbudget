package de.tripbudget.domain.model

/*
 * NEU IN PHASE 3: eine ERLEDIGTE Ausgleichszahlung.
 *
 * Unterschied zu Transfer (Phase 0):
 *   Transfer   = eine Überweisung, wie sie der SettlementSuggester VORSCHLÄGT
 *   Settlement = eine Überweisung, die wirklich STATTGEFUNDEN hat (mit ID und Zeitpunkt)
 */
data class Settlement(
    val id: Long,
    val fromTravelerId: Long,
    val toTravelerId: Long,
    val amountMinorUnits: Long,
    val paidAtMillis: Long,
) {
    /** Für den BalanceCalculator ist eine erledigte Zahlung einfach ein Transfer. */
    fun toTransfer() = Transfer(
        fromTravelerId = fromTravelerId,
        toTravelerId = toTravelerId,
        amountMinorUnits = amountMinorUnits,
    )
}
