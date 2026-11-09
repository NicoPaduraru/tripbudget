package de.tripbudget.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.tripbudget.ui.addexpense.AddExpenseScreen
import de.tripbudget.ui.addplanitem.AddPlanItemScreen
import de.tripbudget.ui.addtrip.AddTripScreen
import de.tripbudget.ui.settings.SettingsScreen
import de.tripbudget.ui.tripdetail.TripDetailScreen
import de.tripbudget.ui.triplist.TripListScreen

/*
 * Die Navigation verbindet die Bildschirme.
 *
 * Phase 2: Pflicht-Parameter im Pfad:       trip/{tripId}
 * Phase 4: OPTIONALER Parameter als Query:  trip/{tripId}/add_expense?planItemId={planItemId}
 *   - "trip/7/add_expense"                → normale neue Ausgabe (planItemId = -1)
 *   - "trip/7/add_expense?planItemId=3"   → Ausgabe aus Aktivität 3, vorausgefüllt
 * Optionale Parameter brauchen einen Standardwert (defaultValue).
 *
 * ERWEITERUNG 1: "add_trip?tripId=7" öffnet denselben Bildschirm zum Bearbeiten.
 */
object Routes {
    const val ARG_TRIP_ID = "tripId"
    const val ARG_PLAN_ITEM_ID = "planItemId"
    const val NO_PLAN_ITEM = -1L
    const val NO_TRIP = -1L

    const val TRIP_LIST = "trip_list"
    const val ADD_TRIP = "add_trip?$ARG_TRIP_ID={$ARG_TRIP_ID}"
    const val TRIP_DETAIL = "trip/{$ARG_TRIP_ID}"
    const val ADD_EXPENSE = "trip/{$ARG_TRIP_ID}/add_expense?$ARG_PLAN_ITEM_ID={$ARG_PLAN_ITEM_ID}"
    const val ADD_PLAN_ITEM = "trip/{$ARG_TRIP_ID}/add_plan_item"
    const val SETTINGS = "settings"

    fun addTrip() = "add_trip"

    fun editTrip(tripId: Long) = "add_trip?$ARG_TRIP_ID=$tripId"

    fun tripDetail(tripId: Long) = "trip/$tripId"

    fun addExpense(tripId: Long, planItemId: Long? = null) =
        if (planItemId == null) "trip/$tripId/add_expense" else "trip/$tripId/add_expense?$ARG_PLAN_ITEM_ID=$planItemId"

    fun addPlanItem(tripId: Long) = "trip/$tripId/add_plan_item"
}

private val tripIdArgument = navArgument(Routes.ARG_TRIP_ID) { type = NavType.LongType }

/** ERWEITERUNG 1: optionale Reise-ID für "Reise bearbeiten" (-1 = neue Reise). */
private val optionalTripIdArgument = navArgument(Routes.ARG_TRIP_ID) {
    type = NavType.LongType
    defaultValue = Routes.NO_TRIP
}

private val planItemIdArgument = navArgument(Routes.ARG_PLAN_ITEM_ID) {
    type = NavType.LongType
    defaultValue = Routes.NO_PLAN_ITEM // fehlt der Parameter, gilt -1 = "keine Aktivität"
}

@Composable
fun TripBudgetNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Routes.TRIP_LIST) {
        composable(Routes.TRIP_LIST) {
            TripListScreen(
                onAddTrip = { navController.navigate(Routes.addTrip()) },
                onTripClick = { tripId -> navController.navigate(Routes.tripDetail(tripId)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.ADD_TRIP, arguments = listOf(optionalTripIdArgument)) {
            AddTripScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(Routes.TRIP_DETAIL, arguments = listOf(tripIdArgument)) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getLong(Routes.ARG_TRIP_ID) ?: 0L
            TripDetailScreen(
                onBack = { navController.popBackStack() },
                onAddExpense = { navController.navigate(Routes.addExpense(tripId)) },
                onAddPlanItem = { navController.navigate(Routes.addPlanItem(tripId)) },
                onConvertPlanItem = { planItemId -> navController.navigate(Routes.addExpense(tripId, planItemId)) },
                onEditTrip = { navController.navigate(Routes.editTrip(tripId)) },
                // Nach dem Löschen zurück zur Reiseliste (die Reise gibt es ja nicht mehr)
                onTripDeleted = { navController.popBackStack(Routes.TRIP_LIST, inclusive = false) },
            )
        }
        composable(Routes.ADD_EXPENSE, arguments = listOf(tripIdArgument, planItemIdArgument)) {
            AddExpenseScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADD_PLAN_ITEM, arguments = listOf(tripIdArgument)) {
            AddPlanItemScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}
