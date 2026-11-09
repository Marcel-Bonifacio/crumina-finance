package id.tirtawijata.crumina.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import id.tirtawijata.crumina.data.Repo
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onLogout: () -> Unit, onSync: () -> Unit) {
    val r = Repo
    val cr = LocalCr.current
    var showUnlock by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    PageHeader(r.t("settings"))

    SectionLabel(r.t("sec_account"))
    CrCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(r.initial, 48)
            Spacer(Modifier.width(14.dp))
            Column {
                Ink(r.displayName ?: r.profile?.name ?: r.firstName ?: "Crumina", 16.sp, FontWeight.Bold)
                Muted(if (r.localMode) r.t("local_mode") else r.profile?.email ?: r.t("google_acct"), 12.sp)
            }
        }
        SettingRow(r.t("display_name"), r.displayName ?: r.firstName ?: r.t("display_name_sub"), onClick = { editName = true }) { Chevron() }
        if (r.localMode) {
            val activity = androidx.compose.ui.platform.LocalContext.current
            SettingRow(r.t("connect_google"), r.t("connect_google_sub"), onClick = { (activity as? id.tirtawijata.crumina.MainActivity)?.startGoogleSignIn() ?: id.tirtawijata.crumina.auth.AuthManager.login(activity) }) { Chevron() }
        }
    }
    Gap()

    SectionLabel(r.t("sec_prefs"))
    CrCard {
        SettingRow(r.t("main_ccy"), r.t("main_ccy_sub"), first = true) {
            Picker(r.mainCcy, listOf("IDR", "USD", "EUR", "SGD", "GBP", "AUD", "JPY", "MYR")) { r.setCcy(it); onSync() }
        }
        SettingRow(r.t("language"), r.t("language_sub")) {
            Picker(if (r.lang == "id") "Bahasa Indonesia" else "English", listOf("English", "Bahasa Indonesia")) { r.changeLang(if (it == "English") "en" else "id") }
        }
        SettingRow(r.t("appearance"), r.t("appearance_sub")) {
            val names = listOf("light" to r.t("th_light"), "dark" to r.t("th_dark"), "contrast" to r.t("th_contrast"))
            Picker(names.first { it.first == r.theme }.second, names.map { it.second }) { picked -> r.chooseTheme(names.first { it.second == picked }.first) }
        }
        SettingRow(r.t("hide_amounts"), r.t("hide_sub")) {
            Switch(checked = r.hideAmounts, onCheckedChange = { r.toggleHide() },
                colors = SwitchDefaults.colors(checkedTrackColor = cr.teal, checkedThumbColor = Color.White))
        }
    }
    Gap()

    SectionLabel(r.t("sec_data"))
    CrCard {
        if (!r.localMode) SettingRow(r.t("statement_unlock"), r.t("stmt_unlock_sub"), first = true, onClick = { showUnlock = true }) { Chevron() }
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val scope = rememberCoroutineScope()
        var checked by remember { mutableStateOf(false) }
        SettingRow(r.t("check_updates"), if (checked && id.tirtawijata.crumina.data.Updater.available == null) r.t("up_to_date") else String.format(r.t("check_updates_sub"), id.tirtawijata.crumina.BuildConfig.VERSION_NAME),
            first = r.localMode, onClick = { scope.launch { id.tirtawijata.crumina.data.Updater.dismissedTag = null; id.tirtawijata.crumina.data.Updater.check(ctx.applicationContext, force = true); checked = true } }) { Chevron() }
        if (!r.localMode) SettingRow(if (r.loading) r.t("syncing") else r.t("sync"), r.t("sync_sub"), onClick = if (r.loading) null else onSync) { Chevron() }
    }
    Spacer(Modifier.height(18.dp))
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(cr.card)
            .border(1.dp, Color(0xFFF5C2CE), RoundedCornerShape(13.dp)).clickable { confirmLogout = true }.padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) { Ink(if (r.localMode) r.t("leave_local") else r.t("log_out"), 14.sp, FontWeight.ExtraBold, color = cr.red) }
    if (showUnlock) StatementUnlockDialog(onDismiss = { showUnlock = false })
    if (editName) {
        var v by remember { mutableStateOf(r.displayName ?: r.firstName ?: "") }
        CrDialog(
            onDismissRequest = { editName = false },
            title = { Text(r.t("display_name")) },
            text = { CrField(v, { v = it.take(40) }, label = { Text(r.t("your_name")) }) },
            confirmButton = { TextButton(onClick = { r.chooseDisplayName(v); editName = false }) { Text(r.t("save")) } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text(r.t("cancel")) } }
        )
    }
    // Logging out wipes this phone's Crumina data, so it is confirmed first (as on the web).
    if (confirmLogout) CrDialog(
        onDismissRequest = { confirmLogout = false },
        title = { Text(r.t("logout_title")) },
        text = { Muted(r.t("logout_body"), 13.sp) },
        confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text(if (r.localMode) r.t("leave_local") else r.t("log_out"), color = cr.red) } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(r.t("cancel")) } }
    )
}

/** Settings row (`.set-row`): title + muted subtitle on the left, control on the right. */
@Composable
private fun SettingRow(title: String, sub: String?, first: Boolean = false, onClick: (() -> Unit)? = null, control: @Composable () -> Unit) {
    val cr = LocalCr.current
    if (!first) Box(Modifier.fillMaxWidth().height(1.dp).background(cr.line))
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Ink(title, 15.sp, FontWeight.Bold)
            if (sub != null) Muted(sub, 12.sp)
        }
        control()
    }
}

@Composable
private fun Chevron() = Ink("›", 22.sp, color = LocalCr.current.muted)

/** Rounded select box (`<select>` on the web) backed by a dropdown menu. */
@Composable
private fun Picker(value: String, options: List<String>, onPick: (String) -> Unit) {
    val cr = LocalCr.current
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(RoundedCornerShape(12.dp)).background(cr.card).border(1.dp, cr.line, RoundedCornerShape(12.dp))
                .clickable { open = true }.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Ink(value, 14.sp, FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Ink("⌄", 14.sp, color = cr.muted)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { open = false; onPick(o) }) }
        }
    }
}

@Composable
private fun StatementUnlockDialog(onDismiss: () -> Unit) {
    val r = Repo
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(true) }
    val inputs = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(Unit) { checking = true; r.discover(); checking = false }
    CrDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { scope.launch { r.refresh() }; onDismiss() }) { Text(r.t("done")) }
        },
        title = { Text(r.t("statement_unlock")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when {
                    checking -> Text(r.t("checking"))
                    r.discoveredBanks.isEmpty() -> Text(r.t("none_found"))
                    else -> r.discoveredBanks.forEach { b ->
                        val key = b.key
                        if (key != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(b.institution ?: key, style = MaterialTheme.typography.titleSmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CrField(
                                    value = inputs[key] ?: "",
                                    onValueChange = { inputs[key] = it },
                                    label = { Text(r.t("password")) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { r.setStatementPassword(key, inputs[key] ?: "") },
                                    enabled = !(inputs[key].isNullOrBlank())
                                ) { Text(if (r.statementPasswords.containsKey(key)) "✓" else r.t("set")) }
                            }
                        }
                    }
                }
            }
        }
    )
}
