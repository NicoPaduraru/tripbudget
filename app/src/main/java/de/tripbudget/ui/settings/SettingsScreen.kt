package de.tripbudget.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import de.tripbudget.MainActivity
import de.tripbudget.R

/*
 * NEU IN PHASE 5: BILDSCHIRM 6 – Einstellungen.
 * Abschnitte: Sicherheit · Sprache · Daten · Datenschutz
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var lockError by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            // ---------- Sicherheit ----------
            SectionHeader(stringResource(R.string.settings_section_security))
            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_app_lock)) },
                supportingContent = {
                    Text(
                        if (lockError) stringResource(R.string.setting_app_lock_unavailable)
                        else stringResource(R.string.setting_app_lock_desc),
                        color = if (lockError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = settings.appLockEnabled,
                        onCheckedChange = { enabled ->
                            // Einschalten nur, wenn das Handy eine Displaysperre hat
                            if (enabled && !canUseAppLock(context)) {
                                lockError = true
                            } else {
                                lockError = false
                                viewModel.setAppLockEnabled(enabled)
                            }
                        },
                    )
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_block_screenshots)) },
                supportingContent = { Text(stringResource(R.string.setting_block_screenshots_desc)) },
                trailingContent = {
                    Switch(checked = settings.blockScreenshots, onCheckedChange = viewModel::setBlockScreenshots)
                },
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ---------- Sprache ----------
            SectionHeader(stringResource(R.string.settings_section_language))
            // Build.VERSION.SDK_INT: Android-Version des Handys. 33 = Android 13 ("TIRAMISU").
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                LanguagePicker(context)
            } else {
                Text(
                    text = stringResource(R.string.language_follows_system),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ---------- Daten ----------
            SectionHeader(stringResource(R.string.settings_section_data))
            OutlinedButton(
                onClick = { confirmDelete = true },
                enabled = deleteState != DeleteState.RUNNING,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(stringResource(R.string.action_delete_all_data))
            }
            if (deleteState == DeleteState.DONE) {
                Text(
                    text = stringResource(R.string.delete_all_done),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ---------- Datenschutz ----------
            SectionHeader(stringResource(R.string.settings_section_privacy))
            Text(
                text = stringResource(R.string.privacy_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_all_title)) },
            text = { Text(stringResource(R.string.delete_all_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteAllData()
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
    )
}

/** Hat das Handy Fingerabdruck, Gesicht oder eine Displaysperre eingerichtet? */
private fun canUseAppLock(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(MainActivity.APP_LOCK_AUTHENTICATORS) ==
        BiometricManager.BIOMETRIC_SUCCESS

/*
 * Sprachwahl über den LocaleManager von Android 13+.
 * @RequiresApi sagt dem Compiler: Diese Funktion nur auf Android 13+ aufrufen.
 * Nach dem Setzen baut Android die Activity automatisch in der neuen Sprache neu auf.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun LanguagePicker(context: Context) {
    val localeManager = remember { context.getSystemService(LocaleManager::class.java) }
    // "" = Systemsprache, sonst "de" oder "en"
    val current = localeManager.applicationLocales.takeIf { !it.isEmpty }?.get(0)?.language ?: ""

    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            "" to stringResource(R.string.language_system),
            "de" to stringResource(R.string.language_german),
            "en" to stringResource(R.string.language_english),
        ).forEach { (tag, label) ->
            FilterChip(
                selected = current == tag,
                onClick = {
                    localeManager.applicationLocales =
                        if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
                },
                label = { Text(label) },
            )
        }
    }
}
