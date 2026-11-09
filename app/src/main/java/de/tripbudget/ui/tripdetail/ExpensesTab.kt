package de.tripbudget.ui.tripdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.tripbudget.R
import de.tripbudget.domain.logic.BudgetStatus
import de.tripbudget.domain.logic.BudgetSummary
import de.tripbudget.domain.model.TripDetail
import de.tripbudget.domain.util.MoneyFormatter
import de.tripbudget.ui.common.MoneyPositive
import de.tripbudget.ui.common.MoneyWarning
import de.tripbudget.ui.common.formatDateRange
import de.tripbudget.ui.common.formatDayHeader
import de.tripbudget.ui.common.labelRes
import java.util.Locale

/*
 * Tab "Ausgaben": Budgetkarte + Ausgaben nach Tagen.
 * Der Inhalt stammt aus Phase 2 und wurde nur in eine eigene Datei verschoben.
 */
@Composable
fun ExpensesTab(
    uiState: TripDetailUiState,
    trip: TripDetail,
    contentPadding: PaddingValues,
) {
    val locale = LocalConfiguration.current.locales[0]

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            Text(
                text = formatDateRange(trip.startDate, trip.endDate, locale),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        uiState.budget?.let { budget ->
            item(key = "budget") { BudgetCard(budget, trip.currencyCode, locale) }
        }
        item(key = "expenses-title") {
            Text(
                text = stringResource(R.string.expenses_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        if (uiState.dayGroups.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.expenses_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        uiState.dayGroups.forEach { group ->
            item(key = "day-${group.date}") {
                DayHeader(group, trip.currencyCode, locale)
            }
            items(group.items, key = { it.expense.id }) { item ->
                ExpenseRow(item, trip.currencyCode, locale)
            }
        }
    }
}

@Composable
private fun BudgetCard(budget: BudgetSummary, currencyCode: String, locale: Locale) {
    val statusColor = when (budget.status) {
        BudgetStatus.ON_TRACK -> MoneyPositive
        BudgetStatus.WARNING -> MoneyWarning
        BudgetStatus.OVER -> MaterialTheme.colorScheme.error
    }
    val resources = LocalContext.current.resources
    fun money(value: Long) = MoneyFormatter.format(value, currencyCode, locale)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.budget_remaining_label), style = MaterialTheme.typography.labelLarge)
            Text(
                text = money(budget.remainingMinorUnits),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
            )
            LinearProgressIndicator(
                progress = { (budget.usedPercent / 100f).coerceIn(0f, 1f) },
                color = statusColor,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            Text(
                text = stringResource(
                    R.string.budget_spent_of,
                    money(budget.spentMinorUnits),
                    money(budget.budgetMinorUnits),
                    budget.usedPercent,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            val daily = budget.dailyBudgetMinorUnits
            if (daily != null) {
                Text(
                    text = stringResource(R.string.budget_daily, money(daily)) + " · " +
                        resources.getQuantityString(R.plurals.days_left, budget.daysLeft, budget.daysLeft),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(stringResource(R.string.budget_trip_over), style = MaterialTheme.typography.bodyMedium)
            }
            // NEU IN PHASE 4: geplante, noch nicht bezahlte Kosten aus dem Reiseplan
            if (budget.plannedOpenMinorUnits > 0) {
                Text(
                    text = stringResource(
                        R.string.budget_planned,
                        money(budget.plannedOpenMinorUnits),
                        money(budget.remainingAfterPlanMinorUnits),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (budget.remainingAfterPlanMinorUnits < 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (budget.status == BudgetStatus.OVER) {
                Text(
                    text = stringResource(R.string.budget_over, money(-budget.remainingMinorUnits)),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            } else if (budget.status == BudgetStatus.WARNING) {
                Text(
                    text = stringResource(R.string.budget_warning),
                    color = MoneyWarning,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun DayHeader(group: ExpenseDayGroup, currencyCode: String, locale: Locale) {
    Column(Modifier.padding(top = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDayHeader(group.date, locale), style = MaterialTheme.typography.labelLarge)
            Text(
                MoneyFormatter.format(group.totalMinorUnits, currencyCode, locale),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        HorizontalDivider(Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ExpenseRow(item: ExpenseItemUi, currencyCode: String, locale: Locale) {
    val resources = LocalContext.current.resources
    val expense = item.expense
    val categoryLabel = stringResource(expense.category.labelRes())

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = expense.description.ifBlank { categoryLabel },
                style = MaterialTheme.typography.bodyLarge,
            )
            val details = listOf(
                categoryLabel,
                stringResource(R.string.expense_paid_by, item.payerName),
                resources.getQuantityString(R.plurals.expense_split_count, item.participantCount, item.participantCount),
            )
            Text(
                text = details.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = MoneyFormatter.format(expense.amountMinorUnits, currencyCode, locale),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
