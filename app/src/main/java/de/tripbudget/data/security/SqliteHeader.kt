package de.tripbudget.data.security

/*
 * NEU IN PHASE 5: Wie erkennt man, ob eine Datenbank-Datei verschlüsselt ist?
 *
 * Jede normale SQLite-Datei beginnt mit genau 16 Bytes Text: "SQLite format 3" + Null-Byte.
 * Eine SQLCipher-Datei ist komplett verschlüsselt – auch der Anfang sieht aus wie Zufall.
 *
 * Außerdem steht im Kopf (Header) an Position 60–63 die "user_version":
 * Dort speichert Room die Datenbank-Version (bei uns 4).
 *
 * Reines Kotlin ohne Android → mit Unit-Tests prüfbar.
 */
object SqliteHeader {

    /** Länge des SQLite-Headers in Bytes. */
    const val SIZE = 100

    private val MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    /** true, wenn die Bytes mit dem Klartext-SQLite-Kennzeichen beginnen. */
    fun isPlaintext(header: ByteArray): Boolean =
        header.size >= MAGIC.size && MAGIC.indices.all { header[it] == MAGIC[it] }

    /**
     * Liest die user_version (4 Bytes ab Position 60, "Big Endian" = höchstes Byte zuerst).
     * Nur für Klartext-Dateien sinnvoll.
     */
    fun userVersion(header: ByteArray): Int {
        require(header.size >= 64) { "Header zu kurz" }
        // "and 0xFF" macht aus einem vorzeichenbehafteten Byte (-128..127) eine Zahl 0..255
        return ((header[60].toInt() and 0xFF) shl 24) or
            ((header[61].toInt() and 0xFF) shl 16) or
            ((header[62].toInt() and 0xFF) shl 8) or
            (header[63].toInt() and 0xFF)
    }
}
