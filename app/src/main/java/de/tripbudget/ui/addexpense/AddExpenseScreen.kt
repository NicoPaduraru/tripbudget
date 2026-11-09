package de.tripbudget.ui.addexpense

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.rememberDatePickerState
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
import de.tripbudget.domain.logic.ExpenseFormError
import de.tripbudget.domain.logic.ExpenseFormInput
import de.tripbudget.domain.model.ExpenseCategory
import de.tripbudget.domain.model.Traveler
import de.tripbudget.domain.model.TripDetail
import de.tripbudget.domain.util.MoneyFormatter
import de.tripbudget.ui.common.formatDate
import de.tripbudget.ui.common.labelRes
import de.tripbudget.ui.common.messageRes
import de.tripbudget.ui.common.toLocalDate
import de.tripbudget.ui.common.toUtcMillis
import de.tripbudget.ui.theme.TripBudgetTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_expense_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val trip = uiState.trip
        if (uiState.isLoading || trip == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            AddExpenseForm(
                uiState = uiState,
                trip = trip,
                onAmountChange = viewModel::onAmountChange,
                onCategoryChange = viewModel::onCategoryChange,
                onDescriptionChange = viewModel::onDescriptionChange,
                onPickDate = { showDatePicker = true },
                onPayerChange = viewModel::onPayerChange,
                onToggleParticipant = viewModel::onToggleParticipant,
                onSave = viewModel::save,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (showDatePicker) {
        ExpenseDatePickerDialog(
            initialDate = uiState.input.date,
            onConfirm = { date ->
                viewModel.onDateChange(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseForm(
    uiState: AddExpenseUiState,
    trip: TripDetail,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (ExpenseCategory) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPickDate: () -> Unit,
    onPayerChange: (Long) -> Unit,
    onToggleParticipant: (Long) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val input = uiState.input
    val errors = uiState.errors
    val locale = LocalConfiguration.current.locales[0]

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // PHASE 4: Hinweis, wenn die Ausgabe aus dem Reiseplan kommt
        uiState.sourcePlanTitle?.let { title ->
            Text(
                text = stringResource(R.string.expense_from_plan, title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // --- Betrag (groß, weil am wichtigsten) ---
        OutlinedTextField(
            value = input.amountText,
            onValueChange = onAmountChange,
            label = { Text(stringResource(R.string.field_amount)) },
            suffix = { Text(trip.currencyCode) },
            textStyle = MaterialTheme.typography.headlineSmall,
            singleLine = true,
            isError = ExpenseFormError.AMOUNT_INVALID in errors,
            supportingText = errorText(errors, ExpenseFormError.AMOUNT_INVALID),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )

        // --- Kategorie ---
        SectionLabel(stringResource(R.string.field_category))
        ChipRow {
            // entries = alle Werte eines Enums (seit Kotlin 1.9, ersetzt values())
            for (category in ExpenseCategory.entries) {
                FilterChip(
                    selected = input.category == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(stringResource(category.labelRes())) },
                )
            }
        }
        ErrorLine(errors, ExpenseFormError.CATEGORY_MISSING)

        // --- Beschreibung ---
        OutlinedTextField(
            value = input.description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.field_description)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        // --- Datum ---
        val date = input.date
        OutlinedButton(onClick = onPickDate, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.ic_calendar), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (date != null) formatDate(date, locale) else stringResource(R.string.field_date))
        }
        ErrorLine(errors, ExpenseFormError.DATE_MISSING)

        // --- Bezahlt von (genau eine Person) ---
        SectionLabel(stringResource(R.string.field_paid_by))
        ChipRow {
            for (traveler in trip.travelers) {
                FilterChip(
                    selected = input.paidByTravelerId == traveler.id,
                    onClick = { onPayerChange(traveler.id) },
                    label = { Text(travelerLabel(traveler)) },
                )
            }
        }
        ErrorLine(errors, ExpenseFormError.PAYER_MISSING)

        // --- Aufteilen auf (beliebig viele) ---
        SectionLabel(stringResource(R.string.field_split_between))
        ChipRow {
            for (traveler in trip.travelers) {
                FilterChip(
                    selected = traveler.id in input.sharedWithTravelerIds,
                    onClick = { onToggleParticipant(traveler.id) },
                    label = { Text(travelerLabel(traveler)) },
                )
            }
        }
        ErrorLine(errors, ExpenseFormError.NO_PARTICIPANTS)
        uiState.perPersonPreviewMinorUnits?.let { perPerson ->
            Text(
                text = stringResource(
                    R.string.per_person_preview,
                    MoneyFormatter.format(perPerson, trip.currencyCode, locale),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = onSave,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.action_save_expense))
        }
    }
}

/** "Nico (ich)" für die eigene Person, sonst nur der Name. */
@Composable
private fun travelerLabel(traveler: Traveler): String =
    if (traveler.isMe) stringResource(R.string.me_suffix, traveler.name) else traveler.name

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
}

/*
 * Eine waagerecht scrollbare Reihe für Chips.
 * "content: @Composable () -> Unit" – eine Composable-Funktion als Parameter.
 * So bauen wir eigene Bausteine wie Row oder Column.
 */
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

private fun errorText(
    errors: Set<ExpenseFormError>,
    vararg relevant: ExpenseFormError,
): (@Composable () -> Unit)? {
    val error = relevant.firstOrNull { it in errors } ?: return null
    val content: @Composable () -> Unit = { Text(stringResource(error.messageRes())) }
    return content
}

@Composable
private fun ErrorLine(errors: Set<ExpenseFormError>, vararg relevant: ExpenseFormError) {
    val error = relevant.firstOrNull { it in errors } ?: return
    Text(
        text = stringResource(error.messageRes()),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 16.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDatePickerDialog(
    initialDate: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDate?.toUtcMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(it.toLocalDate()) } },
                enabled = state.selectedDateMillis != null,
            ) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}

/* ---------- Vorschau ---------- */

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AddExpenseFormPreview() {
    val start = LocalDate.of(2027, 5, 12)
    val trip = TripDetail(
        1, "Lissabon", "Portugal", start, start.plusDays(7), 150000, "EUR",
        listOf(Traveler(1, "Nico", isMe = true), Traveler(2, "Anna"), Traveler(3, "Ben")),
    )
    TripBudgetTheme {
        AddExpenseForm(
            uiState = AddExpenseUiState(
                isLoading = false,
                trip = trip,
                input = ExpenseFormInput(
                    amountText = "10",
                    category = ExpenseCategory.FOOD,
                    date = start,
                    paidByTravelerId = 1,
                    sharedWithTravelerIds = setOf(1, 2, 3),
                ),
                perPersonPreviewMinorUnits = 334,
            ),
            trip = trip,
            onAmountChange = {}, onCategoryChange = {}, onDescriptionChange = {}, onPickDate = {},
            onPayerChange = {}, onToggleParticipant = {}, onSave = {},
        )
    }
}
