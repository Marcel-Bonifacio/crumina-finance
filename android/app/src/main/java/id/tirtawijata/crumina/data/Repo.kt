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
    internal fun secureStore(): SecureStore = secure
    private var ready = false
    // getParameterized, not an anonymous TypeToken subclass: R8 strips the generic
    // signature that the anonymous form relies on, which crashed release builds at startup.
    private val mapType = TypeToken.getParameterized(Map::class.java, String::class.java, String::class.java).type

    // settings
    var mainCcy by mutableStateOf("IDR")
    var lang by mutableStateOf("en")
    var hideAmounts by mutableStateOf(false)
    /** "light" (default), "dark" or "contrast". */
    var theme by mutableStateOf("light")
    /** No Google account: local data only, plus the public features (rates, quotes, PDF upload). */
    var localMode by mutableStateOf(false)
    var displayName by mutableStateOf<String?>(null)

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
    /** Base currency the rates in [fx] are quoted against. */
    var fxBase by mutableStateOf("")

    // statement unlock
    var discoveredBanks by mutableStateOf<List<BankSlot>>(emptyList())
    var statementPasswords by mutableStateOf<Map<String, String>>(emptyMap())

    // budgets + goals
    var budget by mutableStateOf(Budget())
    var goals by mutableStateOf<List<Goal>>(emptyList())

    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    /** Server rejected the session (401). UI sends the user back to sign-in; local data is kept. */
    var sessionExpired by mutableStateOf(false)

    fun init(context: Context) {
        if (ready) return
        store = Store(context.applicationContext)
        secure = SecureStore(context.applicationContext)
        mainCcy = store.mainCcy; lang = store.lang; hideAmounts = store.hideAmounts; theme = store.theme
        localMode = store.localMode; displayName = store.displayName
        accounts = store.accounts; holdings = store.holdings
        manualTx = store.manualTx; uploadedAccounts = store.uploadedAccounts
        store.profileJson?.let { profile = runCatching { Gson().fromJson(it, Profile::class.java) }.getOrNull() }
        statementPasswords = secure.statementPw?.let {
            runCatching { Gson().fromJson<Map<String, String>>(it, mapType) }.getOrNull()
        } ?: emptyMap()
        // Last good rates, so a cold start offline still converts (only when the base matches).
        store.fxJson?.let { j -> runCatching { Gson().fromJson(j, FxResponse::class.java) }.getOrNull()?.let { if (it.base != null && it.rates != null) { fx = it.rates; fxBase = it.base } } }
        budget = store.budget
        goals = store.goals
        ready = true
    }

    val isLoggedIn: Boolean get() = ready && (secure.isLoggedIn || localMode)

    /** Start local mode with an optional name. A guest token for the public API is fetched on refresh. */
    fun startLocal(name: String?) {
        // Data left by an expired Google session belongs to that account, not to whoever starts local mode.
        if (profile?.email != null) logout()
        localMode = true; store.localMode = true
        chooseDisplayName(name)
        sessionExpired = false
    }

    fun chooseDisplayName(name: String?) {
        val n = name?.trim()?.take(40)?.takeIf { it.isNotEmpty() }
        displayName = n; store.displayName = n
    }

    /** Local mode uses the server's guest session (30 days) for quotes and PDF upload; renew it before it lapses. */
    private suspend fun ensureGuestToken(api: Api) {
        val fresh = secure.session != null && System.currentTimeMillis() - store.guestAt < 25L * 864e5.toLong()
        // A fresh-looking token can still be refused (e.g. the server's secret was rotated): check it.
        val refused = fresh && try { api.data(); false } catch (e: retrofit2.HttpException) { e.code() == 401 }
            catch (e: kotlin.coroutines.cancellation.CancellationException) { throw e } catch (e: Exception) { false }
        if (fresh && !refused) return
        quiet { api.guestToken().token }?.let { secure.session = it; store.guestAt = System.currentTimeMillis() }
    }

    /** Start a login: remember a fresh nonce that the callback has to return. */
    fun newLoginState(): String {
        val b = ByteArray(24); java.security.SecureRandom().nextBytes(b)
        val s = b.joinToString("") { "%02x".format(it) }
        secure.pendingLoginState = s
        return s
    }

    /** True only for the callback of a login this app started; the nonce is single-use. */
    fun consumeLoginState(s: String?): Boolean {
        val want = secure.pendingLoginState ?: return false
        val ok = s != null && java.security.MessageDigest.isEqual(want.toByteArray(), s.toByteArray())
        // Clear only on a match: a stray or forged callback must not cancel the login in progress.
        if (ok) secure.pendingLoginState = null
        return ok
    }

    private fun expireSession() {
        secure.session = null
        secure.refreshToken = null
        sessionExpired = true
    }

    fun setSession(token: String, refreshToken: String?) {
        sessionExpired = false
        // Signing in with Google ends local mode; the local data stays and joins the account.
        if (localMode) { localMode = false; store.localMode = false }
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
            val base = mainCcy
            val rates = runCatching { api.fx(base).rates }.getOrNull()
            if (!rates.isNullOrEmpty() && base == mainCcy) { fx = rates; fxBase = base; store.fxJson = Gson().toJson(FxResponse(base, rates)) }
            if (localMode) {
                ensureGuestToken(api) // no mailbox: only rates and quotes below
            } else {
                // Check the session first: a 401 here means sign in again, not "no data".
                val data = try { api.data() } catch (e: retrofit2.HttpException) {
                    if (e.code() == 401) { expireSession(); return }
                    null
                } catch (e: Exception) { null }
                // The app authenticates with a Bearer token, so /api/data only knows the email and returns
                // a profile without the name. Keep the Google name and picture saved at sign-in.
                data?.profile?.let { p ->
                    val old = profile
                    profile = Profile(email = p.email ?: old?.email, name = p.name?.takeIf { it.isNotBlank() } ?: old?.name, picture = p.picture?.takeIf { it.isNotBlank() } ?: old?.picture)
                }
                // Keep what we had when a call fails (timeout, 429, offline) instead of blanking it.
                quiet { api.sync(secure.refreshToken).data?.transactions }?.let { transactions = it }
                quiet { api.statements(secure.refreshToken, pwHeader()).accounts }?.let { stmtAccounts = it.filter { a -> a.error == null } }
            }
            if (holdings.isNotEmpty()) {
                val quotes = holdings.map { it.symbol }.distinct().associateWith { sym -> quiet { api.quote(sym) } }
                // Merge into the current list (it may have changed while quotes loaded), not a snapshot.
                holdings = holdings.map { h -> quotes[h.symbol]?.takeIf { it.price != null }?.let { q -> withQuote(h, q.price!!, q.currency) } ?: h }
                store.holdings = holdings
            }
        } catch (e: kotlin.coroutines.cancellation.CancellationException) { throw e
        } catch (e: Exception) { error = e.message ?: "Sync failed" }
        finally { loading = false }
    }

    /** runCatching that does not swallow coroutine cancellation (e.g. logging out mid-refresh). */
    private inline fun <T> quiet(block: () -> T): T? = try { block() }
        catch (e: kotlin.coroutines.cancellation.CancellationException) { throw e }
        catch (e: Exception) { null }

    /** Yahoo quotes some markets in minor units (GBp pence, ZAc cents, ILA agorot). */
    private fun withQuote(h: Holding, price: Double, ccy: String?): Holding = when (ccy) {
        "GBp", "GBX" -> h.copy(price = price / 100, ccy = "GBP")
        "ZAc" -> h.copy(price = price / 100, ccy = "ZAR")
        "ILA" -> h.copy(price = price / 100, ccy = "ILS")
        else -> h.copy(price = price, ccy = ccy ?: h.ccy)
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
            if (localMode) ensureGuestToken(Net.api(secure))
            val resp = Net.api(secure).upload(UploadReq(pdfBase64, password))
            if (resp.ok && !resp.accounts.isNullOrEmpty()) {
                // Re-uploading the same statement replaces it instead of double-counting.
                val fresh = resp.accounts.filter { it.error == null }
                uploadedAccounts = uploadedAccounts.filterNot { o -> fresh.any { it.institution == o.institution && it.name == o.name } } + fresh
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

    /** Rate for 1 [mainCcy] in [c], or null when unknown (no rates yet, or rates for another base). */
    private fun rate(c: String): Double? = if (fxBase == mainCcy) fx[c]?.takeIf { it > 0.0 && it.isFinite() } else null

    /** True when some balance can't be converted yet; totals then leave it out instead of adding it 1:1. */
    val fxMissing: Boolean
        get() = (accounts.map { it.ccy } + holdings.map { it.ccy } + allStmt.map { it.ccy ?: "IDR" } +
            (if (transactions.isNotEmpty() || manualTx.isNotEmpty()) listOf("IDR") else emptyList()) + manualTx.map { it.ccy })
            .any { it != mainCcy && rate(it) == null }

    /** True when [from] can be shown in the main currency right now. */
    fun canConv(from: String?): Boolean = (from ?: mainCcy) == mainCcy || rate(from!!) != null

    fun conv(amount: Double, from: String?): Double {
        val f = from ?: mainCcy
        if (f == mainCcy) return amount
        // Unknown rate: count nothing rather than treating rupiah as dollars (fxMissing tells the UI).
        val r = rate(f) ?: return 0.0
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
        // A manual credit card is money owed, whatever sign was typed.
        get() = accounts.sumOf { conv(if (it.type == "credit_card") -abs(it.bal) else it.bal, it.ccy) } +
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
            // Feed rows are rendered as IDR; manual entries are stored in their own ccy.
            val toIdr = if (mainCcy == "IDR") 1.0 else rate("IDR") ?: 0.0
            val manual = manualTx.map { Txn(it.date, -abs(conv(it.amount, it.ccy)) * toIdr, it.merchant, "Manual", "spend", null, "manual") }
            return (transactions + stmtTx + manual).sortedByDescending { it.date ?: "" }
        }

    // ---- insights / budgets ----
    val budgetCategories = listOf("Food & Dining", "Groceries", "Transport", "Shopping", "Travel", "Bills & Subs")

    /** Days since [date] (ISO, dd/MM/yyyy or MM/dd/yyyy); Long.MAX_VALUE when it cannot be read. */
    fun daysAgo(date: String?): Long {
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
    fun topMerchants(days: Long): List<Pair<String, Double>> =
        spendWithin(days).filter { !it.merchant.isNullOrBlank() }.groupBy { it.merchant!!.trim() }
            .map { it.key to it.value.sumOf { s -> s.amountMain } }.sortedByDescending { it.second }

    /** First name for the greeting, and the avatar initial (web: "Good evening, Marcel"). */
    val firstName: String? get() = displayName?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: profile?.name?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: profile?.email?.substringBefore("@")?.takeIf { it.isNotBlank() && it != "guest" }
    val initial: String get() = firstName?.take(1)?.uppercase() ?: "C"

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
    fun chooseTheme(t: String) { theme = t; store.theme = t }
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
        fx = emptyMap(); fxBase = ""
        localMode = false; displayName = null
        store.theme = theme // a display preference, not personal data: keep it across sign-out
    }
}
