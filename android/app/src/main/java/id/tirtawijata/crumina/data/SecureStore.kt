package id.tirtawijata.crumina.data

import android.content.Context

/** Keystore-backed storage for session token, Gmail refresh token, and statement passwords. */
class SecureStore(context: Context) {

    private val prefs = encryptedPrefs(context, "crumina_secure")

    var session: String?
        get() = prefs.getString("session", null)
        set(value) = prefs.edit().apply { if (value == null) remove("session") else putString("session", value) }.apply()

    var refreshToken: String?
        get() = prefs.getString("rt", null)
        set(value) = prefs.edit().apply { if (value == null) remove("rt") else putString("rt", value) }.apply()

    var statementPw: String?
        get() = prefs.getString("stmtpw", null)
        set(value) = prefs.edit().apply { if (value == null) remove("stmtpw") else putString("stmtpw", value) }.apply()

    /** One-time nonce for the login in flight; the App Link callback must echo it back. */
    var pendingLoginState: String?
        get() = prefs.getString("login_state", null)
        // commit(), not apply(): the process may be killed while the Custom Tab is open.
        set(value) { prefs.edit().apply { if (value == null) remove("login_state") else putString("login_state", value) }.commit() }

    val isLoggedIn: Boolean get() = session != null

    fun clear() = prefs.edit().clear().apply()
}
