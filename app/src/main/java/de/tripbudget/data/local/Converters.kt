package de.tripbudget.data.local

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

/*
 * SQLite kennt nur einfache Typen: Zahlen, Text, Binärdaten.
 * TypeConverter übersetzen zwischen Kotlin-Typen und diesen Grundtypen.
 *
 * LocalDate → Long: Tage seit dem 1.1.1970 ("Epoch Day"), z. B. 2027-05-12 → 20950.
 * LocalTime → Int (NEU IN PHASE 4): Minuten seit Mitternacht, z. B. 14:30 → 870.
 * Beide lassen sich so korrekt sortieren (ORDER BY).
 */
class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun localTimeToMinutes(time: LocalTime?): Int? = time?.let { it.hour * 60 + it.minute }

    @TypeConverter
    fun minutesToLocalTime(minutes: Int?): LocalTime? = minutes?.let { LocalTime.of(it / 60, it % 60) }
}
