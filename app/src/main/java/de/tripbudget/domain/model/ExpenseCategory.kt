package de.tripbudget.domain.model

/*
 * Feste Kategorien für Ausgaben.
 *
 * Ein ENUM ist eine geschlossene Liste von Werten. Room speichert ihn
 * automatisch als Text ("FOOD", "TRANSPORT" …). Deshalb gilt:
 * Die NAMEN hier nie umbenennen, sonst passen alte Datenbankeinträge nicht mehr!
 * Die Anzeige-Texte (Deutsch/Englisch) stehen in strings.xml.
 *
 * Eigene Kategorien kann man später ergänzen; dann wird daraus eine Tabelle.
 */
enum class ExpenseCategory {
    ACCOMMODATION, // Unterkunft
    TRANSPORT,     // Transport
    FOOD,          // Essen & Trinken
    ACTIVITIES,    // Aktivitäten
    SHOPPING,      // Einkaufen
    OTHER,         // Sonstiges
}
