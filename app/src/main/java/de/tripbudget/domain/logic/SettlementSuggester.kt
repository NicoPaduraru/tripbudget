package de.tripbudget.domain.logic

import de.tripbudget.domain.model.Transfer

/*
 * SCHRITT 3: Wer zahlt wem wie viel?
 *
 * Idee (ein "Greedy"-Algorithmus = nimm immer die gerade beste Wahl):
 *   1. Teile alle Personen in Gläubiger (Saldo > 0) und Schuldner (Saldo < 0).
 *   2. Nimm den größten Schuldner und den größten Gläubiger.
 *   3. Der Schuldner zahlt dem Gläubiger so viel wie möglich:
 *      min(Schulden, Guthaben).
 *   4. Wer danach bei 0 ist, ist fertig. Wiederhole, bis alle bei 0 sind.
 *
 * Ergebnis: höchstens (Anzahl Personen − 1) Überweisungen.
 * Das ist nicht in jedem Sonderfall das mathematische Minimum, aber für
 * Reisegruppen sehr gut und leicht verständlich.
 */
object SettlementSuggester {

    fun suggest(balances: Map<Long, Long>): List<Transfer> {
        require(balances.values.sum() == 0L) { "Salden müssen zusammen 0 ergeben" }

        // Wir arbeiten mit veränderbaren Kopien (Arrays aus [id, betrag]).
        // Gläubiger: größtes Guthaben zuerst. Schuldner: größte Schuld zuerst.
        val creditors = balances.filter { it.value > 0 }
            .map { longArrayOf(it.key, it.value) }
            .sortedByDescending { it[1] }
        val debtors = balances.filter { it.value < 0 }
            .map { longArrayOf(it.key, -it.value) } // Schuld als positive Zahl
            .sortedByDescending { it[1] }

        val transfers = mutableListOf<Transfer>()
        var c = 0 // Index im Gläubiger-Array
        var d = 0 // Index im Schuldner-Array

        while (c < creditors.size && d < debtors.size) {
            val creditor = creditors[c]
            val debtor = debtors[d]
            val amount = minOf(creditor[1], debtor[1])

            transfers += Transfer( // "+=" fügt der Liste ein Element hinzu
                fromTravelerId = debtor[0],
                toTravelerId = creditor[0],
                amountMinorUnits = amount,
            )

            creditor[1] -= amount
            debtor[1] -= amount
            if (creditor[1] == 0L) c++
            if (debtor[1] == 0L) d++
        }
        return transfers
    }
}
