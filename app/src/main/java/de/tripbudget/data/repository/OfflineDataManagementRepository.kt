package de.tripbudget.data.repository

import de.tripbudget.data.local.TripBudgetDatabase
import de.tripbudget.domain.repository.DataManagementRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * NEU IN PHASE 5: alle Reisen, Ausgaben, Zahlungen und Pläne löschen.
 *
 * clearAllTables() ist eine "blockierende" Funktion (kein suspend). Sie darf nicht
 * auf dem Main-Thread laufen. withContext(Dispatchers.IO) schickt sie auf einen
 * Hintergrund-Thread, der für Datei- und Datenbankarbeit gedacht ist.
 */
class OfflineDataManagementRepository @Inject constructor(
    private val database: TripBudgetDatabase,
) : DataManagementRepository {

    override suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }
}
