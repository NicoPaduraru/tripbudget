package de.tripbudget.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

/*
 * SQLite kennt nur einfache Typen: Zahlen, Text, Binärdaten.
 * TypeConverter übersetzen zwischen Kotlin-Typen und diesen Grundtypen.
 *
 * LocalDate → Long: Wir speichern die Anzahl Tage seit dem 1.1.1970 ("Epoch Day").
 * 2027-05-12 wird z. B. zu 20950. Vorteil: Zahlen lassen sich korrekt sortieren.
 */
class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let { LocalDate.ofEpochDay(it) }
}
