package de.tripbudget.domain.model

import java.time.LocalDate

/*
 * Modelle für Reisen. Sie gehören zur Domain-Schicht und wissen NICHTS von
 * Room oder Android. Deshalb heißen die Datenbank-Klassen anders
 * (TripEntity, TravelerEntity) – mehr dazu in data/local.
 *
 * LocalDate (aus java.time) ist ein Datum ohne Uhrzeit, z. B. 2027-05-12.
 * Perfekt für Reisedaten: keine Zeitzonen-Probleme.
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
