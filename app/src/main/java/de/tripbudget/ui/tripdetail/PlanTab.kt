package de.tripbudget.ui.tripdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.tripbudget.R
import de.tripbudget.domain.logic.PlanDay
import de.tripbudget.domain.model.PlanItem
import de.tripbudget.domain.model.TripDetail
import de.tripbudget.domain.util.MoneyFormatter
import de.tripbudget.ui.common.MoneyPositive
import de.tripbudget.ui.common.formatDayHeader
import de.tripbudget.ui.common.formatTime
import java.util.Locale

/*
 * NEU IN PHASE 4: Tab "Plan".
 * Für jeden Reisetag: Überschrift "Tag 1 · Mittwoch, 12. Mai 2027", darunter die Aktivitäten.
 */
@Composable
fun PlanTab(
    uiState: TripDetailUiState,
    trip: TripDetail,
    contentPadding: PaddingValues,
    onConvertToExpense: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    fun money(value: Long) = MoneyFormatter.format(value, trip.currencyCode, locale)
    val totalOpen = uiState.budget?.plannedOpenMinorUnits ?: 0L

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "plan-total") {
            Text(
                text = if (totalOpen > 0) {
                    stringResource(R.string.plan_total_open, money(totalOpen))
                } else {
                    stringResource(R.string.plan_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        uiState.planDays.forEach { day ->
            item(key = "plan-day-${day.date}") {
                PlanDayHeader(day, locale, ::money)
            }
            if (day.items.isEmpty()) {
                item(key = "plan-empty-${day.date}") {
                    Text(
                        text = stringResource(R.string.plan_day_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(day.items, key = { "plan-item-${it.id}" }) { item ->
                PlanItemCard(
                    item = item,
                    locale = locale,
                    money = ::money,
                    onConvert = { onConvertToExpense(item.id) },
                    onDelete = { onDelete(item.id) },
                )
            }
        }
    }
}

@Composable
private fun PlanDayHeader(day: PlanDay, locale: Locale, money: (Long) -> String) {
    Column(Modifier.padding(top = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.plan_day_title, day.dayNumber, formatDayHeader(day.date, locale)),
                style = MaterialTheme.typography.labelLarge,
            )
            if (day.openCostMinorUnits > 0) {
                Text(money(day.openCostMinorUnits), style = MaterialTheme.typography.labelLarge)
            }
        }
        HorizontalDivider(Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun PlanItemCard(
    item: PlanItem,
    locale: Locale,
    money: (Long) -> String,
    onConvert: () -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // Uhrzeit links, feste Mindestbreite, damit alle Titel bündig stehen
                Text(
                    text = item.time?.let { formatTime(it, locale) } ?: stringResource(R.string.plan_all_day),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.widthIn(min = 64.dp).padding(top = 2.dp),
                )
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(item.title, style = MaterialTheme.typography.bodyLarge)
                    if (item.location.isNotBlank()) {
                        Text(
                            text = item.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (item.note.isNotBlank()) {
                        Text(
                            text = item.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item.plannedCostMinorUnits?.let { cost ->
                    Text(
                        text = money(cost),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }
            }
            // Aktionen: übernehmen / bereits übernommen + löschen
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.isConverted) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MoneyPositive)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.plan_converted),
                        style = MaterialTheme.typography.bodySmall,
                        color = MoneyPositive,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    TextButton(onClick = onConvert, modifier = Modifier.weight(1f, fill = false)) {
                        Text(stringResource(R.string.action_convert_to_expense))
                    }
                    Spacer(Modifier.weight(1f))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.action_delete_plan_item, item.title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
