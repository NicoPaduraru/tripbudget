package de.tripbudget.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/*
 * NEU IN PHASE 5: Wo kommt das Passwort der Datenbank her?
 *
 * FALSCH wäre: ein festes Passwort im Code ("geheim123"). Jeder, der die APK
 * entpackt, könnte es lesen – die Verschlüsselung wäre wertlos.
 *
 * RICHTIG (zwei Schlüssel):
 *   1. Beim ersten Start erzeugen wir ein ZUFÄLLIGES Datenbank-Passwort (256 Bit).
 *   2. Dieses Passwort verschlüsseln wir mit einem Schlüssel aus dem ANDROID KEYSTORE.
 *      Der Keystore-Schlüssel verlässt nie die geschützte Hardware des Handys –
 *      auch wir als App können ihn nicht auslesen, nur damit ver-/entschlüsseln lassen.
 *   3. Gespeichert wird nur das VERSCHLÜSSELTE Passwort (Datei in noBackupFilesDir).
 *
 * Wer die Dateien vom Handy kopiert, hat damit nichts in der Hand:
 * ohne den Keystore-Schlüssel dieses einen Geräts kein Passwort, ohne Passwort keine Daten.
 */
class DatabasePassphraseStore(context: Context) {

    // noBackupFilesDir: wird nie in Backups übertragen
    private val file = File(context.noBackupFilesDir, "db_passphrase.bin")

    /** Liefert das Datenbank-Passwort; erzeugt es beim allerersten Aufruf. */
    fun getOrCreatePassphrase(): ByteArray {
        if (file.exists()) return decrypt(file.readBytes())
        val passphrase = generatePassphrase()
        file.writeBytes(encrypt(passphrase))
        return passphrase
    }

    /** 32 zufällige Bytes als Hex-Text (64 Zeichen 0-9 und a-f). */
    private fun generatePassphrase(): ByteArray {
        val random = ByteArray(32)
        SecureRandom().nextBytes(random) // kryptografisch sicherer Zufall, nicht kotlin.random!
        return random.joinToString("") { "%02x".format(it) }.toByteArray(Charsets.US_ASCII)
    }

    /** Holt den Schlüssel aus dem Keystore oder legt ihn beim ersten Mal an. */
    private fun keystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    /*
     * AES-GCM: moderne Verschlüsselung, die zusätzlich prüft, ob die Daten verändert wurden.
     * Der "IV" (Initialisierungsvektor) ist bei jeder Verschlüsselung neu und zufällig.
     * Er ist nicht geheim, wir speichern ihn vor dem Geheimtext:
     *   [Länge des IV][IV][verschlüsseltes Passwort]
     */
    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val iv = cipher.iv
        return byteArrayOf(iv.size.toByte()) + iv + cipher.doFinal(plain)
    }

    private fun decrypt(stored: ByteArray): ByteArray {
        val ivLength = stored[0].toInt()
        val iv = stored.copyOfRange(1, 1 + ivLength)
        val encrypted = stored.copyOfRange(1 + ivLength, stored.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "tripbudget_database_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
