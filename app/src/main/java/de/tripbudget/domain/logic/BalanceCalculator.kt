package de.tripbudget.domain.logic

import de.tripbudget.domain.model.Expense
import de.tripbudget.domain.model.Transfer
import de.tripbudget.domain.model.Traveler

/*
 * SCHRITT 2: Saldo jeder Person berechnen.
 *
 * Saldo = was ich bezahlt habe − was ich verbraucht habe (meine Anteile)
 *       + Ausgleichszahlungen, die ich geleistet habe
 *       − Ausgleichszahlungen, die ich bekommen habe
 *
 *   Saldo > 0  → die Person bekommt noch Geld
 *   Saldo < 0  → die Person schuldet noch Geld
 *
 * Wichtige Eigenschaft: Die Summe ALLER Salden ist immer 0.
 * Geld verschwindet nicht, es wechselt nur die Person. Das testen wir.
 */
object BalanceCalculator {

    /** Liefert für jede Person ihren Saldo in Cent: Map<travelerId, saldo>. */
    fun calculate(
        travelers: List<Traveler>,
        expenses: List<Expense>,
        settledTransfers: List<Transfer> = emptyList(), // Standardwert: Parameter darf fehlen
    ): Map<Long, Long> {
        // mutableMapOf = veränderbare Map (wie HashMap in Java).
        // Jede Person startet mit Saldo 0.
        val balances = mutableMapOf<Long, Long>()
        travelers.forEach { balances[it.id] = 0L } // "it" = das aktuelle Element

        val knownIds = balances.keys

        for (expense in expenses) {
            require(expense.paidByTravelerId in knownIds) { "Unbekannter Zahler in '${expense.description}'" }
            // Der Zahler hat Geld vorgestreckt → sein Saldo steigt.
            balances.addTo(expense.paidByTravelerId, expense.amountMinorUnits)
            // Jeder Anteil ist Verbrauch → Saldo sinkt.
            for (share in expense.shares) {
                require(share.travelerId in knownIds) { "Unbekannte Person in '${expense.description}'" }
                balances.addTo(share.travelerId, -share.amountMinorUnits)
            }
        }

        for (transfer in settledTransfers) {
            // Wer zurückzahlt, verringert seine Schulden → Saldo steigt.
            balances.addTo(transfer.fromTravelerId, transfer.amountMinorUnits)
            // Wer Geld bekommt, hat weniger Guthaben → Saldo sinkt.
            balances.addTo(transfer.toTravelerId, -transfer.amountMinorUnits)
        }

        return balances.toMap() // unveränderliche Kopie nach außen geben
    }

    /*
     * Eine "Extension Function": Wir fügen der Klasse MutableMap eine eigene
     * Methode hinzu, ohne sie zu verändern. So etwas gibt es in Java nicht.
     * "private" = nur in dieser Datei sichtbar.
     */
    private fun MutableMap<Long, Long>.addTo(key: Long, delta: Long) {
        this[key] = (this[key] ?: 0L) + delta // "?:" (Elvis): falls null, nimm 0
    }
}
