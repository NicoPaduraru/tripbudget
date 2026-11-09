package de.tripbudget.ui.addtrip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.tripbudget.domain.logic.TripFormError
import de.tripbudget.domain.logic.TripFormInput
import de.tripbudget.domain.logic.TripFormResult
import de.tripbudget.domain.logic.TripFormValidator
import de.tripbudget.domain.model.TravelerDraft
import de.tripbudget.domain.model.TripEditInfo
import de.tripbudget.domain.repository.TripRepository
import de.tripbudget.domain.util.MoneyFormatter
import de.tripbudget.ui.navigation.Routes
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Alles, was der Bildschirm "Reise anlegen / bearbeiten" anzeigt. */
data class AddTripUiState(
    val input: TripFormInput = TripFormInput(),
    val newTravelerName: String = "",            // Textfeld "Person hinzufügen"
    val errors: Set<TripFormError> = emptySet(),
    val showErrors: Boolean = false,             // Fehler erst nach dem ersten "Speichern" zeigen
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,                // true → Bildschirm schließt sich
    // ERWEITERUNG 1: Bearbeiten
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,              // Reise wird noch aus der Datenbank geladen
    val notFound: Boolean = false,
    val currencyLocked: Boolean = false,         // es gibt schon Ausgaben
    val lockedTravelerIds: Set<Long> = emptySet(), // Personen mit Ausgaben/Zahlungen
)

/*
 * UNIDIRECTIONAL DATA FLOW (Daten fließen in eine Richtung):
 *   Bildschirm → ruft eine Funktion auf (Event), z. B. onNameChange("Lissabon")
 *   ViewModel  → berechnet einen neuen UiState
 *   Bildschirm → zeichnet sich mit dem neuen Zustand neu
 * Der Bildschirm ändert nie selbst Daten. So bleibt alles nachvollziehbar.
 *
 * ERWEITERUNG 1: Derselbe Bildschirm dient zum Anlegen UND Bearbeiten.
 *   Route "add_trip"             → tripId = -1 → neue Reise
 *   Route "add_trip?tripId=7"    → Reise 7 laden und bearbeiten
 * So gibt es Formular, Prüfung und Fehlertexte nur einmal.
 */
@HiltViewModel
class AddTripViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TripRepository,
) : ViewModel() {

    private val tripId: Long = savedStateHandle.get<Long>(Routes.ARG_TRIP_ID) ?: Routes.NO_TRIP
    private val isEditMode = tripId != Routes.NO_TRIP

    // privat veränderbar (_uiState), nach außen nur lesbar (uiState)
    private val _uiState = MutableStateFlow(AddTripUiState(isEditMode = isEditMode, isLoading = isEditMode))
    val uiState: StateFlow<AddTripUiState> = _uiState.asStateFlow()

    private val maxLength = TripFormValidator.MAX_TEXT_LENGTH

    init {
        if (isEditMode) loadTrip()
    }

    /** ERWEITERUNG 1: bestehende Reise laden und das Formular damit füllen. */
    private fun loadTrip() {
        viewModelScope.launch {
            val info = repository.getTripEditInfo(tripId)
            if (info == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, input = info.toInput(), currencyLocked = info.hasExpenses,
                    lockedTravelerIds = info.travelerIdsInUse) }
            }
        }
    }

    // take(n) schneidet zu lange Eingaben ab
    fun onNameChange(value: String) = updateInput { it.copy(name = value.take(maxLength)) }

    fun onDestinationChange(value: String) = updateInput { it.copy(destination = value.take(maxLength)) }

    fun onBudgetChange(value: String) = updateInput { it.copy(budgetText = value.take(15)) }

    fun onCurrencyChange(code: String) {
        if (_uiState.value.currencyLocked) return // Sicherheitsnetz, die UI sperrt die Auswahl schon
        updateInput { it.copy(currencyCode = code) }
    }

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
        updateInput { it.copy(otherTravelers = it.otherTravelers + TravelerDraft(name = name)) }
        _uiState.update { it.copy(newTravelerName = "") }
    }

    /** ERWEITERUNG 1: Person in der Liste umbenennen. */
    fun onTravelerNameChange(index: Int, value: String) = updateInput { input ->
        input.copy(otherTravelers = input.otherTravelers.mapIndexed { i, traveler ->
            if (i == index) traveler.copy(name = value.take(maxLength)) else traveler
        })
    }

    fun removeTraveler(index: Int) {
        val traveler = _uiState.value.input.otherTravelers.getOrNull(index) ?: return
        if (traveler.id in _uiState.value.lockedTravelerIds) return // hängt an Ausgaben/Zahlungen
        updateInput { input -> input.copy(otherTravelers = input.otherTravelers.filterIndexed { i, _ -> i != index }) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return // doppeltes Antippen ignorieren

        when (val result = TripFormValidator.validate(state.input)) {
            is TripFormResult.Invalid -> _uiState.update {
                it.copy(errors = result.errors, showErrors = true)
            }

            is TripFormResult.Valid -> {
                _uiState.update { it.copy(isSaving = true) }
                /*
                 * viewModelScope.launch startet eine COROUTINE: Code, der warten kann,
                 * ohne die Oberfläche einzufrieren. Room speichert im Hintergrund.
                 */
                viewModelScope.launch {
                    if (isEditMode) {
                        repository.updateTrip(tripId, result.trip)
                    } else {
                        repository.createTrip(result.trip)
                    }
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

/** ERWEITERUNG 1: gespeicherte Reise → Formulareingaben (die Umkehrung von TripFormValidator). */
private fun TripEditInfo.toInput(): TripFormInput {
    val me = trip.travelers.first { it.isMe } // "ich" gibt es in jeder Reise genau einmal
    return TripFormInput(
        name = trip.name,
        destination = trip.destination,
        startDate = trip.startDate,
        endDate = trip.endDate,
        budgetText = MoneyFormatter.toInputText(trip.budgetMinorUnits, trip.currencyCode),
        currencyCode = trip.currencyCode,
        myName = me.name,
        myTravelerId = me.id,
        otherTravelers = trip.travelers.filter { !it.isMe }.map { TravelerDraft(it.id, it.name) },
        usedFrom = firstUsedDate,
        usedUntil = lastUsedDate,
    )
}
