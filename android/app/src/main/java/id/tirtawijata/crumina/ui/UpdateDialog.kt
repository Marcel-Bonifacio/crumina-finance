package id.tirtawijata.crumina.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tirtawijata.crumina.BuildConfig
import id.tirtawijata.crumina.data.Repo
import id.tirtawijata.crumina.data.Updater
import kotlinx.coroutines.launch

/** Checks GitHub Releases on launch and offers the newer version. Safe to place on any screen. */
@Composable
fun UpdatePrompt() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val r = Repo
    LaunchedEffect(Unit) { Updater.check(ctx.applicationContext) }
    val rel = Updater.available ?: return
    if (Updater.dismissedTag == rel.tag && Updater.progress == null) return
    val cr = LocalCr.current
    CrDialog(
        onDismissRequest = { if (Updater.progress == null) Updater.dismissedTag = rel.tag },
        title = { Text(r.t("upd_title")) },
        text = {
            Column {
                Ink(String.format(r.t("upd_body"), rel.version, BuildConfig.VERSION_NAME), 14.sp)
                if (rel.notes.isNotBlank()) { Spacer(Modifier.height(8.dp)); Muted(rel.notes.lines().take(6).joinToString("\n"), 12.sp) }
                Updater.progress?.let { p -> Spacer(Modifier.height(12.dp)); LinearProgressIndicator(progress = { p }, color = cr.teal, trackColor = cr.line) }
                Updater.error?.let { Spacer(Modifier.height(10.dp)); Ink(it, 13.sp, color = cr.red) }
            }
        },
        confirmButton = {
            TextButton(enabled = Updater.progress == null, onClick = { scope.launch { Updater.downloadAndInstall(ctx) } }) {
                Text(if (Updater.progress != null) r.t("upd_downloading") else r.t("upd_now"))
            }
        },
        dismissButton = {
            TextButton(enabled = Updater.progress == null, onClick = { Updater.dismissedTag = rel.tag }) { Text(r.t("upd_later")) }
        }
    )
}
