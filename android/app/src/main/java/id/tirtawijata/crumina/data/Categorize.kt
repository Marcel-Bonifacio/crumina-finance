package id.tirtawijata.crumina.data

/** Merchant -> spending category, ported from the web app's cat() rules. */
object Categorize {
    private val rules: List<Pair<Regex, String>> = listOf(
        Regex("alfamart|indomaret|alfamidi|superindo|hypermart|transmart|giant|lotte mart|grosir|minimarket|sayur|freshmart|hero supermarket|ranch market", RegexOption.IGNORE_CASE) to "Groceries",
        Regex("kopi|coffee|eatery|resto|restaurant|\\bfood\\b|bakmi|warung|\\bmie\\b|ayam|cafe|kafe|bakery|roti|sate|nasi|padang|geprek|kfc|mcd|mcdonald|burger|pizza|starbucks|janji jiwa|kenangan|chatime|mixue|bakso|soto|dimsum|seafood|grill|dessert|gofood|grabfood|shopeefood", RegexOption.IGNORE_CASE) to "Food & Dining",
        Regex("grab|gojek|gocar|goride|bluebird|blue bird|\\btaxi\\b|\\bmrt\\b|\\bkrl\\b|transjakarta|busway|parkir|parking|spbu|pertamina|shell|bensin|\\bfuel\\b|\\btol\\b|e-?toll|maxim|indrive|damri", RegexOption.IGNORE_CASE) to "Transport",
        Regex("agoda|hotel|traveloka|airbnb|booking\\.com|flight|garuda|lion air|airasia|citilink|villa|resort|pegipegi|tiket\\.com", RegexOption.IGNORE_CASE) to "Travel",
        Regex("apotek|pharmacy|klinik|clinic|hospital|rumah sakit|kimia farma|guardian|watson|century|dokter|halodoc|alodokter|\\bgym\\b|fitness|bpjs|\\blab\\b", RegexOption.IGNORE_CASE) to "Health",
        Regex("cinema|cinepolis|\\bcgv\\b|\\bxxi\\b|karaoke|steam|playstation|nintendo|\\bgame\\b|nonton|tix ?id", RegexOption.IGNORE_CASE) to "Entertainment",
        Regex("google|netflix|spotify|youtube|disney|\\bhbo\\b|\\bxl\\b|telkom|telkomsel|indihome|\\bpln\\b|pdam|listrik|pulsa|internet|asuransi|insurance|subscription|adobe|microsoft|icloud|apple\\.com|canva|openai|chatgpt|\\bbill\\b|subs", RegexOption.IGNORE_CASE) to "Bills & Subs",
        Regex("shopee|tokopedia|tokped|lazada|blibli|zalora|uniqlo|\\bzara\\b|ikea|ace hardware|miniso|sephora|\\bstore\\b|\\bmall\\b|fashion|bukalapak", RegexOption.IGNORE_CASE) to "Shopping"
    )

    fun category(merchant: String?): String {
        val m = merchant ?: return "Other"
        for ((re, cat) in rules) if (re.containsMatchIn(m)) return cat
        return "Other"
    }
}
