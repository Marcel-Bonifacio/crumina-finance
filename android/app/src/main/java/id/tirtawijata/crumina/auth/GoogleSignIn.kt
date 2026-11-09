package id.tirtawijata.crumina.auth

import android.accounts.Account
import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import id.tirtawijata.crumina.BuildConfig
import id.tirtawijata.crumina.data.AuthCodeReq
import id.tirtawijata.crumina.data.Net
import id.tirtawijata.crumina.data.Repo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Sign in with Google through Google Identity Services, without leaving the app:
 *  1. Credential Manager shows Google's account picker (Sign in with Google).
 *  2. AuthorizationClient asks that account for Gmail read access and a one-time server auth code
 *     (offline access), so the server can read statements and alerts.
 *  3. The server exchanges the code (POST /api/auth/callback {client:"android"}) and returns the same
 *     session / profile / encrypted refresh token as the browser flow.
 * Devices without Google Play services or a Google account fall back to the browser flow (AuthManager.login).
 */
object GoogleSignIn {
    private const val GMAIL = "https://www.googleapis.com/auth/gmail.readonly"

    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    private var pending: ((AuthorizationResult?) -> Unit)? = null

    /** Called by MainActivity with the result of the consent screen it launched for us. */
    fun onConsentResult(activity: Activity, data: Intent?) {
        val cb = pending ?: return
        pending = null
        cb(runCatching { Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data) }.getOrNull())
    }

    /** Returns the tokens, or null when the user cancelled. Throws when GIS is unavailable (caller falls back). */
    suspend fun signIn(activity: Activity, consent: ActivityResultLauncher<IntentSenderRequest>): AuthTokens? {
        busy = true; error = null
        try {
            // 1. Account picker. autoSelect off: the user always sees which account is used.
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
            val cred = try {
                CredentialManager.create(activity).getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build()).credential
            } catch (e: GetCredentialCancellationException) { return null }
            val email = GoogleIdTokenCredential.createFrom(cred.data).id

            // 2. Gmail scope + server auth code for that same account.
            val req = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(GMAIL), Scope("email"), Scope("profile"), Scope("openid")))
                .requestOfflineAccess(BuildConfig.GOOGLE_WEB_CLIENT_ID, true)
                .setAccount(Account(email, "com.google"))
                .build()
            var result: AuthorizationResult? = suspendCancellableCoroutine<AuthorizationResult> { c ->
                Identity.getAuthorizationClient(activity).authorize(req)
                    .addOnSuccessListener { c.resume(it) }
                    .addOnFailureListener { c.resumeWithException(it) }
            }
            if (result?.hasResolution() == true) {
                val pi = result.pendingIntent ?: return null
                result = suspendCancellableCoroutine<AuthorizationResult?> { c ->
                    pending = { r -> c.resume(r) }
                    c.invokeOnCancellation { pending = null }
                    consent.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                }
            }
            val code = result?.serverAuthCode ?: return null // consent declined
            if (result.grantedScopes.none { it == GMAIL }) { error = Repo.t("gmail_needed"); return null }

            // 3. Server exchange.
            val resp = Net.api(Repo.secureStore()).authCode(AuthCodeReq(code))
            if (!resp.ok || resp.t == null) { error = Repo.t(if (resp.error == "gmail_scope_missing") "gmail_needed" else "signin_failed"); return null }
            return AuthTokens(resp.t, resp.p, resp.rt)
        } finally { busy = false }
    }
}
