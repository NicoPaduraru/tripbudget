package de.tripbudget.ui.addplanitem

import android.text.format.DateFormat
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import de.tripbudget.domain.logic.PlanFormError
import de.tripbudget.domain.logic.PlanFormInput
import de.tripbudget.domain.model.Traveler
import de.tripbudget.domain.model.TripDetail
import de.tripbudget.ui.common.formatDate
import de.tripbudget.ui.common.formatDateRange
import de.tripbudget.ui.common.formatTime
import de.tripbudget.ui.common.messageRes
import de.tripbudget.ui.common.toLocalDate
import de.tripbudget.ui.common.toUtcMillis
import de.tripbudget.ui.theme.TripBudgetTheme
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlanItemScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddPlanItemViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_plan_item_title)) },
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
            AddPlanItemForm(
                uiState = uiState,
                trip = trip,
                onTitleChange = viewModel::onTitleChange,
                onPickDate = { showDatePicker = true },
                onPickTime = { showTimePicker = true },
                onClearTime = { viewModel.onTimeChange(null) },
                onLocationChange = viewModel::onLocationChange,
                onCostChange = viewModel::onCostChange,
                onNoteChange = viewModel::onNoteChange,
                onSave = viewModel::save,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (showDatePicker) {
        PlanDatePickerDialog(
            initialDate = uiState.input.date,
            onConfirm = { date ->
                viewModel.onDateChange(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showTimePicker) {
        PlanTimePickerDialog(
            initialTime = uiState.input.time,
            onConfirm = { time ->
                viewModel.onTimeChange(time)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun AddPlanItemForm(
    uiState: AddPlanItemUiState,
    trip: TripDetail,
    onTitleChange: (String) -> Unit,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    onClearTime: () -> Unit,
    onLocationChange: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
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
        // Zur Orientierung: Zeitraum der Reise
        Text(
            text = stringResource(R.string.plan_trip_dates, formatDateRange(trip.startDate, trip.endDate, locale)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = input.title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.field_plan_title)) },
            singleLine = true,
            isError = PlanFormError.TITLE_EMPTY in errors,
            supportingText = errorText(errors, PlanFormError.TITLE_EMPTY),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )

        // --- Datum und Uhrzeit nebeneinander ---
        val date = input.date
        val time = input.time
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onPickDate, modifier = Modifier.weight(1f)) {
                Icon(painterResource(R.drawable.ic_calendar), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (date != null) formatDate(date, locale) else stringResource(R.string.field_date))
            }
            OutlinedButton(onClick = onPickTime, modifier = Modifier.weight(1f)) {
                Icon(painterResource(R.drawable.ic_time), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (time != null) formatTime(time, locale) else stringResource(R.string.field_time))
            }
            // Uhrzeit entfernen → wieder "ganztägig"
            if (time != null) {
                IconButton(onClick = onClearTime) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_clear_time))
                }
            }
        }
        ErrorLine(errors, PlanFormError.DATE_MISSING, PlanFormError.DATE_OUTSIDE_TRIP)

        OutlinedTextField(
            value = input.location,
            onValueChange = onLocationChange,
            label = { Text(stringResource(R.string.field_location)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = input.costText,
            onValueChange = onCostChange,
            label = { Text(stringResource(R.string.field_planned_cost)) },
            suffix = { Text(trip.currencyCode) },
            singleLine = true,
            isError = PlanFormError.COST_INVALID in errors,
            supportingText = errorText(errors, PlanFormError.COST_INVALID) ?: costHint,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = input.note,
            onValueChange = onNoteChange,
            label = { Text(stringResource(R.string.field_note)) },
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = onSave,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.action_save_plan_item))
        }
    }
}

private val costHint: @Composable () -> Unit = { Text(stringResource(R.string.plan_cost_hint)) }

private fun errorText(errors: Set<PlanFormError>, vararg relevant: PlanFormError): (@Composable () -> Unit)? {
    val error = relevant.firstOrNull { it in errors } ?: return null
    val content: @Composable () -> Unit = { Text(stringResource(error.messageRes())) }
    return content
}

@Composable
private fun ErrorLine(errors: Set<PlanFormError>, vararg relevant: PlanFormError) {
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
private fun PlanDatePickerDialog(initialDate: LocalDate?, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDate?.toUtcMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(it.toLocalDate()) } },
                enabled = state.selectedDateMillis != null,
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}

/*
 * NEU: Uhrzeit wählen. Den TimePicker gibt es nicht als fertigen Dialog,
 * deshalb stecken wir ihn in einen AlertDialog.
 * is24Hour: Wir fragen Android, ob das Handy 24-Stunden-Format nutzt (14:30 statt 2:30 PM).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanTimePickerDialog(initialTime: LocalTime?, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialTime?.hour ?: 12,
        initialMinute = initialTime?.minute ?: 0,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        text = { TimePicker(state = state) },
    )
}

/* ---------- Vorschau ---------- */

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AddPlanItemFormPreview() {
    val start = LocalDate.of(2027, 5, 12)
    val trip = TripDetail(1, "Lissabon", "Portugal", start, start.plusDays(7), 150000, "EUR", listOf(Traveler(1, "Nico", true)))
    TripBudgetTheme {
        AddPlanItemForm(
            uiState = AddPlanItemUiState(
                isLoading = false,
                trip = trip,
                input = PlanFormInput(title = "Surfkurs", date = start.plusDays(1), time = LocalTime.of(10, 0), costText = "135"),
            ),
            trip = trip,
            onTitleChange = {}, onPickDate = {}, onPickTime = {}, onClearTime = {},
            onLocationChange = {}, onCostChange = {}, onNoteChange = {}, onSave = {},
        )
    }
}
