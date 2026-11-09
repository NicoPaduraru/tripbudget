package de.tripbudget.data.security

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

/*
 * NEU IN PHASE 5: Wandelt eine bestehende UNVERSCHLÜSSELTE Datenbank einmalig
 * in eine verschlüsselte um – ohne Datenverlust.
 *
 * Ablauf:
 *   1. Ist tripbudget.db Klartext? (Header beginnt mit "SQLite format 3") Sonst: nichts tun.
 *   2. Neue, leere, VERSCHLÜSSELTE Datei anlegen: tripbudget-encrypting.db
 *   3. Die alte Datei dort "anhängen" (ATTACH … KEY '' = ohne Passwort)
 *   4. sqlcipher_export('main', 'plaintext'): kopiert alle Tabellen + Daten
 *      vom Klartext (plaintext) in die verschlüsselte Datei (main)
 *   5. Alte Datei löschen, neue umbenennen in tripbudget.db
 *
 * Geht unterwegs etwas schief, bleibt die alte Datei unangetastet.
 */
object DatabaseEncryptor {

    private const val TEMP_NAME = "tripbudget-encrypting.db"

    fun encryptIfPlaintext(context: Context, databaseName: String, passphrase: ByteArray) {
        val databaseFile = context.getDatabasePath(databaseName)
        if (!databaseFile.exists()) return // Neuinstallation: Room legt gleich verschlüsselt an

        val header = readHeader(databaseFile)
        if (!SqliteHeader.isPlaintext(header)) return // schon verschlüsselt

        val version = SqliteHeader.userVersion(header).coerceAtLeast(1)
        val tempFile = context.getDatabasePath(TEMP_NAME)
        deleteWithCompanions(tempFile) // Reste eines abgebrochenen Versuchs entfernen

        try {
            // Eine verschlüsselte Datenbank über SQLCipher öffnen (neu und leer).
            // Die Callback-Version = alte Version → die neue Datei bekommt dieselbe user_version.
            val helper = SupportOpenHelperFactory(passphrase.copyOf()).create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(TEMP_NAME)
                    .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit
                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    })
                    .build()
            )
            val db = helper.writableDatabase
            // Android legt in jeder Datenbank die Tabelle android_metadata an. Die alte Datei
            // hat sie auch – ohne dieses DROP meldet sqlcipher_export "table already exists".
            db.execSQL("DROP TABLE IF EXISTS main.android_metadata")
            db.execSQL("ATTACH DATABASE ? AS plaintext KEY ''", arrayOf<Any?>(databaseFile.absolutePath))
            // SELECT, weil sqlcipher_export eine Funktion ist; das Ergebnis brauchen wir nicht
            db.query("SELECT sqlcipher_export('main', 'plaintext')").use { it.moveToFirst() }
            db.execSQL("DETACH DATABASE plaintext")
            helper.close()
        } catch (e: Exception) {
            deleteWithCompanions(tempFile)
            throw IllegalStateException("Die Datenbank konnte nicht verschlüsselt werden", e)
        }

        // Erst jetzt, wenn alles geklappt hat: alte Datei weg, neue an ihre Stelle
        deleteWithCompanions(databaseFile)
        check(tempFile.renameTo(databaseFile)) { "Umbenennen der verschlüsselten Datenbank fehlgeschlagen" }
        deleteWithCompanions(tempFile) // eventuelle Begleitdateien der Temp-Datei
    }

    private fun readHeader(file: File): ByteArray =
        file.inputStream().use { input ->
            val buffer = ByteArray(SqliteHeader.SIZE)
            val read = input.read(buffer)
            if (read <= 0) ByteArray(0) else buffer.copyOf(read)
        }

    /** Eine SQLite-Datenbank besteht oft aus mehreren Dateien (-wal, -shm, -journal). */
    private fun deleteWithCompanions(file: File) {
        listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
            File(file.path + suffix).delete()
        }
    }
}
