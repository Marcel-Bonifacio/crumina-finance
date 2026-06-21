package id.tirtawijata.crumina

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.tirtawijata.crumina.auth.AuthManager
import id.tirtawijata.crumina.data.Repo
import id.tirtawijata.crumina.ui.AppScaffold
import id.tirtawijata.crumina.ui.CruminaTheme

class MainActivity : ComponentActivity() {

    private val loggedIn = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        Repo.init(this)
        loggedIn.value = Repo.isLoggedIn
        handleIntent(intent)
        setContent {
            CruminaTheme {
                Surface(Modifier.fillMaxSize()) {
                    if (loggedIn.value) {
                        AppScaffold(onLogout = { Repo.logout(); loggedIn.value = false })
                    } else {
                        LoginScreen { AuthManager.login(this) }
                    }
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
        val tokens = AuthManager.handleCallback(intent?.data) ?: return
        Repo.setSession(tokens.session, tokens.refreshToken)
        tokens.profile?.let { p ->
            runCatching {
                String(Base64.decode(p, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
            }.getOrNull()?.let { Repo.saveProfile(it) }
        }
        loggedIn.value = true
    }
}

@Composable
private fun LoginScreen(onLogin: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Crumina", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("Your money, all in one place.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onLogin) { Text("Sign in with Google") }
    }
}
