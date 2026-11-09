package de.tripbudget.domain.logic

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * NEU IN PHASE 5: Wann ist die App gesperrt?
 *
 * Regeln:
 *   - Beim Start ist die App gesperrt (falls die App-Sperre eingeschaltet ist).
 *   - War die App mindestens [timeoutMillis] im Hintergrund, wird sie wieder gesperrt.
 *   - Kurze Unterbrechungen (Handy drehen, kurz eine andere App) sperren NICHT.
 *
 * Diese Klasse kennt kein Android – nur Zeitstempel. Deshalb können wir
 * sie mit normalen Unit-Tests prüfen, ohne eine Minute zu warten.
 */
class AppLockPolicy(private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS) {

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var backgroundSinceMillis: Long? = null

    /** Die App verschwindet vom Bildschirm (Activity.onStop). */
    fun onBackground(nowMillis: Long) {
        backgroundSinceMillis = nowMillis
    }

    /** Die App wird wieder sichtbar (Activity.onStart). */
    fun onForeground(nowMillis: Long) {
        val since = backgroundSinceMillis ?: return // erster Start: nichts zu tun
        backgroundSinceMillis = null
        if (nowMillis - since >= timeoutMillis) _isLocked.value = true
    }

    /** Nach erfolgreicher Anmeldung – oder wenn die App-Sperre ausgeschaltet ist. */
    fun unlock() {
        _isLocked.value = false
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 60_000L // 1 Minute
    }
}
