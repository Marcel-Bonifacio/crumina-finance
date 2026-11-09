package id.tirtawijata.crumina

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import id.tirtawijata.crumina.auth.GoogleSignIn
import androidx.compose.runtime.mutableStateOf
import id.tirtawijata.crumina.auth.AuthManager
import id.tirtawijata.crumina.data.Repo
import id.tirtawijata.crumina.ui.AppScaffold
import id.tirtawijata.crumina.ui.CruminaTheme
import id.tirtawijata.crumina.ui.LoginScreen

class MainActivity : ComponentActivity() {

    private val loggedIn = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status and navigation bars; screens pad themselves with window insets.
        enableEdgeToEdge()
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        Repo.init(this)
        loggedIn.value = Repo.isLoggedIn
        // Only a fresh launch can carry a new sign-in callback. On recreation (rotation, theme or
        // locale change, restore after process death) it was already consumed, and applying it
        // again would undo a logout.
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            CruminaTheme {
                // Results of GoogleSignIn, which may finish after this Activity was recreated.
                androidx.compose.runtime.LaunchedEffect(GoogleSignIn.result) { GoogleSignIn.result?.let { GoogleSignIn.result = null; applyTokens(it) } }
                androidx.compose.runtime.LaunchedEffect(GoogleSignIn.needsFallback) { if (GoogleSignIn.needsFallback) { GoogleSignIn.needsFallback = false; AuthManager.login(this@MainActivity) } }
                id.tirtawijata.crumina.ui.UpdatePrompt()
                if (loggedIn.value && !Repo.sessionExpired) {
                    AppScaffold(onLogout = { Repo.logout(); loggedIn.value = false })
                } else {
                    LoginScreen(expired = Repo.sessionExpired, onLogin = { startGoogleSignIn() },
                        onLocal = { name -> Repo.startLocal(name); loggedIn.value = true })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        // Reopening from Recents replays the original launch intent; never re-apply its tokens.
        if ((intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return
        val tokens = AuthManager.handleCallback(intent.data) ?: return
        // Consume the token-bearing URI once. The same Intent object is reused when the
        // Activity is relaunched, so clear it in place and keep it as the current intent.
        intent.setData(null)
        setIntent(intent)
        applyTokens(tokens)
    }

    // Google's consent screen for the Gmail scope (launched by GoogleSignIn, result handed back to it).
    private val consentLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        GoogleSignIn.onConsentResult(this, r.data)
    }

    /** Sign in with Google Identity Services; the browser flow is the fallback (no Play services, no account). */
    fun startGoogleSignIn() = GoogleSignIn.start(this, consentLauncher)

    private fun applyTokens(tokens: id.tirtawijata.crumina.auth.AuthTokens) {
        val profileJson = tokens.profile?.let { p ->
            runCatching { String(Base64.decode(p, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)) }.getOrNull()
        }
        // A different Google account signing in (e.g. after the session expired on a shared phone)
        // must not inherit the previous person's accounts, statements or statement passwords.
        val newEmail = profileJson?.let { runCatching { org.json.JSONObject(it).optString("email") }.getOrNull() }
        val oldEmail = Repo.profile?.email
        if (!oldEmail.isNullOrBlank() && !newEmail.isNullOrBlank() && !oldEmail.equals(newEmail, ignoreCase = true)) Repo.logout()
        Repo.setSession(tokens.session, tokens.refreshToken)
        profileJson?.let { Repo.saveProfile(it) }
        loggedIn.value = true
    }
}
