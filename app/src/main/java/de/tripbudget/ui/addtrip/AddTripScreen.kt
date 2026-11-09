package de.tripbudget.ui.addtrip

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import de.tripbudget.R
import de.tripbudget.domain.logic.TripFormError
import de.tripbudget.domain.logic.TripFormInput
import de.tripbudget.ui.common.formatDateRange
import de.tripbudget.ui.common.messageRes
import de.tripbudget.ui.common.toLocalDate
import de.tripbudget.ui.common.toUtcMillis
import de.tripbudget.ui.theme.TripBudgetTheme
import java.time.LocalDate

/** Währungen zur Auswahl. Weitere kommen mit der Mehrwährungs-Funktion (Version 1.1). */
private val SUPPORTED_CURRENCIES = listOf("EUR", "USD", "GBP", "CHF", "JPY")

/*
 * BILDSCHIRM 2: Reise anlegen.
 * Wie bei der Reiseliste: AddTripScreen (mit ViewModel) + AddTripForm (nur Anzeige).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddTripViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // rememberSaveable: Zustand, der nur die Anzeige betrifft und das Drehen überlebt
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // LaunchedEffect läuft, sobald sich isSaved ändert → nach dem Speichern zurück zur Liste
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_trip_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        AddTripForm(
            uiState = uiState,
            onNameChange = viewModel::onNameChange, // "::" = Verweis auf eine Funktion
            onDestinationChange = viewModel::onDestinationChange,
            onPickDates = { showDatePicker = true },
            onBudgetChange = viewModel::onBudgetChange,
            onCurrencyChange = viewModel::onCurrencyChange,
            onMyNameChange = viewModel::onMyNameChange,
            onNewTravelerNameChange = viewModel::onNewTravelerNameChange,
            onAddTraveler = viewModel::addTraveler,
            onRemoveTraveler = viewModel::removeTraveler,
            onSave = viewModel::save,
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (showDatePicker) {
        TripDateRangePickerDialog(
            initialStart = uiState.input.startDate,
            initialEnd = uiState.input.endDate,
            onConfirm = { start, end ->
                viewModel.onDatesChange(start, end)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTripForm(
    uiState: AddTripUiState,
    onNameChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onPickDates: () -> Unit,
    onBudgetChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onMyNameChange: (String) -> Unit,
    onNewTravelerNameChange: (String) -> Unit,
    onAddTraveler: () -> Unit,
    onRemoveTraveler: (Int) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val input = uiState.input
    val errors = uiState.errors
    val locale = LocalConfiguration.current.locales[0]

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding() // Platz für die Tastatur lassen
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // --- Name der Reise ---
        OutlinedTextField(
            value = input.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.field_trip_name)) },
            singleLine = true,
            isError = TripFormError.NAME_EMPTY in errors,
            supportingText = errorText(errors, TripFormError.NAME_EMPTY),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // --- Ziel ---
        OutlinedTextField(
            value = input.destination,
            onValueChange = onDestinationChange,
            label = { Text(stringResource(R.string.field_destination)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // --- Zeitraum ---
        val start = input.startDate
        val end = input.endDate
        OutlinedButton(onClick = onPickDates, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.ic_calendar), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (start != null && end != null) {
                    formatDateRange(start, end, locale)
                } else {
                    stringResource(R.string.field_dates_empty)
                }
            )
        }
        ErrorLine(errors, TripFormError.DATES_MISSING, TripFormError.END_BEFORE_START)

        // --- Budget + Währung ---
        OutlinedTextField(
            value = input.budgetText,
            onValueChange = onBudgetChange,
            label = { Text(stringResource(R.string.field_budget)) },
            suffix = { Text(input.currencyCode) },
            singleLine = true,
            isError = TripFormError.BUDGET_INVALID in errors,
            supportingText = errorText(errors, TripFormError.BUDGET_INVALID) ?: budgetHint,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal, // Zahlentastatur
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.field_currency), style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (code in SUPPORTED_CURRENCIES) {
                FilterChip(
                    selected = code == input.currencyCode,
                    onClick = { onCurrencyChange(code) },
                    label = { Text(code) },
                )
            }
        }

        // --- Mitreisende ---
        Text(
            text = stringResource(R.string.section_travelers),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        OutlinedTextField(
            value = input.myName,
            onValueChange = onMyNameChange,
            label = { Text(stringResource(R.string.field_my_name)) },
            singleLine = true,
            isError = TripFormError.MY_NAME_EMPTY in errors,
            supportingText = errorText(errors, TripFormError.MY_NAME_EMPTY),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Bereits hinzugefügte Personen, jeweils mit "Entfernen"-Knopf
        input.otherTravelers.forEachIndexed { index, name ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                IconButton(onClick = { onRemoveTraveler(index) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.action_remove_traveler, name),
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = uiState.newTravelerName,
                onValueChange = onNewTravelerNameChange,
                label = { Text(stringResource(R.string.field_add_traveler)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                // "Fertig" auf der Tastatur fügt die Person direkt hinzu
                keyboardActions = KeyboardActions(onDone = { onAddTraveler() }),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onAddTraveler,
                enabled = uiState.newTravelerName.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_add))
            }
        }
        ErrorLine(errors, TripFormError.DUPLICATE_TRAVELER, TripFormError.TOO_MANY_TRAVELERS)

        // --- Speichern ---
        Button(
            onClick = onSave,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.action_save_trip))
        }
    }
}

/**
 * Liefert den Fehlertext für ein Textfeld – oder null, wenn kein passender Fehler vorliegt.
 * Der Rückgabetyp ist eine Composable-Funktion, weil "supportingText" genau das erwartet.
 */
private fun errorText(
    errors: Set<TripFormError>,
    vararg relevant: TripFormError,
): (@Composable () -> Unit)? {
    val error = relevant.firstOrNull { it in errors } ?: return null
    // Explizite Typangabe, damit der Compiler die Lambda sicher als @Composable erkennt
    val content: @Composable () -> Unit = { Text(stringResource(error.messageRes())) }
    return content
}

/** Hinweistext unter dem Budget-Feld, wenn kein Fehler angezeigt wird. */
private val budgetHint: @Composable () -> Unit = { Text(stringResource(R.string.budget_hint)) }

/** Rote Fehlerzeile für Bereiche ohne eigenes Textfeld (Zeitraum, Mitreisende). */
@Composable
private fun ErrorLine(errors: Set<TripFormError>, vararg relevant: TripFormError) {
    val error = relevant.firstOrNull { it in errors } ?: return
    Text(
        text = stringResource(error.messageRes()),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 16.dp),
    )
}

/*
 * Dialog zur Auswahl des Reisezeitraums.
 * Der DatePicker arbeitet mit Millisekunden in UTC – wir rechnen in LocalDate um.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDateRangePickerDialog(
    initialStart: LocalDate?,
    initialEnd: LocalDate?,
    onConfirm: (LocalDate?, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStart?.toUtcMillis(),
        initialSelectedEndDateMillis = initialEnd?.toUtcMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        state.selectedStartDateMillis?.toLocalDate(),
                        state.selectedEndDateMillis?.toLocalDate(),
                    )
                },
                enabled = state.selectedStartDateMillis != null,
            ) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DateRangePicker(
            state = state,
            modifier = Modifier.weight(1f), // nimmt den restlichen Platz im Dialog ein
            title = {
                Text(
                    text = stringResource(R.string.date_picker_title),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
            showModeToggle = false,
        )
    }
}

/* ---------- Vorschau ---------- */

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun AddTripFormPreview() {
    TripBudgetTheme {
        AddTripForm(
            uiState = AddTripUiState(
                input = TripFormInput(
                    name = "Lissabon",
                    startDate = LocalDate.of(2027, 5, 12),
                    endDate = LocalDate.of(2027, 5, 19),
                    budgetText = "abc",
                    myName = "Nico",
                    otherTravelers = listOf("Anna", "Ben"),
                ),
                errors = setOf(TripFormError.BUDGET_INVALID),
                showErrors = true,
            ),
            onNameChange = {}, onDestinationChange = {}, onPickDates = {}, onBudgetChange = {},
            onCurrencyChange = {}, onMyNameChange = {}, onNewTravelerNameChange = {},
            onAddTraveler = {}, onRemoveTraveler = {}, onSave = {},
        )
    }
}
