package id.tirtawijata.crumina.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import id.tirtawijata.crumina.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * In-app updates from GitHub Releases (the app is distributed as a signed APK, not through Play).
 *
 * 1. check(): asks the GitHub API for the latest non-prerelease, at most every few hours.
 * 2. download(): fetches its `crumina-<version>.apk` asset (or the older fixed-name `crumina.apk`) into the app cache.
 * 3. Before handing the file to the system installer, verifies it is this app (same package),
 *    newer (higher versionCode) and signed with the same certificate as the installed copy.
 *    Android enforces the signature match again at install time; checking first gives a clear error.
 * The user still confirms the install: Android does not allow silent installs for sideloaded apps.
 *
 * Uses its own HTTP client so the session token (added by Net's interceptor) never goes to GitHub.
 */
object Updater {
    private const val LATEST = "https://api.github.com/repos/Marcel-Bonifacio/crumina-finance/releases/latest"
    private const val CHECK_EVERY_MS = 6 * 3600_000L
    private const val MAX_APK = 150L * 1024 * 1024

    data class Release(val tag: String, val version: String, val notes: String, val apkUrl: String)

    var available by mutableStateOf<Release?>(null)
    var dismissedTag by mutableStateOf<String?>(null)
    var progress by mutableStateOf<Float?>(null)   // 0..1 while downloading
    var error by mutableStateOf<String?>(null)

    private val http by lazy {
        OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    }

    /** Compares dotted versions numerically ("0.10.0" > "0.9.3"); a suffix after "-" is ignored. */
    fun newer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.trim().removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val a = parts(remote); val b = parts(local)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    suspend fun check(context: Context, force: Boolean = false) {
        val prefs = context.getSharedPreferences("crumina_update", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong("last", 0L) < CHECK_EVERY_MS) return
        cleanup(context)
        var answered = false
        val rel = withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder().url(LATEST).header("Accept", "application/vnd.github+json").build()
                http.newCall(req).execute().use { res ->
                    if (!res.isSuccessful) return@runCatching null
                    answered = true
                    val j = JSONObject(res.body?.string() ?: return@runCatching null)
                    if (j.optBoolean("prerelease") || j.optBoolean("draft")) return@runCatching null
                    val tag = j.optString("tag_name")
                    val assets = j.optJSONArray("assets") ?: return@runCatching null
                    // Prefer the versioned asset (crumina-0.7.0.apk); releases up to 0.6.0 only have crumina.apk.
                    val want = "crumina-" + tag.removePrefix("v") + ".apk"
                    var url: String? = null
                    var legacy: String? = null
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        when (a.optString("name")) {
                            want -> url = a.optString("browser_download_url")
                            "crumina.apk" -> legacy = a.optString("browser_download_url")
                        }
                    }
                    if (url == null) url = legacy
                    // Only ever download from this repository's own release assets.
                    val ok = url?.startsWith("https://github.com/Marcel-Bonifacio/crumina-finance/releases/download/") == true
                    if (tag.isBlank() || !ok) null else Release(tag, tag.removePrefix("v"), j.optString("body").take(600), url!!)
                }
            }.getOrNull()
        }
        if (rel != null || answered) prefs.edit().putLong("last", now).apply() // offline: try again next launch
        available = rel?.takeIf { newer(it.version, BuildConfig.VERSION_NAME) }
    }

    /** Downloads and verifies the update, then opens the system installer. Call from a coroutine. */
    suspend fun downloadAndInstall(context: Context) {
        val rel = available ?: return
        error = null; progress = 0f
        try {
            val cached = cachedFile(context, rel)
            val file = if (cached.exists() && withContext(Dispatchers.IO) { verify(context, cached) } == null) cached
                else withContext(Dispatchers.IO) { download(context, rel) }
            val problem = withContext(Dispatchers.IO) { verify(context, file) }
            if (problem != null) { file.delete(); error = problem; return }
            install(context, file)
        } catch (e: kotlin.coroutines.cancellation.CancellationException) { throw e
        } catch (e: Exception) { error = "Download failed. Check your connection and try again."
        } finally { progress = null }
    }

    private fun cachedFile(context: Context, rel: Release) =
        File(File(context.cacheDir, "updates"), "crumina-${rel.version.replace(Regex("[^0-9A-Za-z.]"), "")}.apk")

    private fun download(context: Context, rel: Release): File {
        File(context.cacheDir, "updates").mkdirs()
        val part = File(cachedFile(context, rel).path + ".part")
        val out = part
        http.newCall(Request.Builder().url(rel.apkUrl).build()).execute().use { res ->
            if (!res.isSuccessful) throw IllegalStateException("http ${res.code}")
            val body = res.body ?: throw IllegalStateException("empty")
            val total = body.contentLength().takeIf { it > 0 }
            if ((total ?: 0) > MAX_APK) throw IllegalStateException("too large")
            body.byteStream().use { input ->
                out.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024); var n = 0L
                    while (true) {
                        val k = input.read(buf); if (k < 0) break
                        n += k; if (n > MAX_APK) throw IllegalStateException("too large")
                        o.write(buf, 0, k)
                        if (total != null) progress = (n.toFloat() / total).coerceIn(0f, 1f)
                    }
                }
            }
        }
        // Only a complete download gets the final name, so a cut-off file is never reused.
        val done = cachedFile(context, rel); done.delete()
        if (!out.renameTo(done)) throw IllegalStateException("rename")
        return done
    }

    /** Null when the APK is a newer build of this app signed with the same key; otherwise a reason. */
    private fun verify(context: Context, apk: File): String? {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        val archive = pm.getPackageArchiveInfo(apk.path, flags) ?: return "The downloaded file is not a valid app package."
        val installed = pm.getPackageInfo(context.packageName, flags)
        // Some Android versions leave signingInfo empty for archives; GET_SIGNATURES still reports the signer.
        @Suppress("DEPRECATION")
        val archiveCerts = certs(archive).ifEmpty { pm.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNATURES)?.let { certs(it) } ?: emptySet() }
        if (archive.packageName != context.packageName) return "The update is for a different app."
        if (versionCode(archive) <= versionCode(installed)) return "The download is not newer than this version."
        val a = archiveCerts; val b = certs(installed)
        if (a.isEmpty() || a != b) return "The update is not signed by Crumina's key, so it was not installed."
        return null
    }

    @Suppress("DEPRECATION")
    private fun versionCode(p: PackageInfo): Long = if (Build.VERSION.SDK_INT >= 28) p.longVersionCode else p.versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun certs(p: PackageInfo): Set<String> {
        val sigs = if (Build.VERSION.SDK_INT >= 28) {
            val si = p.signingInfo ?: return emptySet()
            if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory
        } else p.signatures
        return (sigs ?: emptyArray()).map { s ->
            MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    private fun install(context: Context, apk: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            // One-time permission: "Install unknown apps" for Crumina. The user comes back and taps Update again.
            error = "Allow Crumina to install updates, then tap Update again."
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".updates", apk)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Removes downloaded APKs once they are no longer newer than the running app. */
    private fun cleanup(context: Context) {
        File(context.cacheDir, "updates").listFiles()?.forEach { f ->
            val v = f.name.removePrefix("crumina-").removeSuffix(".apk")
            if (f.name.endsWith(".part") || !newer(v, BuildConfig.VERSION_NAME)) f.delete()
        }
    }
}
