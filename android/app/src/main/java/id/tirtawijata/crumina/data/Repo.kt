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

    // server-derived
    var profile by mutableStateOf<Profile?>(null)
    var stmtAccounts by mutableStateOf<List<StmtAccount>>(emptyList())
    var transactions by mutableStateOf<List<Txn>>(emptyList())
    var fx by mutableStateOf<Map<String, Double>>(emptyMap())

    // statement unlock
    var discoveredBanks by mutableStateOf<List<BankSlot>>(emptyList())
    var statementPasswords by mutableStateOf<Map<String, String>>(emptyMap())
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
            transactions = runCatching { api.sync().data?.transactions }.getOrNull() ?: emptyList()
            stmtAccounts = (runCatching { api.statements().accounts }.getOrNull() ?: emptyList())
                .filter { it.error == null }
            if (holdings.isNotEmpty()) {
                holdings = holdings.map { h ->
                    val q = runCatching { api.quote(h.symbol) }.getOrNull()
                    if (q?.price != null) h.copy(price = q.price, ccy = q.currency ?: h.ccy) else h
                }
                store.holdings = holdings
            }
        } catch (e: Exception) {
            error = e.message ?: "Sync failed"
        }
        loading = false
    }

    suspend fun discover() {
        discoveredBanks = runCatching { Net.api(secure).discoverBanks(1).banks }.getOrNull() ?: emptyList()
    }

    fun setStatementPassword(bank: String, pw: String) {
        val m = statementPasswords.toMutableMap()
        if (pw.isBlank()) m.remove(bank) else m[bank] = pw
        statementPasswords = m
        secure.statementPw = if (m.isEmpty()) null else Gson().toJson(m)
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
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = dec; maximumFractionDigits = dec
        }
        val s = sym[c] ?: "$c "
        val sign = if (signed && amount < 0) "−" else ""
        return sign + s + nf.format(abs(amount))
    }

    fun inMain(amount: Double, from: String?, signed: Boolean = true) =
        money(conv(amount, from), mainCcy, signed)

    // ---- totals ----
    val cashTotal: Double
        get() = accounts.sumOf { conv(it.bal, it.ccy) } +
            stmtAccounts.filter { it.type in listOf("savings", "bank", "cash", "deposit") }
                .sumOf { conv(it.balance ?: 0.0, it.ccy ?: "IDR") }

    val investTotal: Double
        get() = holdings.sumOf { conv(it.units * it.price, it.ccy) } +
            stmtAccounts.filter { it.type == "investment" }
                .sumOf { conv(it.balance ?: 0.0, it.ccy ?: "IDR") }

    private val cardTotal: Double
        get() = stmtAccounts.filter { it.type == "credit_card" }
            .sumOf { conv(it.balance ?: 0.0, it.ccy ?: "IDR") }

    val netWorth: Double get() = cashTotal + investTotal + cardTotal

    val feed: List<Txn>
        get() {
            val stmtTx = stmtAccounts.flatMap { it.txns ?: emptyList() }
            return (transactions + stmtTx).sortedByDescending { it.date ?: "" }
        }

    // ---- insights / budgets ----
    val budgetCategories = listOf("Food & Dining", "Groceries", "Transport", "Shopping", "Travel", "Bills & Subs")

    private fun daysAgo(date: String?): Long {
        val d = date?.take(10) ?: return Long.MAX_VALUE
        return try {
            java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.parse(d), java.time.LocalDate.now())
        } catch (e: Exception) { Long.MAX_VALUE }
    }

    private fun spendTxWithin(days: Long): List<Txn> =
        transactions.filter { (it.cls == "spend" || it.cls == "fee") && daysAgo(it.date) in 0..days }

    private fun spendWithin(days: Long): List<Pair<String, Double>> =
        spendTxWithin(days).map { Categorize.category(it.merchant) to conv(abs(it.amount), "IDR") }

    fun spentWindow(days: Long): Double = spendWithin(days).sumOf { it.second }
    fun spentInCategory(cat: String, days: Long): Double = spendWithin(days).filter { it.first == cat }.sumOf { it.second }
    fun categoryTotals(days: Long): List<Pair<String, Double>> =
        spendWithin(days).groupBy({ it.first }, { it.second }).map { it.key to it.value.sum() }.sortedByDescending { it.second }

    val spent30: Double get() = spentWindow(30)
    val daily30: Double get() = spent30 / 30.0
    val biggest30: Double get() = spendTxWithin(30).maxOfOrNull { conv(abs(it.amount), "IDR") } ?: 0.0
    val count30: Int get() = spendTxWithin(30).size

    // recurring: a synced merchant seen 2+ times (avg amount in main currency)
    fun recurring(): List<Pair<String, Double>> =
        transactions.filter { (it.cls == "spend" || it.cls == "fee") && !it.merchant.isNullOrBlank() }
            .groupBy { it.merchant!!.trim() }
            .filter { it.value.size >= 2 }
            .map { (m, list) -> m to list.map { conv(abs(it.amount), "IDR") }.average() }
            .sortedByDescending { it.second }

    // carbon: rough kg CO2e per USD spent, by category
    private val co2Factors = mapOf(
        "Food & Dining" to 0.4, "Groceries" to 0.5, "Transport" to 0.6, "Travel" to 1.1,
        "Shopping" to 0.5, "Bills & Subs" to 0.3, "Health" to 0.3, "Entertainment" to 0.3, "Other" to 0.4
    )
    private val usdPerMain: Double get() = if (mainCcy == "USD") 1.0 else (fx["USD"] ?: 0.0)
    val carbonMonthlyKg: Double
        get() = categoryTotals(30).sumOf { (cat, amtMain) -> amtMain * usdPerMain * (co2Factors[cat] ?: 0.4) }
    val carbonTreesYear: Double get() = carbonMonthlyKg * 12.0 / 21.77
    val carbonDrivingKm: Double get() = if (carbonMonthlyKg > 0) carbonMonthlyKg / 0.192 else 0.0

    // ---- mutations ----
    fun addAccount(a: Account) { accounts = accounts + a; store.accounts = accounts }
    fun removeAccount(index: Int) { accounts = accounts.filterIndexed { i, _ -> i != index }; store.accounts = accounts }
    fun addHolding(h: Holding) { holdings = holdings + h; store.holdings = holdings }
    fun removeHolding(index: Int) { holdings = holdings.filterIndexed { i, _ -> i != index }; store.holdings = holdings }

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
        stmtAccounts = emptyList(); profile = null
        discoveredBanks = emptyList(); statementPasswords = emptyMap()
        budget = Budget()
        goals = emptyList()
        mainCcy = "IDR"; lang = "en"; hideAmounts = false
    }
}
