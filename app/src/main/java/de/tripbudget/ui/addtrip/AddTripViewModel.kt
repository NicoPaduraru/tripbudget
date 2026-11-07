package de.tripbudget.ui.addtrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tripbudget.domain.logic.TripFormError
import de.tripbudget.domain.logic.TripFormInput
import de.tripbudget.domain.logic.TripFormResult
import de.tripbudget.domain.logic.TripFormValidator
import de.tripbudget.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Alles, was der Bildschirm "Reise anlegen" anzeigt. */
data class AddTripUiState(
    val input: TripFormInput = TripFormInput(),
    val newTravelerName: String = "",            // Textfeld "Person hinzufügen"
    val errors: Set<TripFormError> = emptySet(),
    val showErrors: Boolean = false,             // Fehler erst nach dem ersten "Speichern" zeigen
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,                // true → Bildschirm schließt sich
)

/*
 * UNIDIRECTIONAL DATA FLOW (Daten fließen in eine Richtung):
 *   Bildschirm → ruft eine Funktion auf (Event), z. B. onNameChange("Lissabon")
 *   ViewModel  → berechnet einen neuen UiState
 *   Bildschirm → zeichnet sich mit dem neuen Zustand neu
 * Der Bildschirm ändert nie selbst Daten. So bleibt alles nachvollziehbar.
 */
class AddTripViewModel(private val repository: TripRepository) : ViewModel() {

    // privat veränderbar (_uiState), nach außen nur lesbar (uiState)
    private val _uiState = MutableStateFlow(AddTripUiState())
    val uiState: StateFlow<AddTripUiState> = _uiState.asStateFlow()

    private val maxLength = TripFormValidator.MAX_TEXT_LENGTH

    // take(n) schneidet zu lange Eingaben ab
    fun onNameChange(value: String) = updateInput { it.copy(name = value.take(maxLength)) }

    fun onDestinationChange(value: String) = updateInput { it.copy(destination = value.take(maxLength)) }

    fun onBudgetChange(value: String) = updateInput { it.copy(budgetText = value.take(15)) }

    fun onCurrencyChange(code: String) = updateInput { it.copy(currencyCode = code) }

    fun onMyNameChange(value: String) = updateInput { it.copy(myName = value.take(maxLength)) }

    /** Nur Beginn gewählt? Dann ist es eine eintägige Reise (Ende = Beginn). */
    fun onDatesChange(start: LocalDate?, end: LocalDate?) =
        updateInput { it.copy(startDate = start, endDate = end ?: start) }

    fun onNewTravelerNameChange(value: String) {
        _uiState.update { it.copy(newTravelerName = value.take(maxLength)) }
    }

    fun addTraveler() {
        val name = _uiState.value.newTravelerName.trim()
        if (name.isEmpty()) return
        updateInput { it.copy(otherTravelers = it.otherTravelers + name) }
        _uiState.update { it.copy(newTravelerName = "") }
    }

    fun removeTraveler(index: Int) =
        updateInput { input -> input.copy(otherTravelers = input.otherTravelers.filterIndexed { i, _ -> i != index }) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return // doppeltes Antippen ignorieren

        when (val result = TripFormValidator.validate(state.input)) {
            is TripFormResult.Invalid -> _uiState.update {
                it.copy(errors = result.errors, showErrors = true)
            }

            is TripFormResult.Valid -> {
                _uiState.update { it.copy(isSaving = true) }
                /*
                 * viewModelScope.launch startet eine COROUTINE: Code, der warten kann,
                 * ohne die Oberfläche einzufrieren. createTrip ist "suspend" –
                 * Room erledigt das Speichern im Hintergrund.
                 */
                viewModelScope.launch {
                    repository.createTrip(result.trip)
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                }
            }
        }
    }

    /**
     * Ändert die Eingaben. Nach dem ersten Speicherversuch werden die Fehler
     * bei jeder Eingabe neu geprüft, damit rote Hinweise sofort verschwinden.
     */
    private fun updateInput(transform: (TripFormInput) -> TripFormInput) {
        _uiState.update { state ->
            val newInput = transform(state.input)
            val newErrors = if (state.showErrors) errorsOf(newInput) else emptySet()
            state.copy(input = newInput, errors = newErrors)
        }
    }

    private fun errorsOf(input: TripFormInput): Set<TripFormError> =
        when (val result = TripFormValidator.validate(input)) {
            is TripFormResult.Valid -> emptySet()
            is TripFormResult.Invalid -> result.errors
        }
}
