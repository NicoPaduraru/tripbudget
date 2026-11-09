package de.tripbudget.ui.tripdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.Clock
import androidx.lifecycle.viewModelScope
import de.tripbudget.domain.logic.BudgetCalculator
import de.tripbudget.domain.logic.BudgetSummary
import de.tripbudget.domain.logic.GroupSettlementCalculator
import de.tripbudget.domain.logic.PlanDay
import de.tripbudget.domain.logic.PlanSchedule
import de.tripbudget.domain.logic.SettlementSummary
import de.tripbudget.domain.model.Expense
import de.tripbudget.domain.model.PlanItem
import de.tripbudget.domain.model.Settlement
import de.tripbudget.domain.model.Transfer
import de.tripbudget.domain.model.TripDetail
import de.tripbudget.domain.repository.ExpenseRepository
import de.tripbudget.domain.repository.PlanRepository
import de.tripbudget.domain.repository.SettlementRepository
import de.tripbudget.domain.repository.TripRepository
import de.tripbudget.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Eine Ausgabe, fertig aufbereitet für eine Zeile in der Liste. */
data class ExpenseItemUi(
    val expense: Expense,
    val payerName: String,
    val participantCount: Int,
)

/** Alle Ausgaben eines Tages mit Tagessumme. */
data class ExpenseDayGroup(
    val date: LocalDate,
    val totalMinorUnits: Long,
    val items: List<ExpenseItemUi>,
)

/** Eine vorgeschlagene Überweisung mit Namen statt IDs. */
data class TransferUi(
    val transfer: Transfer,
    val fromName: String,
    val toName: String,
)

/** Eine erledigte Zahlung mit Namen. */
data class SettlementItemUi(
    val settlement: Settlement,
    val fromName: String,
    val toName: String,
)

data class TripDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val trip: TripDetail? = null,
    // Tab "Plan" (NEU IN PHASE 4)
    val planDays: List<PlanDay> = emptyList(),
    // Tab "Ausgaben"
    val budget: BudgetSummary? = null,
    val dayGroups: List<ExpenseDayGroup> = emptyList(),
    // Tab "Abrechnung"
    val settlement: SettlementSummary? = null,
    val suggestions: List<TransferUi> = emptyList(),
    val history: List<SettlementItemUi> = emptyList(),
)

/*
 * Reiseübersicht mit drei Tabs, ein ViewModel für alle.
 * PHASE 4: combine() mit VIER Flows: Reise, Ausgaben, Zahlungen, Plan.
 * Der Plan beeinflusst auch den Ausgaben-Tab: geplante Kosten in der Budgetkarte.
 */
@HiltViewModel
class TripDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val planRepository: PlanRepository,
    private val clock: Clock,
) : ViewModel() {

    private val tripId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.ARG_TRIP_ID)) {
        "tripId fehlt in der Route"
    }

    val uiState: StateFlow<TripDetailUiState> =
        combine(
            tripRepository.observeTripDetail(tripId),
            expenseRepository.observeExpenses(tripId),
            settlementRepository.observeSettlements(tripId),
            planRepository.observePlanItems(tripId),
        ) { trip, expenses, settlements, planItems ->
            if (trip == null) {
                TripDetailUiState(isLoading = false, notFound = true)
            } else {
                buildState(trip, expenses, settlements, planItems)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TripDetailUiState(isLoading = true),
        )

    /*
     * ERWEITERUNG 1: Wurde die Reise gelöscht? Ein eigener kleiner StateFlow, weil das ein
     * einmaliges Ereignis ist ("jetzt zurück zur Liste"), kein Teil der angezeigten Daten.
     */
    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted.asStateFlow()

    fun deleteTrip() {
        if (_isDeleted.value) return
        viewModelScope.launch {
            tripRepository.deleteTrip(tripId)
            _isDeleted.value = true
        }
    }

    private var isMarking = false

    fun markAsPaid(transfer: Transfer) {
        val current = uiState.value.settlement?.suggestions.orEmpty()
        if (isMarking || transfer !in current) return
        isMarking = true
        viewModelScope.launch {
            try {
                settlementRepository.markAsPaid(tripId, transfer)
            } finally {
                isMarking = false
            }
        }
    }

    fun undoSettlement(settlementId: Long) {
        viewModelScope.launch { settlementRepository.undo(settlementId) }
    }

    /** NEU: Aktivität aus dem Plan löschen. Eine schon übernommene Ausgabe bleibt bestehen. */
    fun deletePlanItem(planItemId: Long) {
        viewModelScope.launch { planRepository.deletePlanItem(planItemId) }
    }

    private fun buildState(
        trip: TripDetail,
        expenses: List<Expense>,
        settlements: List<Settlement>,
        planItems: List<PlanItem>,
    ): TripDetailUiState {
        val namesById = trip.travelers.associate { it.id to it.name }
        fun name(id: Long) = namesById[id] ?: "?"

        val budget = BudgetCalculator.summarize(
            budgetMinorUnits = trip.budgetMinorUnits,
            expenses = expenses,
            startDate = trip.startDate,
            endDate = trip.endDate,
            today = LocalDate.now(clock),
            plannedOpenMinorUnits = PlanSchedule.openCostOf(planItems),
        )

        val dayGroups = expenses
            .groupBy { it.date }
            .entries
            .sortedByDescending { it.key }
            .map { (date, dayExpenses) ->
                ExpenseDayGroup(
                    date = date,
                    totalMinorUnits = dayExpenses.sumOf { it.amountMinorUnits },
                    items = dayExpenses.map { ExpenseItemUi(it, name(it.paidByTravelerId), it.shares.size) },
                )
            }

        val settlement = GroupSettlementCalculator.compute(trip.travelers, expenses, settlements)

        return TripDetailUiState(
            isLoading = false,
            trip = trip,
            planDays = PlanSchedule.buildDays(trip.startDate, trip.endDate, planItems),
            budget = budget,
            dayGroups = dayGroups,
            settlement = settlement,
            suggestions = settlement.suggestions.map { TransferUi(it, name(it.fromTravelerId), name(it.toTravelerId)) },
            history = settlements.map { SettlementItemUi(it, name(it.fromTravelerId), name(it.toTravelerId)) },
        )
    }
}
