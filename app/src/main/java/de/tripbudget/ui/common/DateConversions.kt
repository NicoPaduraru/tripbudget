package de.tripbudget.ui.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/*
 * Die Material-DatePicker arbeiten mit Millisekunden in UTC,
 * unsere App mit LocalDate. Diese zwei Funktionen übersetzen.
 * (In Phase 1 standen sie privat in AddTripScreen.kt; jetzt nutzen sie
 * zwei Bildschirme, deshalb liegen sie hier – "Don't Repeat Yourself".)
 */
fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
