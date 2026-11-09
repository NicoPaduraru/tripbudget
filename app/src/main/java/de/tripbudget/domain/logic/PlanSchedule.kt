package de.tripbudget.domain.logic

import de.tripbudget.domain.model.PlanItem
import java.time.LocalDate

/*
 * NEU IN PHASE 4: baut aus einer flachen Liste von Aktivitäten den Tagesplan.
 *
 *   Tag 1 · 12.05.  → Ankunft 14:00, Abendessen 19:30
 *   Tag 2 · 13.05.  → (nichts geplant)
 *   Tag 3 · 14.05.  → Strandtag (ganztägig), Surfkurs 10:00
 *
 * Jeder Reisetag erscheint – auch ohne Aktivitäten. So sieht man Lücken im Plan.
 */

data class PlanDay(
    val dayNumber: Int,          // 1, 2, 3 …
    val date: LocalDate,
    val items: List<PlanItem>,
    /** Summe der geplanten Kosten dieses Tages (ohne bereits übernommene). */
    val openCostMinorUnits: Long,
)

object PlanSchedule {

    fun buildDays(tripStart: LocalDate, tripEnd: LocalDate, items: List<PlanItem>): List<PlanDay> {
        val itemsByDate = items.groupBy { it.date }
        val dayCount = BudgetCalculator.daysBetweenInclusive(tripStart, tripEnd)

        // (0 until n) = 0, 1, …, n-1 – ein "Bereich" (Range) in Kotlin
        return (0 until dayCount).map { offset ->
            val date = tripStart.plusDays(offset.toLong())
            val dayItems = itemsByDate[date].orEmpty().sortedWith(
                // Ganztägige (time == null) zuerst, dann nach Uhrzeit, dann nach Titel
                compareBy<PlanItem>({ it.time != null }, { it.time }, { it.title })
            )
            PlanDay(
                dayNumber = offset + 1,
                date = date,
                items = dayItems,
                openCostMinorUnits = openCostOf(dayItems),
            )
        }
    }

    /**
     * Geplante Kosten, die noch NICHT als Ausgabe erfasst wurden.
     * Übernommene Aktivitäten zählen schon als Ausgabe – sonst würden sie doppelt zählen.
     */
    fun openCostOf(items: List<PlanItem>): Long =
        items.filter { !it.isConverted }.sumOf { it.plannedCostMinorUnits ?: 0L }
}
