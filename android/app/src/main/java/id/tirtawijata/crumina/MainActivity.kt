package id.tirtawijata.crumina

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
                if (loggedIn.value && !Repo.sessionExpired) {
                    AppScaffold(onLogout = { Repo.logout(); loggedIn.value = false })
                } else {
                    LoginScreen(expired = Repo.sessionExpired) { AuthManager.login(this) }
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
        Repo.setSession(tokens.session, tokens.refreshToken)
        tokens.profile?.let { p ->
            runCatching {
                String(Base64.decode(p, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
            }.getOrNull()?.let { Repo.saveProfile(it) }
        }
        loggedIn.value = true
    }
}
