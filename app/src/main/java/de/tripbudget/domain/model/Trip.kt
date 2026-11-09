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

/** Eine neue, geprüfte Reise, bereit zum Speichern. Entsteht nur über TripFormValidator. */
data class NewTrip(
    val name: String,
    val destination: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val budgetMinorUnits: Long,
    val currencyCode: String,
    val myName: String,
    val otherTravelerNames: List<String>,
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
