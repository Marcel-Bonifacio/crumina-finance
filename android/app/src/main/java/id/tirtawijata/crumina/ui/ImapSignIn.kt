package id.tirtawijata.crumina.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tirtawijata.crumina.auth.AuthTokens
import id.tirtawijata.crumina.data.ImapProvider
import id.tirtawijata.crumina.data.Net
import id.tirtawijata.crumina.data.Repo
import kotlinx.coroutines.launch

/** Providers if the server can't be reached for its list (same ids as app/lib/imapProviders.js). */
private val FALLBACK = listOf(
    ImapProvider("gmail", "Gmail", listOf("gmail.com", "googlemail.com"), "https://myaccount.google.com/apppasswords"),
    ImapProvider("icloud", "iCloud Mail", listOf("icloud.com", "me.com", "mac.com"), "https://support.apple.com/102654"),
    ImapProvider("yahoo", "Yahoo Mail", listOf("yahoo.com", "yahoo.co.id", "ymail.com"), "https://help.yahoo.com/kb/SLN15241.html")
)

/**
 * Sign in with an email address and an app password (IMAP), as on the web's /login. No Google OAuth:
 * no "unverified app" screen and no weekly expiry. The server checks the login, then returns the session
 * and the credentials encrypted with its own key; the app keeps those in its encrypted store.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ImapSignInDialog(onDismiss: () -> Unit, onSignedIn: (AuthTokens, String) -> Unit) {
    val r = Repo
    val cr = LocalCr.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var providers by remember { mutableStateOf(FALLBACK) }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") } // chosen from the address, or by the user
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        runCatching { Net.api(r.secureStore()).config() }.getOrNull()?.providers?.takeIf { it.isNotEmpty() }?.let { providers = it }
    }
    // Pick the provider from the address as it's typed (gmail.com -> Gmail).
    val domain = email.substringAfter('@', "").lowercase()
    LaunchedEffect(domain, providers) {
        val hit = providers.firstOrNull { p -> p.domains?.contains(domain) == true }
        // Known domain: pick it. Unknown domain: no silent default, the user picks a provider.
        provider = hit?.id ?: if (domain.contains('.')) "" else provider
    }
    val current = providers.firstOrNull { it.id == provider }

    CrDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(r.t("imap_title")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // Errors go first: the dialog is tall (provider chips, help link), and a message at the
                // bottom would be off-screen on a phone, so a failed sign-in looked like nothing happened.
                err?.let { Ink(it, 13.sp, color = cr.red); Spacer(Modifier.height(10.dp)) }
                Muted(r.t("imap_body"), 13.sp)
                Spacer(Modifier.height(10.dp))
                CrField(email, { email = it.trim().take(254) }, label = { Text(r.t("imap_email")) })
                Spacer(Modifier.height(4.dp))
                Column {
                    Muted(r.t("imap_provider"), 12.sp)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                        providers.forEach { p -> Chip(p.name, provider == p.id) { provider = p.id } }
                    }
                }
                CrField(pass, { pass = it.take(256) }, label = { Text(r.t("imap_password")) }, visualTransformation = PasswordVisualTransformation())
                current?.help?.let { url ->
                    TextButton(onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }) {
                        Text(String.format(r.t("imap_help"), current.name), color = cr.teal, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && email.contains('@') && pass.isNotBlank() && provider.isNotEmpty(), onClick = {
                busy = true; err = null
                scope.launch {
                    val resp = runCatching { Net.api(r.secureStore()).imapLogin(email, pass, provider) }.getOrNull()
                    busy = false
                    when {
                        resp == null -> err = r.t("imap_err_network")
                        resp.ok && resp.t != null && resp.imap != null -> onSignedIn(AuthTokens(resp.t, resp.p, null), resp.imap)
                        resp.error == "auth" -> err = r.t("imap_err_auth")
                        resp.error == "provider" -> err = r.t("imap_err_provider")
                        else -> err = r.t("imap_err_network")
                    }
                }
            }) { Text(if (busy) r.t("signing_in") else r.t("imap_connect")) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(r.t("cancel")) } }
    )
}

