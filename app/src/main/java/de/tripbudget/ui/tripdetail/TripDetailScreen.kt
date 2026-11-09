package de.tripbudget.ui.tripdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import de.tripbudget.R
import de.tripbudget.domain.logic.BudgetStatus
import de.tripbudget.domain.logic.BudgetSummary
import de.tripbudget.domain.logic.GroupSettlementCalculator
import de.tripbudget.domain.logic.PlanSchedule
import de.tripbudget.domain.model.Expense
import de.tripbudget.domain.model.ExpenseCategory
import de.tripbudget.domain.model.PlanItem
import de.tripbudget.domain.model.Share
import de.tripbudget.domain.model.Transfer
import de.tripbudget.domain.model.Traveler
import de.tripbudget.domain.model.TripDetail
import de.tripbudget.ui.theme.TripBudgetTheme
import java.time.LocalDate
import java.time.LocalTime

/*
 * Die Tabs innerhalb einer Reise. PHASE 4: "Plan" ist dazugekommen.
 * Ein Enum ist "Serializable" – deshalb kann rememberSaveable den gewählten
 * Tab über das Drehen des Handys hinweg merken.
 */
enum class TripTab { PLAN, EXPENSES, SETTLEMENT }

/*
 * BILDSCHIRM 3: Reiseübersicht – PHASE 3 mit unterer Navigationsleiste.
 *
 * Aufteilung auf drei Dateien:
 *   TripDetailScreen.kt – Gerüst: obere Leiste, Tabs, Button
 *   ExpensesTab.kt      – Inhalt "Ausgaben" (aus Phase 2)
 *   SettlementTab.kt    – Inhalt "Abrechnung" (Phase 3)
 *   PlanTab.kt          – Inhalt "Plan" (NEU IN PHASE 4)
 * Kleine Dateien mit einer klaren Aufgabe sind leichter zu lesen und zu ändern.
 */
@Composable
fun TripDetailScreen(
    onBack: () -> Unit,
    onAddExpense: () -> Unit,
    onAddPlanItem: () -> Unit,
    onConvertPlanItem: (Long) -> Unit,
    viewModel: TripDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Welcher Tab gewählt ist, betrifft nur die Anzeige → Zustand in der UI, nicht im ViewModel
    var selectedTab by rememberSaveable { mutableStateOf(TripTab.EXPENSES) }

    TripDetailContent(
        uiState = uiState,
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        onBack = onBack,
        onAddExpense = onAddExpense,
        onAddPlanItem = onAddPlanItem,
        onConvertPlanItem = onConvertPlanItem,
        onDeletePlanItem = viewModel::deletePlanItem,
        onMarkPaid = viewModel::markAsPaid,
        onUndoSettlement = viewModel::undoSettlement,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailContent(
    uiState: TripDetailUiState,
    selectedTab: TripTab,
    onTabSelected: (TripTab) -> Unit,
    onBack: () -> Unit,
    onAddExpense: () -> Unit,
    onAddPlanItem: () -> Unit,
    onConvertPlanItem: (Long) -> Unit,
    onDeletePlanItem: (Long) -> Unit,
    onMarkPaid: (Transfer) -> Unit,
    onUndoSettlement: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trip = uiState.trip

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(trip?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
        bottomBar = {
            if (trip != null) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == TripTab.PLAN,
                        onClick = { onTabSelected(TripTab.PLAN) },
                        icon = { Icon(painterResource(R.drawable.ic_calendar), contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_plan)) },
                    )
                    NavigationBarItem(
                        selected = selectedTab == TripTab.EXPENSES,
                        onClick = { onTabSelected(TripTab.EXPENSES) },
                        icon = { Icon(painterResource(R.drawable.ic_receipt), contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_expenses)) },
                    )
                    NavigationBarItem(
                        selected = selectedTab == TripTab.SETTLEMENT,
                        onClick = { onTabSelected(TripTab.SETTLEMENT) },
                        icon = { Icon(painterResource(R.drawable.ic_swap), contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_settlement)) },
                    )
                }
            }
        },
        floatingActionButton = {
            // Der Button hängt vom Tab ab: Plan → Aktivität, Ausgaben → Ausgabe, Abrechnung → keiner
            if (trip != null && selectedTab != TripTab.SETTLEMENT) {
                val isPlan = selectedTab == TripTab.PLAN
                ExtendedFloatingActionButton(
                    onClick = if (isPlan) onAddPlanItem else onAddExpense,
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = {
                        Text(stringResource(if (isPlan) R.string.action_add_plan_item else R.string.action_add_expense))
                    },
                )
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.notFound || trip == null -> Box(
                Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.trip_not_found), textAlign = TextAlign.Center)
            }

            else -> {
                // Abstand für obere Leiste, untere Leiste und (im Ausgaben-Tab) den schwebenden Button
                val contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() +
                        if (selectedTab == TripTab.SETTLEMENT) 16.dp else 88.dp,
                )
                when (selectedTab) {
                    TripTab.PLAN -> PlanTab(uiState, trip, contentPadding, onConvertPlanItem, onDeletePlanItem)
                    TripTab.EXPENSES -> ExpensesTab(uiState, trip, contentPadding)
                    TripTab.SETTLEMENT -> SettlementTab(uiState, trip, contentPadding, onMarkPaid, onUndoSettlement)
                }
            }
        }
    }
}

/* ---------- Vorschauen: Beispieldaten für beide Tabs ---------- */

private fun previewState(): TripDetailUiState {
    val start = LocalDate.of(2027, 5, 12)
    val travelers = listOf(Traveler(1, "Nico", isMe = true), Traveler(2, "Anna"), Traveler(3, "Ben"))
    val trip = TripDetail(1, "Lissabon", "Portugal", start, start.plusDays(7), 150000, "EUR", travelers)
    val dinner = Expense(1, "Abendessen", ExpenseCategory.FOOD, start, 2, 9000,
        listOf(Share(1, 3000), Share(2, 3000), Share(3, 3000)))
    val taxi = Expense(2, "", ExpenseCategory.TRANSPORT, start, 1, 2500, listOf(Share(1, 1250), Share(3, 1250)))
    val settlement = GroupSettlementCalculator.compute(travelers, listOf(dinner, taxi), emptyList())
    val names = travelers.associate { it.id to it.name }
    val plan = listOf(
        PlanItem(1, start, LocalTime.of(14, 0), "Ankunft & Check-in", "Alfama", null, "", null),
        PlanItem(2, start, LocalTime.of(19, 30), "Abendessen", "Time Out Market", 9000, "", 1),
        PlanItem(3, start.plusDays(1), null, "Strandtag", "Cascais", null, "", null),
        PlanItem(4, start.plusDays(1), LocalTime.of(10, 0), "Surfkurs", "Carcavelos", 13500, "3 × 45 €", null),
    )
    return TripDetailUiState(
        isLoading = false,
        trip = trip,
        planDays = PlanSchedule.buildDays(trip.startDate, trip.endDate, plan),
        budget = BudgetSummary(150000, 11500, 138500, 7, BudgetStatus.ON_TRACK, 8, 17312, plannedOpenMinorUnits = 13500),
        dayGroups = listOf(
            ExpenseDayGroup(start, 11500, listOf(ExpenseItemUi(dinner, "Anna", 3), ExpenseItemUi(taxi, "Nico", 2))),
        ),
        settlement = settlement,
        suggestions = settlement.suggestions.map {
            TransferUi(it, names.getValue(it.fromTravelerId), names.getValue(it.toTravelerId))
        },
    )
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ExpensesTabPreview() {
    TripBudgetTheme {
        TripDetailContent(previewState(), TripTab.EXPENSES, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SettlementTabPreview() {
    TripBudgetTheme {
        TripDetailContent(previewState(), TripTab.SETTLEMENT, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun PlanTabPreview() {
    TripBudgetTheme {
        TripDetailContent(previewState(), TripTab.PLAN, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
