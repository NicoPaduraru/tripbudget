package de.tripbudget.domain.model

/*
 * NEU IN PHASE 5: Einstellungen der App.
 * Die Sprache gehört nicht hierher – die verwaltet Android selbst (ab Android 13).
 */
data class AppSettings(
    /** Beim Öffnen und nach 1 Minute im Hintergrund nach Fingerabdruck/PIN fragen. */
    val appLockEnabled: Boolean = false,
    /** Screenshots und die Vorschau in der App-Übersicht verhindern. */
    val blockScreenshots: Boolean = false,
)
