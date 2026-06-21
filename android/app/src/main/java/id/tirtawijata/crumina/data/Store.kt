package id.tirtawijata.crumina.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Encrypted local store for app data (accounts, holdings, settings, cached profile). */
class Store(context: Context) {
    private val gson = Gson()
    private val accType = object : TypeToken<List<Account>>() {}.type
    private val holdType = object : TypeToken<List<Holding>>() {}.type

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "crumina_app",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var mainCcy: String
        get() = prefs.getString("ccy", "IDR") ?: "IDR"
        set(v) { prefs.edit().putString("ccy", v).apply() }

    var lang: String
        get() = prefs.getString("lang", "en") ?: "en"
        set(v) { prefs.edit().putString("lang", v).apply() }

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

    private val goalType = object : TypeToken<List<Goal>>() {}.type
    var goals: List<Goal>
        get() = prefs.getString("goals", null)?.let {
            runCatching { gson.fromJson<List<Goal>>(it, goalType) }.getOrNull()
        } ?: emptyList()
        set(v) { prefs.edit().putString("goals", gson.toJson(v)).apply() }

    fun clear() = prefs.edit().clear().apply()
}
