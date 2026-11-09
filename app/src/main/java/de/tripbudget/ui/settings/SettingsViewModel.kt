package de.tripbudget.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import de.tripbudget.domain.model.AppSettings
import de.tripbudget.domain.repository.DataManagementRepository
import de.tripbudget.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DeleteState { IDLE, RUNNING, DONE }

/** NEU IN PHASE 5: Einstellungen. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dataManagementRepository: DataManagementRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    private val _deleteState = MutableStateFlow(DeleteState.IDLE)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAppLockEnabled(enabled) }
    }

    fun setBlockScreenshots(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBlockScreenshots(enabled) }
    }

    fun deleteAllData() {
        if (_deleteState.value == DeleteState.RUNNING) return
        _deleteState.value = DeleteState.RUNNING
        viewModelScope.launch {
            dataManagementRepository.deleteAllData()
            _deleteState.value = DeleteState.DONE
        }
    }
}
