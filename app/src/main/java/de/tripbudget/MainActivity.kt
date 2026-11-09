package de.tripbudget

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.tripbudget.domain.logic.AppLockPolicy
import de.tripbudget.domain.repository.SettingsRepository
import de.tripbudget.ui.lock.LockScreen
import de.tripbudget.ui.navigation.TripBudgetNavHost
import de.tripbudget.ui.theme.TripBudgetTheme
import javax.inject.Inject
import kotlinx.coroutines.launch

/*
 * Die einzige Activity der App.
 *
 * PHASE 5:
 *   - erbt von FragmentActivity statt ComponentActivity,
 *     weil BiometricPrompt (Fingerabdruck-Dialog) eine FragmentActivity braucht
 *   - zeigt den Sperrbildschirm ÜBER der App, solange sie gesperrt ist
 *   - meldet der App-Sperre, wann die App in den Hintergrund geht und zurückkommt
 *   - setzt FLAG_SECURE, wenn "Screenshots verhindern" eingeschaltet ist
 *
 * PHASE 5b:
 *   @AndroidEntryPoint = "Hilt, diese Activity bekommt Abhängigkeiten von dir."
 *   Das ist auch die Voraussetzung dafür, dass hiltViewModel() in den Bildschirmen funktioniert.
 *
 *   Eine Activity baut Android selbst – wir können ihr keinen Konstruktor mit Parametern geben.
 *   Darum "Field Injection": Hilt füllt die mit @Inject markierten Felder in super.onCreate().
 *   lateinit, weil die Felder davor noch leer sind.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var appLock: AppLockPolicy
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeScreenshotSetting()

        setContent {
            TripBudgetTheme {
                // null = Einstellungen noch nicht geladen
                val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
                val isLocked by appLock.isLocked.collectAsStateWithLifecycle()
                val current = settings

                // App-Sperre aus? Dann gilt die App als entsperrt.
                LaunchedEffect(current?.appLockEnabled) {
                    if (current != null && !current.appLockEnabled) appLock.unlock()
                }

                Box(Modifier.fillMaxSize()) {
                    // Die App bleibt darunter bestehen – so geht die Navigation beim Sperren nicht verloren
                    TripBudgetNavHost()

                    // Solange Einstellungen laden oder die App gesperrt ist: Sperrbildschirm darüber
                    if (current == null || (current.appLockEnabled && isLocked)) {
                        LockScreen(
                            canUnlock = current != null,
                            onUnlock = ::showUnlockPrompt,
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appLock.onForeground(System.currentTimeMillis())
    }

    override fun onStop() {
        super.onStop()
        appLock.onBackground(System.currentTimeMillis())
    }

    /** Zeigt den Android-Dialog für Fingerabdruck, Gesicht oder Geräte-PIN. */
    private fun showUnlockPrompt() {
        // Hat das Handy (noch) eine Displaysperre? Wenn nicht, ist die App-Sperre wirkungslos.
        if (BiometricManager.from(this).canAuthenticate(APP_LOCK_AUTHENTICATORS) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, R.string.lock_unavailable, Toast.LENGTH_LONG).show()
            lifecycleScope.launch { settingsRepository.setAppLockEnabled(false) }
            appLock.unlock()
            return
        }

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            // object : … = eine anonyme Klasse, die nur hier gebraucht wird (wie in Java)
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    appLock.unlock()
                }
                // Abbruch oder Fehler: App bleibt gesperrt, der Knopf "Entsperren" bleibt sichtbar
            },
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.lock_prompt_title))
            .setSubtitle(getString(R.string.lock_prompt_subtitle))
            .setAllowedAuthenticators(APP_LOCK_AUTHENTICATORS)
            .build()
        prompt.authenticate(promptInfo)
    }

    /*
     * FLAG_SECURE: Android verhindert Screenshots und Bildschirmaufnahmen und zeigt in der
     * App-Übersicht nur eine leere Fläche. repeatOnLifecycle: nur beobachten, solange sichtbar.
     */
    private fun observeScreenshotSetting() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.settings.collect { settings ->
                    if (settings.blockScreenshots) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }
    }

    companion object {
        /** Erlaubt: Fingerabdruck/Gesicht ODER Geräte-PIN/-Muster/-Passwort. */
        const val APP_LOCK_AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
