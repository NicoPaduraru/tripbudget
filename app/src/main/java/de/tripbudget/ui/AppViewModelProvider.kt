package de.tripbudget.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.tripbudget.TripBudgetApplication
import de.tripbudget.ui.addtrip.AddTripViewModel
import de.tripbudget.ui.triplist.TripListViewModel

/*
 * Android erzeugt ViewModels selbst (damit sie z. B. das Drehen des Bildschirms
 * überleben). Unsere ViewModels brauchen aber ein Repository im Konstruktor.
 * Diese "Factory" sagt Android, WIE jedes ViewModel gebaut wird.
 *
 * Benutzung im Bildschirm:  viewModel(factory = AppViewModelProvider.Factory)
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            TripListViewModel(tripBudgetApplication().container.tripRepository)
        }
        initializer {
            AddTripViewModel(tripBudgetApplication().container.tripRepository)
        }
    }
}

/** Holt unsere Application-Klasse (und damit den AppContainer) aus den CreationExtras. */
fun CreationExtras.tripBudgetApplication(): TripBudgetApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TripBudgetApplication
