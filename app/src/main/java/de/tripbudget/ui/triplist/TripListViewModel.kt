package de.tripbudget.ui.triplist

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import de.tripbudget.domain.model.TripSummary
import de.tripbudget.domain.repository.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/*
 * UI-STATE: alles, was der Bildschirm zum Zeichnen braucht, in EINER Klasse.
 * Der Bildschirm zeigt nur an, was hier steht – er entscheidet nichts selbst.
 */
data class TripListUiState(
    val trips: List<TripSummary> = emptyList(),
    val isLoading: Boolean = true,
)

/*
 * Das ViewModel bereitet die Daten für den Bildschirm auf.
 * Es überlebt das Drehen des Handys; der Bildschirm wird dabei neu gezeichnet,
 * die Daten bleiben erhalten.
 */
/*
 * PHASE 5b:
 *   @HiltViewModel      = "Hilt, du darfst dieses ViewModel bauen."
 *   @Inject constructor = "Das hier sind meine Zutaten – besorg sie dir selbst."
 * Hilt schaut nach, wer ein TripRepository liefern kann (RepositoryModule) und reicht es herein.
 */
@HiltViewModel
class TripListViewModel @Inject constructor(repository: TripRepository) : ViewModel() {

    /*
     * Aus dem Flow des Repositorys wird ein StateFlow (hat immer einen aktuellen Wert).
     * - map: jede neue Reiseliste → neuer UiState
     * - stateIn: startet den Flow im viewModelScope (wird beim Schließen automatisch beendet)
     * - WhileSubscribed(5_000): läuft weiter, solange jemand zuschaut, plus 5 Sekunden
     *   (damit beim Drehen des Bildschirms nicht neu geladen wird)
     */
    val uiState: StateFlow<TripListUiState> = repository.observeTrips()
        .map { trips -> TripListUiState(trips = trips, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TripListUiState(isLoading = true),
        )
}
