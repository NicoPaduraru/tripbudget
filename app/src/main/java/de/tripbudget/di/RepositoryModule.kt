package de.tripbudget.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.tripbudget.data.repository.OfflineDataManagementRepository
import de.tripbudget.data.repository.OfflineExpenseRepository
import de.tripbudget.data.repository.OfflinePlanRepository
import de.tripbudget.data.repository.OfflineSettlementRepository
import de.tripbudget.data.repository.OfflineTripRepository
import de.tripbudget.data.settings.DataStoreSettingsRepository
import de.tripbudget.domain.repository.DataManagementRepository
import de.tripbudget.domain.repository.ExpenseRepository
import de.tripbudget.domain.repository.PlanRepository
import de.tripbudget.domain.repository.SettingsRepository
import de.tripbudget.domain.repository.SettlementRepository
import de.tripbudget.domain.repository.TripRepository
import javax.inject.Singleton

/*
 * PHASE 5b: Welche Umsetzung gehört zu welchem Interface?
 *
 * Die ViewModels verlangen ein TripRepository (Interface). Hilt weiß aber nicht von selbst,
 * dass damit OfflineTripRepository gemeint ist – es könnte ja mehrere Umsetzungen geben.
 * @Binds sagt genau das: "Wer TripRepository will, bekommt OfflineTripRepository."
 *
 * Warum "abstract" und ohne Funktionskörper? Es gibt nichts zu tun – OfflineTripRepository
 * hat einen @Inject constructor, Hilt kann es also selbst bauen. @Binds ist nur die
 * Zuordnung. Das ist etwas schneller als @Provides, weil Hilt keinen Code ausführen muss.
 *
 * @Singleton: Alle Bildschirme teilen sich dieselbe Repository-Instanz (wie im alten AppContainer).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTripRepository(impl: OfflineTripRepository): TripRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: OfflineExpenseRepository): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindSettlementRepository(impl: OfflineSettlementRepository): SettlementRepository

    @Binds
    @Singleton
    abstract fun bindPlanRepository(impl: OfflinePlanRepository): PlanRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindDataManagementRepository(impl: OfflineDataManagementRepository): DataManagementRepository
}
