package id.tirtawijata.crumina.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Token storage backed by the Android Keystore. The underlying file
 * (crumina_secure.xml) is excluded from cloud backup and device transfer.
 */
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
        get() = prefs.getString(KEY_SESSION, null)
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_SESSION) else putString(KEY_SESSION, value)
        }.apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_RT, null)
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_RT) else putString(KEY_RT, value)
        }.apply()

    val isLoggedIn: Boolean get() = session != null

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_SESSION = "session"
        const val KEY_RT = "rt"
    }
}
