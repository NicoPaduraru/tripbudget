package de.tripbudget.domain.repository

import de.tripbudget.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/** NEU IN PHASE 5: Einstellungen lesen und ändern. */
interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setAppLockEnabled(enabled: Boolean)

    suspend fun setBlockScreenshots(enabled: Boolean)
}

/** NEU IN PHASE 5: alles löschen ("Recht auf Löschung", DSGVO). */
interface DataManagementRepository {
    suspend fun deleteAllData()
}
