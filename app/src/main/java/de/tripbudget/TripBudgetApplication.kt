package de.tripbudget

import android.app.Application
import de.tripbudget.di.AppContainer

/*
 * Die Application-Klasse wird beim Start der App als ALLERERSTES erzeugt,
 * noch vor jedem Bildschirm, und lebt so lange wie die App.
 * Damit Android sie benutzt, steht sie im AndroidManifest.xml:
 *     android:name=".TripBudgetApplication"
 */
class TripBudgetApplication : Application() {

    // "lateinit": wird nicht im Konstruktor, sondern in onCreate() gesetzt.
    lateinit var container: AppContainer
        private set // von außen nur lesbar

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
