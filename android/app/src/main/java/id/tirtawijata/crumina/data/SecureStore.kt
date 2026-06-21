package id.tirtawijata.crumina.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Keystore-backed storage for session token, Gmail refresh token, and statement passwords. */
class SecureStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "crumina_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var session: String?
        get() = prefs.getString("session", null)
        set(value) = prefs.edit().apply { if (value == null) remove("session") else putString("session", value) }.apply()

    var refreshToken: String?
        get() = prefs.getString("rt", null)
        set(value) = prefs.edit().apply { if (value == null) remove("rt") else putString("rt", value) }.apply()

    var statementPw: String?
        get() = prefs.getString("stmtpw", null)
        set(value) = prefs.edit().apply { if (value == null) remove("stmtpw") else putString("stmtpw", value) }.apply()

    val isLoggedIn: Boolean get() = session != null

    fun clear() = prefs.edit().clear().apply()
}
