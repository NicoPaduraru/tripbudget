package de.tripbudget.domain.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/*
 * Formatiert Beträge für die Anzeige.
 * Deutsch:  1234 (EUR) → "12,34 €"     Englisch: 1234 (EUR) → "€12.34"
 *
 * NEU IN PHASE 1: "minorUnits" statt "cents".
 * Nicht jede Währung hat Cent: Der japanische Yen (JPY) hat 0 Nachkommastellen.
 * Wir speichern deshalb immer die KLEINSTE Einheit der Währung:
 *   EUR: 1234 = 12,34 €      JPY: 1234 = 1.234 ¥
 */
object MoneyFormatter {
    fun format(minorUnits: Long, currencyCode: String = "EUR", locale: Locale = Locale.GERMANY): String {
        val currency = Currency.getInstance(currencyCode)
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = currency
        val digits = fractionDigitsOf(currencyCode)
        formatter.minimumFractionDigits = digits
        formatter.maximumFractionDigits = digits
        val factor = Math.pow(10.0, digits.toDouble())
        return formatter.format(minorUnits / factor) // Double nur für die Anzeige, nie zum Rechnen
    }

    /**
     * Anzahl Nachkommastellen einer Währung (EUR → 2, JPY → 0).
     * Liefert 2, falls Java die Währung nicht kennt oder -1 meldet (z. B. bei Gold "XAU").
     */
    fun fractionDigitsOf(currencyCode: String): Int =
        try {
            Currency.getInstance(currencyCode).defaultFractionDigits.takeIf { it >= 0 } ?: 2
        } catch (e: IllegalArgumentException) {
            2
        }

    /**
     * NEU IN PHASE 4: Betrag als Text für ein Eingabefeld, ohne Währungszeichen und
     * ohne Tausenderpunkt, damit MoneyParser ihn wieder lesen kann.
     *   4550 (EUR, Deutsch)  → "45,50"
     *   4550 (EUR, Englisch) → "45.50"
     *   3000 (JPY)           → "3000"
     */
    fun toInputText(minorUnits: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val digits = fractionDigitsOf(currencyCode)
        if (digits == 0) return minorUnits.toString()
        var factor = 1L
        repeat(digits) { factor *= 10 }
        val separator = java.text.DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val whole = minorUnits / factor
        val fraction = (minorUnits % factor).toString().padStart(digits, '0')
        return "$whole$separator$fraction"
    }
}
