package de.tripbudget.ui.tripdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.tripbudget.R
import de.tripbudget.domain.logic.PersonBalance
import de.tripbudget.domain.model.Transfer
import de.tripbudget.domain.model.TripDetail
import de.tripbudget.domain.util.MoneyFormatter
import de.tripbudget.ui.common.MoneyPositive
import de.tripbudget.ui.common.formatDate
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

/*
 * NEU IN PHASE 3: Tab "Abrechnung".
 *
 * Drei Abschnitte:
 *   1. Salden       – wer bekommt, wer schuldet (und warum: bezahlt vs. Anteil)
 *   2. Vorschlag    – die Überweisungen, mit Knopf "Bezahlt"
 *   3. Erledigt     – bereits bezahlte Überweisungen, mit "Rückgängig"
 */
@Composable
fun SettlementTab(
    uiState: TripDetailUiState,
    trip: TripDetail,
    contentPadding: PaddingValues,
    onMarkPaid: (Transfer) -> Unit,
    onUndo: (Long) -> Unit,
) {
    val summary = uiState.settlement ?: return // noch nichts berechnet
    val locale = LocalConfiguration.current.locales[0]
    fun money(value: Long) = MoneyFormatter.format(value, trip.currencyCode, locale)

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // --- 1. Salden ---
        item(key = "balances-title") {
            SectionTitle(stringResource(R.string.settlement_balances_title))
        }
        items(summary.balances, key = { "balance-${it.traveler.id}" }) { balance ->
            BalanceRow(balance, ::money)
        }

        // --- 2. Vorschlag ---
        item(key = "suggestions-title") {
            SectionTitle(stringResource(R.string.settlement_suggestions_title), Modifier.padding(top = 16.dp))
        }
        if (summary.isSettled) {
            item(key = "settled") { SettledCard() }
        } else {
            items(
                uiState.suggestions,
                key = { "suggestion-${it.transfer.fromTravelerId}-${it.transfer.toTravelerId}" },
            ) { suggestion ->
                SuggestionCard(
                    suggestion = suggestion,
                    amountText = money(suggestion.transfer.amountMinorUnits),
                    onMarkPaid = { onMarkPaid(suggestion.transfer) },
                )
            }
        }

        // --- 3. Erledigte Zahlungen (nur wenn es welche gibt) ---
        if (uiState.history.isNotEmpty()) {
            item(key = "history-title") {
                SectionTitle(stringResource(R.string.settlement_history_title), Modifier.padding(top = 16.dp))
            }
            items(uiState.history, key = { "history-${it.settlement.id}" }) { item ->
                HistoryRow(item, money(item.settlement.amountMinorUnits), locale, onUndo = { onUndo(item.settlement.id) })
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, modifier = modifier)
}

/**
 * Eine Person mit Saldo. "money" ist eine Funktion als Parameter:
 * So muss BalanceRow Währung und Sprache nicht selbst kennen.
 */
@Composable
private fun BalanceRow(balance: PersonBalance, money: (Long) -> String) {
    val value = balance.balanceMinorUnits
    val (statusText, color) = when { // Destructuring: when liefert ein Paar (Text, Farbe)
        value > 0 -> stringResource(R.string.balance_gets, money(value)) to MoneyPositive
        value < 0 -> stringResource(R.string.balance_owes, money(abs(value))) to MaterialTheme.colorScheme.error
        else -> stringResource(R.string.balance_even) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val name = if (balance.traveler.isMe) {
        stringResource(R.string.me_suffix, balance.traveler.name)
    } else {
        balance.traveler.name
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
                // Erklärt, WIE der Saldo entsteht
                Text(
                    text = stringResource(
                        R.string.balance_details,
                        money(balance.paidMinorUnits),
                        money(balance.shareMinorUnits),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = statusText,
                color = color,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun SuggestionCard(suggestion: TransferUi, amountText: String, onMarkPaid: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.transfer_from_to, suggestion.fromName, suggestion.toName),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(text = amountText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            FilledTonalButton(onClick = onMarkPaid) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_mark_paid))
            }
        }
    }
}

@Composable
private fun SettledCard() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MoneyPositive)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.settlement_all_settled), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun HistoryRow(item: SettlementItemUi, amountText: String, locale: Locale, onUndo: () -> Unit) {
    // Zeitpunkt (Millisekunden) → Datum in der Zeitzone des Handys
    val date = Instant.ofEpochMilli(item.settlement.paidAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.transfer_from_to, item.fromName, item.toName) + " · " + amountText,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.settlement_paid_on, formatDate(date, locale)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onUndo) { Text(stringResource(R.string.action_undo)) }
    }
}
