package de.tripbudget.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withResumed
import de.tripbudget.R
import de.tripbudget.ui.theme.TripBudgetTheme

/*
 * NEU IN PHASE 5: Sperrbildschirm.
 * Eine undurchsichtige Fläche (Surface) über der ganzen App. Sie fängt alle
 * Berührungen ab, sodass man die App darunter nicht bedienen kann.
 */
@Composable
fun LockScreen(
    canUnlock: Boolean,
    onUnlock: () -> Unit,
) {
    // Sobald der Sperrbildschirm erscheint und die App im Vordergrund ist: Dialog automatisch öffnen
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(canUnlock) {
        if (canUnlock) lifecycle.withResumed { onUnlock() }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Text(
                text = stringResource(R.string.lock_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = stringResource(R.string.lock_text),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (canUnlock) {
                Button(onClick = onUnlock, modifier = Modifier.padding(top = 32.dp)) {
                    Text(stringResource(R.string.action_unlock))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LockScreenPreview() {
    TripBudgetTheme { LockScreen(canUnlock = true, onUnlock = {}) }
}
