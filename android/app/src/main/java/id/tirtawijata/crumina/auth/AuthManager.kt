package id.tirtawijata.crumina.auth

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import id.tirtawijata.crumina.BuildConfig

data class AuthTokens(val session: String, val profile: String?, val refreshToken: String?)

/**
 * The server is the OAuth client; the app only opens an external Custom Tab to start
 * the flow and then receives the result through a verified App Link. Google requires
 * the external user-agent here, not an embedded WebView.
 */
object AuthManager {

    fun login(context: Context) {
        val url = Uri.parse(BuildConfig.API_BASE + "/api/auth/start?client=android")
        CustomTabsIntent.Builder().build().launchUrl(context, url)
    }

    // https://crumina.tirtawijata.com/auth/android#t=<session>&p=<profile>&rt=<enc_refresh>
    fun handleCallback(uri: Uri?): AuthTokens? {
        val fragment = uri?.fragment ?: return null
        val params = fragment.split("&").mapNotNull { part ->
            val i = part.indexOf('=')
            if (i <= 0) null else part.substring(0, i) to Uri.decode(part.substring(i + 1))
        }.toMap()
        val session = params["t"] ?: return null
        return AuthTokens(session, params["p"], params["rt"])
    }
}
