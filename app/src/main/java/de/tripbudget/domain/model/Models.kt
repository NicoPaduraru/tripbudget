package de.tripbudget.domain.model

/*
 * Die Datenmodelle der Abrechnung.
 *
 * KOTLIN VS. JAVA:
 * Eine "data class" ersetzt in Kotlin eine ganze Java-Klasse mit
 * Konstruktor, Gettern, equals(), hashCode() und toString().
 * Die Klasse Traveler unten wären in Java ca. 40 Zeilen.
 *
 * "val" = unveränderlich (wie "final" in Java). Wir nutzen fast nur val,
 * weil unveränderliche Objekte weniger Fehler verursachen.
 *
 * Geld speichern wir IMMER als Long in Cent (1234 = 12,34 €), nie als Double,
 * denn 0.1 + 0.2 ergibt mit Double 0.30000000000000004.
 */

/** Eine mitreisende Person. Wir kennen nur einen Namen, kein Konto. */
data class Traveler(
    val id: Long,
    val name: String,
)

/** Der Anteil einer Person an einer Ausgabe. */
data class Share(
    val travelerId: Long,
    val amountCents: Long,
)

/**
 * Eine Ausgabe: Wer hat wie viel bezahlt, und wie ist sie aufgeteilt?
 *
 * Der "init"-Block läuft bei jeder Erzeugung. Mit require() prüfen wir Regeln.
 * Ist eine Regel verletzt, wirft Kotlin eine IllegalArgumentException.
 * So kann es nie eine "kaputte" Ausgabe im Programm geben.
 */
data class Expense(
    val id: Long,
    val description: String,
    val paidByTravelerId: Long,
    val amountCents: Long,
    val shares: List<Share>,
) {
    init {
        require(amountCents > 0) { "Betrag muss größer als 0 sein, war $amountCents" }
        require(shares.isNotEmpty()) { "Eine Ausgabe braucht mindestens einen Anteil" }
        require(shares.sumOf { it.amountCents } == amountCents) {
            "Summe der Anteile (${shares.sumOf { it.amountCents }}) passt nicht zum Betrag ($amountCents)"
        }
        require(shares.map { it.travelerId }.toSet().size == shares.size) {
            "Jede Person darf nur einmal in den Anteilen vorkommen"
        }
    }
}

/**
 * Eine Ausgleichszahlung zwischen zwei Personen:
 * "fromTravelerId zahlt toTravelerId den Betrag amountCents".
 * Wird für Vorschläge UND für bereits erledigte Zahlungen genutzt.
 */
data class Transfer(
    val fromTravelerId: Long,
    val toTravelerId: Long,
    val amountCents: Long,
) {
    init {
        require(amountCents > 0) { "Überweisungsbetrag muss größer als 0 sein" }
        require(fromTravelerId != toTravelerId) { "Niemand kann sich selbst bezahlen" }
    }
}
