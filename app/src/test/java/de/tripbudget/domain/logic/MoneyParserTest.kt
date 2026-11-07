package de.tripbudget.domain.logic

import de.tripbudget.domain.util.MoneyParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyParserTest {

    @Test
    fun `ganze Zahl wird in Cent umgerechnet`() {
        assertEquals(150000L, MoneyParser.parseToMinorUnits("1500"))
    }

    @Test
    fun `Komma und Punkt sind beide erlaubt`() {
        assertEquals(150050L, MoneyParser.parseToMinorUnits("1500,50"))
        assertEquals(150050L, MoneyParser.parseToMinorUnits("1500.50"))
    }

    @Test
    fun `eine Nachkommastelle bedeutet Zehntel`() {
        assertEquals(150050L, MoneyParser.parseToMinorUnits("1500,5"))
    }

    @Test
    fun `Leerzeichen am Rand werden ignoriert`() {
        assertEquals(1000L, MoneyParser.parseToMinorUnits("  10 "))
    }

    @Test
    fun `Yen hat keine Nachkommastellen`() {
        assertEquals(1500L, MoneyParser.parseToMinorUnits("1500", fractionDigits = 0))
        assertNull(MoneyParser.parseToMinorUnits("1500,5", fractionDigits = 0))
    }

    @Test
    fun `ungültige Eingaben ergeben null`() {
        val invalid = listOf("", "   ", "abc", "12,345", "1.500,00", "12,", ",50", "-5", "1e3", "1234567890")
        for (text in invalid) {
            assertNull("Eingabe '$text' hätte abgelehnt werden müssen", MoneyParser.parseToMinorUnits(text))
        }
    }

    @Test
    fun `größter erlaubter Betrag`() {
        assertEquals(99_999_999_999L, MoneyParser.parseToMinorUnits("999999999,99"))
    }
}
