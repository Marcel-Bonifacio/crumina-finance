package id.tirtawijata.crumina.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/** Single source of truth: Compose-observable state + network + on-device persistence. */
object Repo {
    private lateinit var store: Store
    private lateinit var secure: SecureStore
    private var ready = false
    private val mapType = object : TypeToken<Map<String, String>>() {}.type

    // settings
    var mainCcy by mutableStateOf("IDR")
    var lang by mutableStateOf("en")
    var hideAmounts by mutableStateOf(false)

    // local data
    var accounts by mutableStateOf<List<Account>>(emptyList())
    var holdings by mutableStateOf<List<Holding>>(emptyList())
    var manualTx by mutableStateOf<List<ManualTx>>(emptyList())
    var uploadedAccounts by mutableStateOf<List<StmtAccount>>(emptyList())

    // server-derived
    var profile by mutableStateOf<Profile?>(null)
    var stmtAccounts by mutableStateOf<List<StmtAccount>>(emptyList())
    var transactions by mutableStateOf<List<Txn>>(emptyList())
    var fx by mutableStateOf<Map<String, Double>>(emptyMap())

    // statement unlock
    var discoveredBanks by mutableStateOf<List<BankSlot>>(emptyList())
    var statementPasswords by mutableStateOf<Map<String, String>>(emptyMap())

    // budgets + goals
    var budget by mutableStateOf(Budget())
    var goals by mutableStateOf<List<Goal>>(emptyList())

    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun init(context: Context) {
        if (ready) return
        store = Store(context.applicationContext)
        secure = SecureStore(context.applicationContext)
        mainCcy = store.mainCcy; lang = store.lang; hideAmounts = store.hideAmounts
        accounts = store.accounts; holdings = store.holdings
        manualTx = store.manualTx; uploadedAccounts = store.uploadedAccounts
        store.profileJson?.let { profile = runCatching { Gson().fromJson(it, Profile::class.java) }.getOrNull() }
        statementPasswords = secure.statementPw?.let {
            runCatching { Gson().fromJson<Map<String, String>>(it, mapType) }.getOrNull()
        } ?: emptyMap()
        budget = store.budget
        goals = store.goals
        ready = true
    }

    val isLoggedIn: Boolean get() = ready && secure.isLoggedIn

    fun setSession(token: String, refreshToken: String?) {
        secure.session = token
        if (refreshToken != null) secure.refreshToken = refreshToken
    }

    fun saveProfile(json: String) {
        store.profileJson = json
        profile = runCatching { Gson().fromJson(json, Profile::class.java) }.getOrNull()
    }

    suspend fun refresh() {
        loading = true; error = null
        try {
            val api = Net.api(secure)
            fx = runCatching { api.fx(mainCcy).rates }.getOrNull() ?: emptyMap()
            runCatching { api.data().profile }.getOrNull()?.let { profile = it }
            transactions = runCatching { api.sync(secure.refreshToken).data?.transactions }.getOrNull() ?: emptyList()
            stmtAccounts = (runCatching { api.statements(secure.refreshToken, pwHeader()).accounts }.getOrNull() ?: emptyList()).filter { it.error == null }
            if (holdings.isNotEmpty()) {
                holdings = holdings.map { h ->
                    val q = runCatching { api.quote(h.symbol) }.getOrNull()
                    if (q?.price != null) h.copy(price = q.price, ccy = q.currency ?: h.ccy) else h
                }
                store.holdings = holdings
            }
        } catch (e: Exception) { error = e.message ?: "Sync failed" }
        loading = false
    }

    suspend fun discover() {
        discoveredBanks = runCatching { Net.api(secure).discoverBanks(1, secure.refreshToken).banks }.getOrNull() ?: emptyList()
    }

    fun setStatementPassword(bank: String, pw: String) {
        val m = statementPasswords.toMutableMap()
        if (pw.isBlank()) m.remove(bank) else m[bank] = pw
        statementPasswords = m
        secure.statementPw = if (m.isEmpty()) null else Gson().toJson(m)
    }

    private fun pwHeader(): String? = secure.statementPw?.takeIf { it.isNotBlank() }
        ?.let { android.util.Base64.encodeToString(it.toByteArray(), android.util.Base64.NO_WRAP) }

    suspend fun upload(pdfBase64: String, password: String?) {
        loading = true; error = null
        try {
            val resp = Net.api(secure).upload(UploadReq(pdfBase64, password))
            if (resp.ok && resp.accounts != null) {
                uploadedAccounts = uploadedAccounts + resp.accounts.filter { it.error == null }
                store.uploadedAccounts = uploadedAccounts
            } else {
                error = resp.error ?: resp.note ?: "Could not read PDF"
            }
        } catch (e: Exception) { error = e.message ?: "Upload failed" }
        loading = false
    }

    // ---- money ----
    private val sym = mapOf(
        "IDR" to "Rp", "USD" to "$", "EUR" to "€", "SGD" to "S$", "GBP" to "£",
        "JPY" to "¥", "AUD" to "A$", "MYR" to "RM", "CNY" to "CN¥", "HKD" to "HK$", "THB" to "฿"
    )

    fun conv(amount: Double, from: String?): Double {
        val f = from ?: mainCcy
        if (f == mainCcy) return amount
        val r = fx[f]
        return if (r != null && r != 0.0) amount / r else amount
    }

    fun money(amount: Double, ccy: String? = mainCcy, signed: Boolean = true): String {
        if (hideAmounts) return "••••"
        val c = ccy ?: mainCcy
        val dec = if (c == "IDR" || c == "JPY") 0 else 2
        val nf = NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = dec; maximumFractionDigits = dec }
        val s = sym[c] ?: "$c "
        val sign = if (signed && amount < 0) "−" else ""
        return sign + s + nf.format(abs(amount))
    }

    fun inMain(amount: Double, from: String?, signed: Boolean = true) = money(conv(amount, from), mainCcy, signed)

    // ---- statements (synced + uploaded) ----
    val allStmt: List<StmtAccount> get() = stmtAccounts + uploadedAccounts

    // ---- totals ----
    val cashTotal: Double
        get() = accounts.sumOf { conv(it.bal, it.ccy) } +
            allStmt.filter { it.type in listOf("savings", "bank", "cash", "deposit") }.sumOf { conv(it.balance ?: 0.0, it.ccy ?: "IDR") }
    val investTotal: Double
        get() = holdings.sumOf { conv(it.units * it.price, it.ccy) } +
            allStmt.filter { it.type == "investment" }.sumOf { conv(it.balance ?: 0.0, it.ccy ?: "IDR") }
    private val cardTotal: Double
        get() = allStmt.filter { it.type == "credit_card" }.sumOf { conv(it.balance ?: 0.0, it.ccy ?: "IDR") }
    val netWorth: Double get() = cashTotal + investTotal + cardTotal

    val feed: List<Txn>
        get() {
            val stmtTx = allStmt.flatMap { it.txns ?: emptyList() }
            val manual = manualTx.map { Txn(it.date, -abs(it.amount), it.merchant, "Manual", "spend", null, "manual") }
            return (transactions + stmtTx + manual).sortedByDescending { it.date ?: "" }
        }

    // ---- insights / budgets ----
    val budgetCategories = listOf("Food & Dining", "Groceries", "Transport", "Shopping", "Travel", "Bills & Subs")

    private fun daysAgo(date: String?): Long {
        val d = date?.trim()?.take(10) ?: return Long.MAX_VALUE
        val parsed = runCatching { java.time.LocalDate.parse(d) }.getOrNull()
            ?: runCatching { java.time.LocalDate.parse(d, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrNull()
            ?: runCatching { java.time.LocalDate.parse(d, java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy")) }.getOrNull()
            ?: return Long.MAX_VALUE
        return java.time.temporal.ChronoUnit.DAYS.between(parsed, java.time.LocalDate.now())
    }

    private data class Spend(val category: String, val amountMain: Double, val date: String?, val merchant: String?)

    private fun allSpend(): List<Spend> =
        transactions.filter { it.cls == "spend" || it.cls == "fee" }
            .map { Spend(Categorize.category(it.merchant), conv(abs(it.amount), "IDR"), it.date, it.merchant) } +
        manualTx.map { Spend(it.category, conv(abs(it.amount), it.ccy), it.date, it.merchant) }

    private fun spendWithin(days: Long): List<Spend> = allSpend().filter { daysAgo(it.date) in 0..days }

    fun spentWindow(days: Long): Double = spendWithin(days).sumOf { it.amountMain }
    fun spentInCategory(cat: String, days: Long): Double = spendWithin(days).filter { it.category == cat }.sumOf { it.amountMain }
    fun categoryTotals(days: Long): List<Pair<String, Double>> =
        spendWithin(days).groupBy { it.category }.map { it.key to it.value.sumOf { s -> s.amountMain } }.sortedByDescending { it.second }

    val spent30: Double get() = spentWindow(30)
    val daily30: Double get() = spent30 / 30.0
    val biggest30: Double get() = spendWithin(30).maxOfOrNull { it.amountMain } ?: 0.0
    val count30: Int get() = spendWithin(30).size

    fun recurring(): List<Pair<String, Double>> =
        spendWithin(120).filter { !it.merchant.isNullOrBlank() }
            .groupBy { it.merchant!!.trim() }
            .filter { it.value.size >= 2 }
            .map { (m, l) -> m to l.map { it.amountMain }.average() }
            .sortedByDescending { it.second }

    private val co2Factors = mapOf(
        "Food & Dining" to 0.4, "Groceries" to 0.5, "Transport" to 0.6, "Travel" to 1.1,
        "Shopping" to 0.5, "Bills & Subs" to 0.3, "Health" to 0.3, "Entertainment" to 0.3, "Other" to 0.4
    )
    private val usdPerMain: Double get() = if (mainCcy == "USD") 1.0 else (fx["USD"] ?: 0.0)
    val carbonMonthlyKg: Double get() = categoryTotals(30).sumOf { (cat, amtMain) -> amtMain * usdPerMain * (co2Factors[cat] ?: 0.4) }
    val carbonTreesYear: Double get() = carbonMonthlyKg * 12.0 / 21.77
    val carbonDrivingKm: Double get() = if (carbonMonthlyKg > 0) carbonMonthlyKg / 0.192 else 0.0

    // ---- monthly spend trend (last 6 calendar months) ----
    fun monthlyTrend(): List<Pair<String, Double>> {
        val now = java.time.LocalDate.now()
        val months = (0..5).map { now.minusMonths(it.toLong()).toString().take(7) }.reversed()
        val byMonth = allSpend().filter { (it.date?.length ?: 0) >= 7 }.groupBy { it.date!!.take(7) }
        return months.map { m -> m to (byMonth[m]?.sumOf { it.amountMain } ?: 0.0) }
    }

    // ---- mutations ----
    fun addAccount(a: Account) { accounts = accounts + a; store.accounts = accounts }
    fun removeAccount(index: Int) { accounts = accounts.filterIndexed { i, _ -> i != index }; store.accounts = accounts }
    fun addHolding(h: Holding) { holdings = holdings + h; store.holdings = holdings }
    fun removeHolding(index: Int) { holdings = holdings.filterIndexed { i, _ -> i != index }; store.holdings = holdings }
    fun addManualTx(t: ManualTx) { manualTx = manualTx + t; store.manualTx = manualTx }
    fun removeManualTx(index: Int) { manualTx = manualTx.filterIndexed { i, _ -> i != index }; store.manualTx = manualTx }
    fun clearUploaded() { uploadedAccounts = emptyList(); store.uploadedAccounts = uploadedAccounts }

    fun setCcy(c: String) { mainCcy = c; store.mainCcy = c }
    fun changeLang(l: String) { lang = l; store.lang = l }
    fun toggleHide() { hideAmounts = !hideAmounts; store.hideAmounts = hideAmounts }
    fun saveBudget(b: Budget) { budget = b; store.budget = b }
    fun addGoal(g: Goal) { goals = goals + g; store.goals = goals }
    fun updateGoal(index: Int, g: Goal) { goals = goals.mapIndexed { i, old -> if (i == index) g else old }; store.goals = goals }
    fun removeGoal(index: Int) { goals = goals.filterIndexed { i, _ -> i != index }; store.goals = goals }

    fun t(key: String) = Strings.t(key, lang)

    fun logout() {
        secure.clear(); store.clear()
        accounts = emptyList(); holdings = emptyList(); transactions = emptyList()
        stmtAccounts = emptyList(); profile = null; manualTx = emptyList(); uploadedAccounts = emptyList()
        discoveredBanks = emptyList(); statementPasswords = emptyMap()
        budget = Budget(); goals = emptyList()
        mainCcy = "IDR"; lang = "en"; hideAmounts = false
    }
}
