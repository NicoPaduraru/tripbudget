package de.tripbudget.ui.common

import androidx.compose.ui.graphics.Color

/*
 * Farben mit Bedeutung, die mehrere Bildschirme nutzen.
 * (In Phase 2 standen sie privat in TripDetailScreen.kt; seit Phase 3 braucht
 * sie auch die Abrechnung – deshalb liegen sie jetzt hier.)
 * Rot nehmen wir aus dem Theme: MaterialTheme.colorScheme.error.
 */
val MoneyPositive = Color(0xFF2E7D32) // grün: im Budget / bekommt Geld
val MoneyWarning = Color(0xFFEF6C00)  // orange: Budget fast aufgebraucht
