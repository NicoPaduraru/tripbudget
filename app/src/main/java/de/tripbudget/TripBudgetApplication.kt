package de.tripbudget

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/*
 * Die Application-Klasse wird beim Start der App als ALLERERSTES erzeugt,
 * noch vor jedem Bildschirm, und lebt so lange wie die App.
 * Damit Android sie benutzt, steht sie im AndroidManifest.xml:
 *     android:name=".TripBudgetApplication"
 *
 * PHASE 5b: @HiltAndroidApp startet Hilt.
 * Beim Bauen erzeugt Hilt daraus eine Klasse "Hilt_TripBudgetApplication", die den
 * App-weiten Container (SingletonComponent) anlegt. Den AppContainer von Hand
 * brauchen wir deshalb nicht mehr – die Klasse ist jetzt leer.
 */
@HiltAndroidApp
class TripBudgetApplication : Application()
