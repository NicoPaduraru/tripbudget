package de.tripbudget.domain.model

import java.time.LocalDate
import java.time.LocalTime

/*
 * NEU IN PHASE 4: eine geplante Aktivität im Reiseplan.
 *
 * LocalTime (aus java.time) ist eine Uhrzeit ohne Datum, z. B. 14:30.
 * "time" ist optional (null = ganztägig, z. B. "Strandtag").
 * "plannedCostMinorUnits" ist optional (null = kostenlos oder unbekannt).
 */
data class PlanItem(
    val id: Long,
    val date: LocalDate,
    val time: LocalTime?,
    val title: String,
    val location: String,
    val plannedCostMinorUnits: Long?,
    val note: String,
    /** ID der Ausgabe, wenn die Aktivität schon als Ausgabe übernommen wurde. */
    val expenseId: Long?,
) {
    // Eine berechnete Eigenschaft ("get()"): wird bei jedem Zugriff neu ausgewertet
    val isConverted: Boolean get() = expenseId != null
}

/** Eine neue, geprüfte Aktivität. Entsteht nur über PlanFormValidator. */
data class NewPlanItem(
    val date: LocalDate,
    val time: LocalTime?,
    val title: String,
    val location: String,
    val plannedCostMinorUnits: Long?,
    val note: String,
)
