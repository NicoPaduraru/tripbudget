package de.tripbudget.domain.util

/*
 * Wandelt die Eingabe aus einem Textfeld in die kleinste Währungseinheit um.
 *
 *   "1500"     → 150000   (EUR, 2 Nachkommastellen)
 *   "1500,5"   → 150050
 *   "1500.50"  → 150050   (Punkt und Komma sind beide erlaubt)
 *   "1500"     → 1500     (JPY, 0 Nachkommastellen)
 *
 * Bei ungültiger Eingabe kommt null zurück, z. B.:
 *   ""  "abc"  "12,345" (zu viele Nachkommastellen)  "1.500,00" (Tausenderpunkt)
 *
 * Warum so streng? Eine Eingabe, die wir nicht eindeutig verstehen, lehnen
 * wir lieber ab, als falsch zu raten. Das ist auch ein Sicherheitsprinzip:
 * "Eingaben validieren".
 */
object MoneyParser {

    /** Höchstens 9 Stellen vor dem Komma (bis 999.999.999), damit Long nie überläuft. */
    private const val MAX_WHOLE_DIGITS = 9

    fun parseToMinorUnits(text: String, fractionDigits: Int = 2): Long? {
        val input = text.trim()
        if (input.isEmpty()) return null

        // Position des Dezimaltrennzeichens suchen (Komma oder Punkt), -1 = keins
        val separatorIndex = input.indexOfFirst { it == ',' || it == '.' }
        val wholePart = if (separatorIndex == -1) input else input.substring(0, separatorIndex)
        val fractionPart = if (separatorIndex == -1) "" else input.substring(separatorIndex + 1)

        if (wholePart.isEmpty() || wholePart.length > MAX_WHOLE_DIGITS || !wholePart.isAsciiDigits()) {
            return null
        }
        if (separatorIndex != -1) {
            // Ein Trennzeichen MUSS von 1 bis fractionDigits Ziffern gefolgt werden.
            if (fractionPart.isEmpty() || fractionPart.length > fractionDigits || !fractionPart.isAsciiDigits()) {
                return null
            }
        }

        var factor = 1L
        repeat(fractionDigits) { factor *= 10 }

        // "5" bei 2 Nachkommastellen bedeutet 50 Cent → mit Nullen auffüllen
        val fractionValue = if (fractionPart.isEmpty()) 0L else fractionPart.padEnd(fractionDigits, '0').toLong()
        return wholePart.toLong() * factor + fractionValue
    }

    /** Nur die Ziffern 0–9 (Char.isDigit() würde auch z. B. arabische Ziffern akzeptieren). */
    private fun String.isAsciiDigits(): Boolean = all { it in '0'..'9' }
}
