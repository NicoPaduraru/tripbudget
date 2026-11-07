package de.tripbudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.tripbudget.ui.navigation.TripBudgetNavHost
import de.tripbudget.ui.theme.TripBudgetTheme

/*
 * Die einzige Activity der App. Mit Jetpack Compose braucht man meist nur eine:
 * Die Bildschirme sind Composable-Funktionen, zwischen denen die Navigation wechselt.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // App zeichnet bis unter Status- und Navigationsleiste
        setContent {
            TripBudgetTheme { // Farben und Schriften aus ui/theme (von Android Studio erzeugt)
                TripBudgetNavHost()
            }
        }
    }
}
