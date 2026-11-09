package id.tirtawijata.crumina.data

/**
 * Parses an amount the way people type it in Indonesia and elsewhere:
 * "42.500.000" and "1.234,56" (dot thousands), "1,000,000" and "1,234.56" (comma thousands),
 * "50,5" (comma decimal) and plain "1234.5". Returns null for anything else, including
 * NaN/Infinity/"1e999", which Gson cannot serialize and would crash the save.
 */
object Num {
    private val dotThousands = Regex("^\\d{1,3}(\\.\\d{3})+(,\\d{1,4})?$")
    private val commaThousands = Regex("^\\d{1,3}(,\\d{3})+(\\.\\d{1,4})?$")
    private val commaDecimal = Regex("^\\d+,\\d{1,4}$")
    private val plain = Regex("^\\d+(\\.\\d+)?$")

    /** For share units and per-unit prices: "1.250" is one and a quarter, not 1250. */
    fun parseDecimal(input: String?): Double? {
        val t = (input ?: "").trim().replace(" ", "")
        if (t.count { it == '.' } <= 1 && plain.matches(t.removePrefix("-"))) return t.toDoubleOrNull()?.takeIf { it.isFinite() }
        return parse(input)
    }

    fun parse(input: String?): Double? {
        val t = (input ?: "").trim().removePrefix("Rp").removePrefix("rp").replace(" ", "").replace(" ", "")
        if (t.isEmpty() || t.length > 24) return null
        val neg = t.startsWith("-")
        val s = t.removePrefix("-")
        val v = when {
            dotThousands.matches(s) -> s.replace(".", "").replace(",", ".").toDoubleOrNull()
            commaThousands.matches(s) -> s.replace(",", "").toDoubleOrNull()
            commaDecimal.matches(s) -> s.replace(",", ".").toDoubleOrNull()
            plain.matches(s) -> s.toDoubleOrNull()
            else -> null
        } ?: return null
        if (!v.isFinite()) return null
        return if (neg) -v else v
    }
}
