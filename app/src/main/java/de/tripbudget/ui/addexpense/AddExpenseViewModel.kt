package de.tripbudget.ui.addexpense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.Clock
import androidx.lifecycle.viewModelScope
import de.tripbudget.domain.logic.CostSplitter
import de.tripbudget.domain.logic.ExpenseFormError
import de.tripbudget.domain.logic.ExpenseFormInput
import de.tripbudget.domain.logic.ExpenseFormResult
import de.tripbudget.domain.logic.ExpenseFormValidator
import de.tripbudget.domain.model.ExpenseCategory
import de.tripbudget.domain.model.TripDetail
import de.tripbudget.domain.repository.ExpenseRepository
import de.tripbudget.domain.repository.PlanRepository
import de.tripbudget.domain.repository.TripRepository
import de.tripbudget.domain.util.MoneyFormatter
import de.tripbudget.domain.util.MoneyParser
import de.tripbudget.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddExpenseUiState(
    val isLoading: Boolean = true,
    val trip: TripDetail? = null,
    val input: ExpenseFormInput = ExpenseFormInput(),
    val errors: Set<ExpenseFormError> = emptySet(),
    val showErrors: Boolean = false,
    /** Größter Anteil pro Person, z. B. 3,34 € bei 10 € für 3 Personen. null = noch nicht berechenbar. */
    val perPersonPreviewMinorUnits: Long? = null,
    /** NEU IN PHASE 4: Titel der Aktivität, wenn die Ausgabe aus dem Reiseplan kommt. */
    val sourcePlanTitle: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

/*
 * BILDSCHIRM 4: Ausgabe erfassen.
 * Gleiches Muster wie AddTripViewModel (Phase 1). Neu ist nur:
 *   - die Reise wird zuerst geladen (für Mitreisende und Währung)
 *   - sinnvolle Vorgaben: heute, "ich" habe bezahlt, für alle
 *   - eine Live-Vorschau "pro Person"
 */
@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val expenseRepository: ExpenseRepository,
    private val planRepository: PlanRepository,
    private val clock: Clock,
) : ViewModel() {

    private val tripId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.ARG_TRIP_ID)) {
        "tripId fehlt in der Route"
    }

    /** NEU IN PHASE 4: optionale Aktivität aus dem Plan (-1 = keine). */
    private val planItemId: Long? = savedStateHandle.get<Long>(Routes.ARG_PLAN_ITEM_ID)
        ?.takeIf { it != Routes.NO_PLAN_ITEM }

    /** Wird beim Speichern verknüpft – nur wenn die Aktivität noch nicht übernommen wurde. */
    private var planItemToLink: Long? = null

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    // init läuft einmal, wenn das ViewModel erzeugt wird
    init {
        viewModelScope.launch {
            // first(): nur den ersten Wert des Flows nehmen, dann aufhören zuzuhören
            val trip = tripRepository.observeTripDetail(tripId).filterNotNull().first()
            val me = trip.travelers.firstOrNull { it.isMe } ?: trip.travelers.first()
            // Nach der Reise eingetragen? Dann standardmäßig der letzte Reisetag.
            val today = LocalDate.now(clock)
            val defaultDate = if (today.isAfter(trip.endDate)) trip.endDate else today

            // PHASE 4: Kommt die Ausgabe aus dem Plan? Dann Werte daraus übernehmen.
            val planItem = planItemId?.let { planRepository.getPlanItem(it) }
            if (planItem != null && !planItem.isConverted) planItemToLink = planItem.id

            var input = ExpenseFormInput(
                date = defaultDate,
                paidByTravelerId = me.id,
                sharedWithTravelerIds = trip.travelers.map { t -> t.id }.toSet(),
            )
            if (planItem != null) {
                input = input.copy(
                    amountText = planItem.plannedCostMinorUnits
                        ?.let { MoneyFormatter.toInputText(it, trip.currencyCode) }
                        .orEmpty(),
                    category = ExpenseCategory.ACTIVITIES,
                    description = planItem.title.take(ExpenseFormValidator.MAX_DESCRIPTION_LENGTH),
                    date = planItem.date,
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    trip = trip,
                    input = input,
                    sourcePlanTitle = planItem?.title,
                    perPersonPreviewMinorUnits = perPersonPreview(input, trip),
                )
            }
        }
    }

    fun onAmountChange(value: String) = updateInput { it.copy(amountText = value.take(15)) }

    fun onCategoryChange(category: ExpenseCategory) = updateInput { it.copy(category = category) }

    fun onDescriptionChange(value: String) =
        updateInput { it.copy(description = value.take(ExpenseFormValidator.MAX_DESCRIPTION_LENGTH)) }

    fun onDateChange(date: LocalDate) = updateInput { it.copy(date = date) }

    fun onPayerChange(travelerId: Long) = updateInput { it.copy(paidByTravelerId = travelerId) }

    /** Person an- oder abwählen. Mengen (Set): "+" fügt hinzu, "-" entfernt. */
    fun onToggleParticipant(travelerId: Long) = updateInput { input ->
        val current = input.sharedWithTravelerIds
        input.copy(sharedWithTravelerIds = if (travelerId in current) current - travelerId else current + travelerId)
    }

    fun save() {
        val state = _uiState.value
        val trip = state.trip ?: return
        if (state.isSaving) return

        when (val result = validate(state.input, trip)) {
            is ExpenseFormResult.Invalid -> _uiState.update { it.copy(errors = result.errors, showErrors = true) }
            is ExpenseFormResult.Valid -> {
                _uiState.update { it.copy(isSaving = true) }
                viewModelScope.launch {
                    expenseRepository.addExpense(tripId, result.expense, fromPlanItemId = planItemToLink)
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                }
            }
        }
    }

    private fun validate(input: ExpenseFormInput, trip: TripDetail): ExpenseFormResult =
        ExpenseFormValidator.validate(input, trip.currencyCode, trip.travelers.map { it.id })

    private fun updateInput(transform: (ExpenseFormInput) -> ExpenseFormInput) {
        _uiState.update { state ->
            val newInput = transform(state.input)
            val trip = state.trip
            val newErrors = if (state.showErrors && trip != null) {
                (validate(newInput, trip) as? ExpenseFormResult.Invalid)?.errors ?: emptySet()
            } else {
                emptySet()
            }
            state.copy(
                input = newInput,
                errors = newErrors,
                perPersonPreviewMinorUnits = trip?.let { perPersonPreview(newInput, it) },
            )
        }
    }

    /** Berechnet die Vorschau mit dem CostSplitter – unabhängig davon, ob schon alles ausgefüllt ist. */
    private fun perPersonPreview(input: ExpenseFormInput, trip: TripDetail): Long? {
        val digits = MoneyFormatter.fractionDigitsOf(trip.currencyCode)
        val amount = MoneyParser.parseToMinorUnits(input.amountText, digits) ?: return null
        val participants = trip.travelers.map { it.id }.filter { it in input.sharedWithTravelerIds }
        if (amount <= 0 || participants.isEmpty()) return null
        return CostSplitter.splitEvenly(amount, participants).maxOf { it.amountMinorUnits }
    }
}
