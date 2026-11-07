package de.tripbudget.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.tripbudget.ui.addtrip.AddTripScreen
import de.tripbudget.ui.triplist.TripListScreen

/*
 * Die Navigation verbindet die Bildschirme ("Ziele" / destinations).
 * Jedes Ziel hat eine "Route", hier einfache Texte.
 *
 * navController.navigate(route) → neuen Bildschirm oben auf den Stapel legen
 * navController.popBackStack()  → obersten Bildschirm entfernen (= zurück)
 *
 * Ausblick: In Phase 2 brauchen wir Routen mit Parametern (die Reise-ID).
 * Dann stellen wir auf "typsichere" Routen mit Klassen um.
 */
object Routes {
    const val TRIP_LIST = "trip_list"
    const val ADD_TRIP = "add_trip"
}

@Composable
fun TripBudgetNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Routes.TRIP_LIST) {
        composable(Routes.TRIP_LIST) {
            TripListScreen(
                onAddTrip = { navController.navigate(Routes.ADD_TRIP) },
            )
        }
        composable(Routes.ADD_TRIP) {
            AddTripScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}
