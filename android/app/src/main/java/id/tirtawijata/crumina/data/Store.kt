package id.tirtawijata.crumina.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Encrypted local store for app data (accounts, holdings, settings, cached profile). */
class Store(context: Context) {
    private val gson = Gson()
    private val accType = TypeToken.getParameterized(List::class.java, Account::class.java).type
    private val holdType = TypeToken.getParameterized(List::class.java, Holding::class.java).type

    private val prefs = encryptedPrefs(context, "crumina_app")

    var fxJson: String?
        get() = prefs.getString("fx", null)
        set(v) { prefs.edit().apply { if (v == null) remove("fx") else putString("fx", v) }.apply() }

    var mainCcy: String
        get() = prefs.getString("ccy", "IDR") ?: "IDR"
        set(v) { prefs.edit().putString("ccy", v).apply() }

    var lang: String
        get() = prefs.getString("lang", "en") ?: "en"
        set(v) { prefs.edit().putString("lang", v).apply() }

    /** Local mode: no Google account; data lives only on this phone. */
    var localMode: Boolean
        get() = prefs.getBoolean("local", false)
        set(v) { prefs.edit().putBoolean("local", v).apply() }
    /** Name the user chose for greetings (overrides the Google first name). */
    var displayName: String?
        get() = prefs.getString("dname", null)
        set(v) { prefs.edit().apply { if (v.isNullOrBlank()) remove("dname") else putString("dname", v) }.apply() }
    var guestAt: Long
        get() = prefs.getLong("guest_at", 0L)
        set(v) { prefs.edit().putLong("guest_at", v).apply() }
    var theme: String
        get() = prefs.getString("theme", "light") ?: "light"
        set(v) { prefs.edit().putString("theme", v).apply() }
    var hideAmounts: Boolean
        get() = prefs.getBoolean("hide", false)
        set(v) { prefs.edit().putBoolean("hide", v).apply() }

    var profileJson: String?
        get() = prefs.getString("profile", null)
        set(v) { prefs.edit().putString("profile", v).apply() }

    var accounts: List<Account>
        get() = prefs.getString("accounts", null)?.let {
            runCatching { gson.fromJson<List<Account>>(it, accType) }.getOrNull()
        } ?: emptyList()
        set(v) { prefs.edit().putString("accounts", gson.toJson(v)).apply() }

    var holdings: List<Holding>
        get() = prefs.getString("holdings", null)?.let {
            runCatching { gson.fromJson<List<Holding>>(it, holdType) }.getOrNull()
        } ?: emptyList()
        set(v) { prefs.edit().putString("holdings", gson.toJson(v)).apply() }

    var budget: Budget
        get() = prefs.getString("budget", null)?.let {
            runCatching { gson.fromJson(it, Budget::class.java) }.getOrNull()
        } ?: Budget()
        set(v) { prefs.edit().putString("budget", gson.toJson(v)).apply() }

    private val goalType = TypeToken.getParameterized(List::class.java, Goal::class.java).type
    var goals: List<Goal>
        get() = prefs.getString("goals", null)?.let {
            runCatching { gson.fromJson<List<Goal>>(it, goalType) }.getOrNull()
        } ?: emptyList()
        set(v) { prefs.edit().putString("goals", gson.toJson(v)).apply() }

    private val manualType = TypeToken.getParameterized(List::class.java, ManualTx::class.java).type
    var manualTx: List<ManualTx>
        get() = prefs.getString("manualtx", null)?.let { runCatching { gson.fromJson<List<ManualTx>>(it, manualType) }.getOrNull() } ?: emptyList()
        set(v) { prefs.edit().putString("manualtx", gson.toJson(v)).apply() }

    private val stmtListType = TypeToken.getParameterized(List::class.java, StmtAccount::class.java).type
    var uploadedAccounts: List<StmtAccount>
        get() = prefs.getString("uploaded", null)?.let { runCatching { gson.fromJson<List<StmtAccount>>(it, stmtListType) }.getOrNull() } ?: emptyList()
        set(v) { prefs.edit().putString("uploaded", gson.toJson(v)).apply() }

    fun clear() = prefs.edit().clear().apply()
}

/**
 * EncryptedSharedPreferences that recovers instead of crash-looping: some devices' keystores lose or
 * corrupt the master key (OEM bugs, restore to a new device), and then every launch would throw.
 * The unreadable file is deleted and recreated, which signs the user out but keeps the app usable.
 */
fun encryptedPrefs(context: Context, name: String): android.content.SharedPreferences {
    fun open() = EncryptedSharedPreferences.create(
        context, name,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    return try { open() } catch (e: Exception) {
        context.deleteSharedPreferences(name)
        open()
    }
}
