package de.tripbudget.domain.model

import java.time.LocalDate

/*
 * Modelle für Reisen. Sie gehören zur Domain-Schicht und wissen NICHTS von
 * Room oder Android. Die Datenbank-Klassen heißen deshalb anders
 * (TripEntity, TravelerEntity …) – siehe data/local.
 */

/** Kurzinfo für die Reiseliste. */
data class TripSummary(
    val id: Long,
    val name: String,
    val destination: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val budgetMinorUnits: Long,
    val currencyCode: String,
    val travelerCount: Int,
)

/** NEU IN PHASE 2: eine Reise mit allen Mitreisenden (für die Reiseübersicht). */
data class TripDetail(
    val id: Long,
    val name: String,
    val destination: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val budgetMinorUnits: Long,
    val currencyCode: String,
    val travelers: List<Traveler>, // "ich" steht immer an erster Stelle
)

/**
 * ERWEITERUNG 1: eine Person im Formular "Reise anlegen/bearbeiten".
 * id = null → neue Person, bekommt beim Speichern eine ID.
 * id = 7    → Person gibt es schon in der Datenbank (wird umbenannt oder bleibt).
 */
data class TravelerDraft(
    val id: Long? = null,
    val name: String,
)

/**
 * Eine geprüfte Reise, bereit zum Speichern – beim Anlegen UND beim Bearbeiten.
 * Entsteht nur über TripFormValidator.
 * ERWEITERUNG 1: Personen als TravelerDraft (mit ID) statt nur als Namen.
 */
data class NewTrip(
    val name: String,
    val destination: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val budgetMinorUnits: Long,
    val currencyCode: String,
    val me: TravelerDraft,
    val others: List<TravelerDraft>,
)

/**
 * ERWEITERUNG 1: alles, was der Bildschirm "Reise bearbeiten" über die Reise wissen muss.
 * Manches darf man nicht mehr frei ändern, weil schon Daten daran hängen:
 *   - travelerIdsInUse: Diese Personen haben bezahlt, mitgezahlt oder Geld überwiesen
 *     → sie dürfen nicht entfernt werden (sonst stimmt die Abrechnung nicht mehr).
 *   - hasExpenses: Gibt es Ausgaben, bleibt die Währung fest (45,50 € sind nicht 45,50 ¥).
 *   - firstUsedDate/lastUsedDate: frühestes und spätestes Datum von Ausgaben und Plan
 *     → der neue Zeitraum muss sie einschließen.
 */
data class TripEditInfo(
    val trip: TripDetail,
    val travelerIdsInUse: Set<Long>,
    val hasExpenses: Boolean,
    val firstUsedDate: LocalDate?,
    val lastUsedDate: LocalDate?,
)

/** NEU IN PHASE 2: eine neue, geprüfte Ausgabe. Entsteht nur über ExpenseFormValidator. */
data class NewExpense(
    val description: String,
    val category: ExpenseCategory,
    val date: LocalDate,
    val paidByTravelerId: Long,
    val amountMinorUnits: Long,
    val shares: List<Share>,
)
