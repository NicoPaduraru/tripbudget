package de.tripbudget.ui.addplanitem

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.Clock
import androidx.lifecycle.viewModelScope
import de.tripbudget.domain.logic.PlanFormError
import de.tripbudget.domain.logic.PlanFormInput
import de.tripbudget.domain.logic.PlanFormResult
import de.tripbudget.domain.logic.PlanFormValidator
import de.tripbudget.domain.model.TripDetail
import de.tripbudget.domain.repository.PlanRepository
import de.tripbudget.domain.repository.TripRepository
import de.tripbudget.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class AddPlanItemUiState(
    val isLoading: Boolean = true,
    val trip: TripDetail? = null,
    val input: PlanFormInput = PlanFormInput(),
    val errors: Set<PlanFormError> = emptySet(),
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

/*
 * NEU IN PHASE 4: BILDSCHIRM 5 – Aktivität planen.
 * Genau das Muster von AddTripViewModel und AddExpenseViewModel.
 * Versuch: Lies erst diese Datei und vergleiche sie mit AddExpenseViewModel.
 * Fällt dir auf, wie viel gleich ist? Genau das macht Code wartbar.
 */
@HiltViewModel
class AddPlanItemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val planRepository: PlanRepository,
    private val clock: Clock,
) : ViewModel() {

    private val tripId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.ARG_TRIP_ID)) {
        "tripId fehlt in der Route"
    }

    private val _uiState = MutableStateFlow(AddPlanItemUiState())
    val uiState: StateFlow<AddPlanItemUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val trip = tripRepository.observeTripDetail(tripId).filterNotNull().first()
            // Läuft die Reise gerade? Dann heute vorschlagen, sonst den ersten Reisetag.
            val now = LocalDate.now(clock)
            val defaultDate = if (now.isBefore(trip.startDate) || now.isAfter(trip.endDate)) trip.startDate else now
            _uiState.update {
                it.copy(isLoading = false, trip = trip, input = it.input.copy(date = defaultDate))
            }
        }
    }

    fun onTitleChange(value: String) = updateInput { it.copy(title = value.take(PlanFormValidator.MAX_TEXT_LENGTH)) }

    fun onDateChange(date: LocalDate) = updateInput { it.copy(date = date) }

    /** null = Uhrzeit entfernen (ganztägig). */
    fun onTimeChange(time: LocalTime?) = updateInput { it.copy(time = time) }

    fun onLocationChange(value: String) = updateInput { it.copy(location = value.take(PlanFormValidator.MAX_TEXT_LENGTH)) }

    fun onCostChange(value: String) = updateInput { it.copy(costText = value.take(15)) }

    fun onNoteChange(value: String) = updateInput { it.copy(note = value.take(PlanFormValidator.MAX_NOTE_LENGTH)) }

    fun save() {
        val state = _uiState.value
        val trip = state.trip ?: return
        if (state.isSaving) return

        when (val result = validate(state.input, trip)) {
            is PlanFormResult.Invalid -> _uiState.update { it.copy(errors = result.errors, showErrors = true) }
            is PlanFormResult.Valid -> {
                _uiState.update { it.copy(isSaving = true) }
                viewModelScope.launch {
                    planRepository.addPlanItem(tripId, result.item)
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                }
            }
        }
    }

    private fun validate(input: PlanFormInput, trip: TripDetail): PlanFormResult =
        PlanFormValidator.validate(input, trip.currencyCode, trip.startDate, trip.endDate)

    private fun updateInput(transform: (PlanFormInput) -> PlanFormInput) {
        _uiState.update { state ->
            val newInput = transform(state.input)
            val trip = state.trip
            val newErrors = if (state.showErrors && trip != null) {
                (validate(newInput, trip) as? PlanFormResult.Invalid)?.errors ?: emptySet()
            } else {
                emptySet()
            }
            state.copy(input = newInput, errors = newErrors)
        }
    }
}
