package de.tripbudget.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/*
 * ENTITIES = Tabellen der Datenbank.
 * Room liest die Annotationen (@Entity, @PrimaryKey …) und erzeugt daraus
 * beim Bauen automatisch den SQL-Code. Das erledigt KSP (Kotlin Symbol Processing).
 *
 * Warum eigene Klassen und nicht direkt die Domain-Modelle?
 * Die Datenbank hat eigene Bedürfnisse (IDs, Fremdschlüssel, Tabellennamen).
 * So bleibt die Domain-Schicht frei von Room und lässt sich ohne Android testen.
 */

@Entity(tableName = "trips")
data class TripEntity(
    // autoGenerate: Die Datenbank vergibt die ID (1, 2, 3 …). 0 heißt "noch keine ID".
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val destination: String,
    val startDate: LocalDate, // Room kann LocalDate nicht direkt speichern → Converters.kt
    val endDate: LocalDate,
    val budgetMinorUnits: Long,
    val currencyCode: String,
    val createdAtMillis: Long,
)

@Entity(
    tableName = "travelers",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            // Wird eine Reise gelöscht, löscht die Datenbank ihre Mitreisenden automatisch mit.
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // Index auf tripId: schnellere Suche "alle Mitreisenden einer Reise"
    indices = [Index("tripId")],
)
data class TravelerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val name: String,
    val isMe: Boolean, // true = die Person, der das Handy gehört
)

/**
 * Keine Tabelle, sondern das Ergebnis einer Abfrage:
 * alle Spalten einer Reise (@Embedded) + die Anzahl ihrer Mitreisenden.
 */
data class TripSummaryRow(
    @Embedded val trip: TripEntity,
    val travelerCount: Int,
)
