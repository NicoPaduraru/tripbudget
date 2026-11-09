package de.tripbudget.domain.model

import java.time.LocalDate

/*
 * Die Datenmodelle der Abrechnung (seit Phase 0).
 *
 * NEU IN PHASE 2:
 *   - Traveler weiß, ob er "ich" ist (isMe)
 *   - Expense hat Kategorie und Datum
 *   - "amountCents" heißt jetzt überall "amountMinorUnits" (kleinste Währungseinheit,
 *     bei EUR Cent, bei JPY Yen) – passend zum MoneyParser aus Phase 1.
 *
 * KOTLIN VS. JAVA:
 * Eine "data class" ersetzt in Kotlin eine ganze Java-Klasse mit
 * Konstruktor, Gettern, equals(), hashCode() und toString().
 * "val" = unveränderlich (wie "final" in Java).
 */

/** Eine mitreisende Person. Wir kennen nur einen Namen, kein Konto. */
data class Traveler(
    val id: Long,
    val name: String,
    val isMe: Boolean = false, // Standardwert: alte Aufrufe ohne isMe funktionieren weiter
)

/** Der Anteil einer Person an einer Ausgabe. */
data class Share(
    val travelerId: Long,
    val amountMinorUnits: Long,
)

/**
 * Eine Ausgabe: Wer hat wie viel bezahlt, wofür, wann, und wie ist sie aufgeteilt?
 *
 * Der "init"-Block läuft bei jeder Erzeugung. Mit require() prüfen wir Regeln
 * (Invarianten). So kann es nie eine "kaputte" Ausgabe im Programm geben.
 */
data class Expense(
    val id: Long,
    val description: String,
    val category: ExpenseCategory,
    val date: LocalDate,
    val paidByTravelerId: Long,
    val amountMinorUnits: Long,
    val shares: List<Share>,
) {
    init {
        require(amountMinorUnits > 0) { "Betrag muss größer als 0 sein, war $amountMinorUnits" }
        require(shares.isNotEmpty()) { "Eine Ausgabe braucht mindestens einen Anteil" }
        require(shares.sumOf { it.amountMinorUnits } == amountMinorUnits) {
            "Summe der Anteile (${shares.sumOf { it.amountMinorUnits }}) passt nicht zum Betrag ($amountMinorUnits)"
        }
        require(shares.map { it.travelerId }.toSet().size == shares.size) {
            "Jede Person darf nur einmal in den Anteilen vorkommen"
        }
    }
}

/**
 * Eine Ausgleichszahlung zwischen zwei Personen:
 * "fromTravelerId zahlt toTravelerId den Betrag amountMinorUnits".
 */
data class Transfer(
    val fromTravelerId: Long,
    val toTravelerId: Long,
    val amountMinorUnits: Long,
) {
    init {
        require(amountMinorUnits > 0) { "Überweisungsbetrag muss größer als 0 sein" }
        require(fromTravelerId != toTravelerId) { "Niemand kann sich selbst bezahlen" }
    }
}
