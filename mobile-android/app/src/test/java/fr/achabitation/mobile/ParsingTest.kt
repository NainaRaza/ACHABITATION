package fr.achabitation.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParsingTest {
    @Test
    fun parse_decimal_accepts_french_comma() {
        assertEquals(12.5, decimalOrNull("12,50")!!, 0.0001)
    }

    @Test
    fun parse_decimal_rejects_invalid_input() {
        assertNull(decimalOrNull("abc"))
    }

    @Test
    fun parse_custom_amount_map_accepts_multiple_separators() {
        val parsed = parseCustomAmountMap("Halal=10, Végétarien:4.5")
        assertEquals(10.0, parsed["Halal"]!!, 0.0001)
        assertEquals(4.5, parsed["Végétarien"]!!, 0.0001)
    }
}
