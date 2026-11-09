package de.tripbudget.data.security

import de.tripbudget.domain.logic.AppLockPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** NEU IN PHASE 5: Tests für App-Sperre und Erkennung verschlüsselter Dateien. */
class SecurityLogicTest {

    /* ---------- AppLockPolicy ---------- */

    @Test
    fun `beim Start gesperrt, nach unlock offen`() {
        val policy = AppLockPolicy(timeoutMillis = 60_000)
        assertTrue(policy.isLocked.value)
        policy.unlock()
        assertFalse(policy.isLocked.value)
    }

    @Test
    fun `kurz im Hintergrund sperrt nicht`() {
        val policy = AppLockPolicy(timeoutMillis = 60_000)
        policy.unlock()
        policy.onBackground(nowMillis = 1_000)
        policy.onForeground(nowMillis = 30_000) // 29 Sekunden
        assertFalse(policy.isLocked.value)
    }

    @Test
    fun `lange im Hintergrund sperrt`() {
        val policy = AppLockPolicy(timeoutMillis = 60_000)
        policy.unlock()
        policy.onBackground(nowMillis = 1_000)
        policy.onForeground(nowMillis = 61_000) // genau 60 Sekunden
        assertTrue(policy.isLocked.value)
    }

    @Test
    fun `Vordergrund ohne vorherigen Hintergrund ändert nichts`() {
        val policy = AppLockPolicy(timeoutMillis = 60_000)
        policy.unlock()
        policy.onForeground(nowMillis = 999_999)
        assertFalse(policy.isLocked.value)
    }

    @Test
    fun `jeder Hintergrund-Aufenthalt zählt einzeln`() {
        val policy = AppLockPolicy(timeoutMillis = 60_000)
        policy.unlock()
        policy.onBackground(0); policy.onForeground(40_000)       // 40 s
        policy.onBackground(50_000); policy.onForeground(90_000)  // wieder 40 s
        assertFalse(policy.isLocked.value) // insgesamt 80 s, aber nie 60 s am Stück
    }

    /* ---------- SqliteHeader ---------- */

    private fun header(userVersion: Int): ByteArray {
        val bytes = ByteArray(SqliteHeader.SIZE)
        "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII).copyInto(bytes)
        bytes[60] = (userVersion shr 24).toByte()
        bytes[61] = (userVersion shr 16).toByte()
        bytes[62] = (userVersion shr 8).toByte()
        bytes[63] = userVersion.toByte()
        return bytes
    }

    @Test
    fun `Klartext-Datenbank wird erkannt und Version gelesen`() {
        val h = header(userVersion = 4)
        assertTrue(SqliteHeader.isPlaintext(h))
        assertEquals(4, SqliteHeader.userVersion(h))
    }

    @Test
    fun `große Versionsnummer`() {
        assertEquals(300, SqliteHeader.userVersion(header(300)))
    }

    @Test
    fun `zufällige Bytes gelten als verschlüsselt`() {
        val random = ByteArray(SqliteHeader.SIZE) { (it * 37 + 11).toByte() }
        assertFalse(SqliteHeader.isPlaintext(random))
        assertFalse(SqliteHeader.isPlaintext(ByteArray(0))) // leere Datei
    }
}
