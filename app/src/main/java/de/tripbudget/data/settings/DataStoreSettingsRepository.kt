package de.tripbudget.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.tripbudget.domain.model.AppSettings
import de.tripbudget.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlinx.coroutines.flow.map

/*
 * NEU IN PHASE 5: Einstellungen mit Jetpack DataStore.
 *
 * DataStore ersetzt die alten SharedPreferences:
 *   - arbeitet mit Coroutines und Flow (blockiert nie den Main-Thread)
 *   - Änderungen sind atomar (alles oder nichts)
 *   - Flow: Ändert sich eine Einstellung, merken es alle Bildschirme sofort
 *
 * Für Einstellungen nehmen wir bewusst NICHT die Datenbank: Sie werden gebraucht,
 * BEVOR die verschlüsselte Datenbank geöffnet ist (z. B. "ist die App-Sperre an?").
 */

// Ein "Delegate": erzeugt genau EINEN DataStore pro App (Datei settings.preferences_pb)
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/*
 * PHASE 5b: @Inject constructor – Hilt baut diese Klasse selbst.
 * @ApplicationContext sagt Hilt: "Gib mir den Context der ganzen App"
 * (nicht den einer Activity, der beim Drehen des Bildschirms ungültig würde).
 */
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {

    private val dataStore = context.applicationContext.settingsDataStore

    // Schlüssel = Name + Typ eines Eintrags
    private object Keys {
        val APP_LOCK = booleanPreferencesKey("app_lock_enabled")
        val BLOCK_SCREENSHOTS = booleanPreferencesKey("block_screenshots")
    }

    override val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            appLockEnabled = prefs[Keys.APP_LOCK] ?: false,
            blockScreenshots = prefs[Keys.BLOCK_SCREENSHOTS] ?: false,
        )
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.APP_LOCK] = enabled }
    }

    override suspend fun setBlockScreenshots(enabled: Boolean) {
        dataStore.edit { it[Keys.BLOCK_SCREENSHOTS] = enabled }
    }
}
