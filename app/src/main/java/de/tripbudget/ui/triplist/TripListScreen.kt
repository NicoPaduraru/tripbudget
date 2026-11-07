package de.tripbudget.ui.triplist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.tripbudget.R
import de.tripbudget.domain.model.TripSummary
import de.tripbudget.domain.util.MoneyFormatter
import de.tripbudget.ui.AppViewModelProvider
import de.tripbudget.ui.common.formatDateRange
import de.tripbudget.ui.theme.TripBudgetTheme
import java.time.LocalDate

/*
 * BILDSCHIRM 1: Reiseliste.
 *
 * Aufteilung in zwei Funktionen (ein wichtiges Compose-Muster):
 *   TripListScreen   – holt den Zustand aus dem ViewModel ("stateful")
 *   TripListContent  – zeichnet nur, was man ihr gibt ("stateless")
 * Vorteil: TripListContent lässt sich in der Vorschau (@Preview) ohne
 * Datenbank anzeigen und leicht testen.
 */
@Composable
fun TripListScreen(
    onAddTrip: () -> Unit,
    viewModel: TripListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    // "by" + collectAsStateWithLifecycle: Wenn sich uiState ändert, zeichnet Compose neu.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TripListContent(uiState = uiState, onAddTrip = onAddTrip)
}

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar ist (noch) als "experimentell" markiert
@Composable
fun TripListContent(
    uiState: TripListUiState,
    onAddTrip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold( // Grundgerüst: obere Leiste, Inhalt, schwebender Button
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.trip_list_title)) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTrip,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.action_new_trip)) },
            )
        },
    ) { innerPadding -> // Platz, den TopAppBar & Co. belegen
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            uiState.trips.isEmpty() -> EmptyTripList(Modifier.padding(innerPadding))

            else -> LazyColumn( // LazyColumn = nur sichtbare Einträge werden gezeichnet (wie RecyclerView)
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp, // Platz für den Button
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // key: hilft Compose, Einträge beim Ändern der Liste wiederzuerkennen
                items(uiState.trips, key = { it.id }) { trip ->
                    TripCard(trip)
                }
            }
        }
    }
}

@Composable
private fun EmptyTripList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.trip_list_empty_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.trip_list_empty_text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun TripCard(trip: TripSummary, modifier: Modifier = Modifier) {
    // Aktuelle Sprache der App, z. B. Deutsch → "12.05.2027" und "1.500,00 €"
    val locale = LocalConfiguration.current.locales[0]
    val resources = LocalContext.current.resources

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = trip.name, style = MaterialTheme.typography.titleLarge)
            if (trip.destination.isNotBlank()) {
                Text(
                    text = trip.destination,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatDateRange(trip.startDate, trip.endDate, locale),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(
                        R.string.trip_budget_label,
                        MoneyFormatter.format(trip.budgetMinorUnits, trip.currencyCode, locale),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
                // Plurals: "1 Person" / "3 Personen" – die Regel kommt aus res/values*/strings.xml
                Text(
                    text = resources.getQuantityString(R.plurals.traveler_count, trip.travelerCount, trip.travelerCount),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/* ---------- Vorschauen: in Android Studio rechts im "Split"-Modus sichtbar ---------- */

@Preview(showBackground = true)
@Composable
private fun TripListPreview() {
    TripBudgetTheme {
        TripListContent(
            uiState = TripListUiState(
                isLoading = false,
                trips = listOf(
                    TripSummary(1, "Lissabon", "Portugal", LocalDate.of(2027, 5, 12), LocalDate.of(2027, 5, 19), 150000, "EUR", 3),
                    TripSummary(2, "Tokio", "Japan", LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 14), 400000, "JPY", 2),
                ),
            ),
            onAddTrip = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripListEmptyPreview() {
    TripBudgetTheme {
        TripListContent(uiState = TripListUiState(isLoading = false), onAddTrip = {})
    }
}
