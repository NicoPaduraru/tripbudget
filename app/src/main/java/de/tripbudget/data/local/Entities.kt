package de.tripbudget.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import de.tripbudget.domain.model.ExpenseCategory
import java.time.LocalDate
import java.time.LocalTime

/*
 * ENTITIES = Tabellen der Datenbank.
 * Room liest die Annotationen und erzeugt beim Bauen den SQL-Code (über KSP).
 *
 * WICHTIG: Jede Änderung an diesen Klassen ändert die Datenbank-Struktur.
 * Dann muss die Version in TripBudgetDatabase steigen und eine Migration her
 * (siehe Migrations.kt). Phase 2 fügt expenses und expense_shares hinzu, Phase 3 settlements,
 * Phase 4 plan_items.
 */

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val destination: String,
    val startDate: LocalDate,
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
            onDelete = ForeignKey.CASCADE, // Reise weg → Mitreisende weg
        ),
    ],
    indices = [Index("tripId")],
)
data class TravelerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val name: String,
    val isMe: Boolean,
)

/* ---------------------------- NEU IN PHASE 2 ---------------------------- */

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE, // Reise weg → Ausgaben weg
        ),
        ForeignKey(
            entity = TravelerEntity::class,
            parentColumns = ["id"],
            childColumns = ["paidByTravelerId"],
            // NO_ACTION: Eine Person, die etwas bezahlt hat, kann nicht einzeln gelöscht werden.
            // (Beim Löschen der ganzen Reise klappt es, weil die Ausgaben mit gelöscht werden.)
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index("tripId"), Index("paidByTravelerId")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val paidByTravelerId: Long,
    val amountMinorUnits: Long,
    val category: ExpenseCategory, // Room speichert Enums automatisch als Text, z. B. "FOOD"
    val description: String,
    val date: LocalDate,
    val createdAtMillis: Long,
)

/**
 * Wer trägt welchen Anteil einer Ausgabe? Eine Zeile pro Person und Ausgabe.
 * Der Primärschlüssel besteht aus ZWEI Spalten: Jede Person kommt pro Ausgabe nur einmal vor.
 */
@Entity(
    tableName = "expense_shares",
    primaryKeys = ["expenseId", "travelerId"],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE, // Ausgabe weg → Anteile weg
        ),
        ForeignKey(
            entity = TravelerEntity::class,
            parentColumns = ["id"],
            childColumns = ["travelerId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index("travelerId")],
)
data class ExpenseShareEntity(
    val expenseId: Long,
    val travelerId: Long,
    val amountMinorUnits: Long,
)

/* ---------------------------- NEU IN PHASE 3 ---------------------------- */

/**
 * Eine erledigte Ausgleichszahlung: "fromTravelerId hat toTravelerId amountMinorUnits gezahlt".
 * Zwei Fremdschlüssel auf DIESELBE Tabelle (travelers) – das ist völlig normal.
 */
@Entity(
    tableName = "settlements",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TravelerEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromTravelerId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = TravelerEntity::class,
            parentColumns = ["id"],
            childColumns = ["toTravelerId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index("tripId"), Index("fromTravelerId"), Index("toTravelerId")],
)
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val fromTravelerId: Long,
    val toTravelerId: Long,
    val amountMinorUnits: Long,
    val paidAtMillis: Long,
)

/* ---------------------------- NEU IN PHASE 4 ---------------------------- */

/**
 * Eine geplante Aktivität.
 * expenseId zeigt auf die Ausgabe, wenn die Aktivität als Ausgabe übernommen wurde.
 * SET_NULL: Wird diese Ausgabe gelöscht, bleibt die Aktivität im Plan und gilt wieder als offen.
 */
@Entity(
    tableName = "plan_items",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("tripId"), Index("expenseId")],
)
data class PlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val date: LocalDate,
    val time: LocalTime?,              // "?" → Spalte darf NULL sein (ganztägig)
    val title: String,
    val location: String,
    val plannedCostMinorUnits: Long?,  // null = keine Kosten geplant
    val note: String,
    val expenseId: Long?,              // null = noch nicht als Ausgabe übernommen
    val createdAtMillis: Long,
)

/* ------------------- Abfrage-Ergebnisse (keine Tabellen) ------------------- */

/** Reise + Anzahl Mitreisende, für die Liste. */
data class TripSummaryRow(
    @Embedded val trip: TripEntity,
    val travelerCount: Int,
)

/**
 * NEU: Reise + alle ihre Mitreisenden.
 * @Relation: Room lädt zu jeder Reise automatisch die passenden Zeilen aus "travelers"
 * (travelers.tripId = trips.id). Das erspart uns ein JOIN von Hand.
 */
data class TripWithTravelers(
    @Embedded val trip: TripEntity,
    @Relation(parentColumn = "id", entityColumn = "tripId")
    val travelers: List<TravelerEntity>,
)

/** NEU: Ausgabe + ihre Anteile. Gleiches Prinzip wie oben. */
data class ExpenseWithShares(
    @Embedded val expense: ExpenseEntity,
    @Relation(parentColumn = "id", entityColumn = "expenseId")
    val shares: List<ExpenseShareEntity>,
)
