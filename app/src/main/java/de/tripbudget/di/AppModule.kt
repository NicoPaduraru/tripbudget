package de.tripbudget.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.tripbudget.domain.logic.AppLockPolicy
import java.time.Clock
import javax.inject.Singleton

/*
 * PHASE 5b: Dinge, die zu keiner Schicht so richtig gehören.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /*
     * Die Uhr der App. Repositories brauchen sie für "erstellt am", ViewModels für "heute".
     * Statt überall LocalDate.now() oder System.currentTimeMillis() zu schreiben, bekommen
     * alle dieselbe Clock. In einem Test kann man stattdessen Clock.fixed(...) hereinreichen –
     * dann ist "heute" immer derselbe Tag und der Test liefert immer dasselbe Ergebnis.
     */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    /*
     * AppLockPolicy liegt in der Domain-Schicht und soll dort frei von Hilt-Annotationen bleiben
     * (reines Kotlin, siehe Phase 1). Darum kein @Inject in der Klasse, sondern ein Rezept hier.
     * @Singleton ist hier PFLICHT: Activity und Einstellungen müssen DIESELBE Sperre sehen.
     */
    @Provides
    @Singleton
    fun provideAppLockPolicy(): AppLockPolicy = AppLockPolicy()
}
