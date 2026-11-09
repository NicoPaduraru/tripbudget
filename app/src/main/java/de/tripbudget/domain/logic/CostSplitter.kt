package de.tripbudget.domain.logic

import de.tripbudget.domain.model.Share

/*
 * SCHRITT 1: Kosten gleichmäßig aufteilen.
 *
 * Problem: 10,00 € durch 3 Personen = 3,333... €. Cent kann man nicht teilen.
 * Lösung: Jeder bekommt 3,33 €, der Rest (1 Cent) wird verteilt:
 *         3,34 + 3,33 + 3,33 = 10,00 €. Die Summe stimmt immer exakt.
 *
 * KOTLIN VS. JAVA:
 * "object" ist ein Singleton: es gibt genau eine Instanz, man ruft
 * CostSplitter.splitEvenly(...) direkt auf, wie eine static-Methode in Java.
 */
object CostSplitter {

    /**
     * Teilt [amountMinorUnits] gleichmäßig auf die Personen in [travelerIds] auf.
     * Die ersten Personen in der Liste bekommen den Restcent.
     */
    fun splitEvenly(amountMinorUnits: Long, travelerIds: List<Long>): List<Share> {
        require(amountMinorUnits > 0) { "Betrag muss größer als 0 sein" }
        require(travelerIds.isNotEmpty()) { "Mindestens eine Person nötig" }
        require(travelerIds.toSet().size == travelerIds.size) { "Personen doppelt in der Liste" }

        val count = travelerIds.size
        val base = amountMinorUnits / count      // ganzzahlige Division: 1000 / 3 = 333
        val remainder = amountMinorUnits % count // Rest (Modulo):          1000 % 3 = 1

        // mapIndexed geht durch die Liste und liefert Index + Element.
        // In Java wäre das eine for-Schleife mit einer neuen ArrayList.
        return travelerIds.mapIndexed { index, travelerId ->
            val extraCent = if (index < remainder) 1 else 0 // "if" ist in Kotlin ein Ausdruck
            Share(travelerId = travelerId, amountMinorUnits = base + extraCent)
        }
    }
}
