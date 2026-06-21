package id.tirtawijata.crumina.ui

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
    var showUnlock by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(r.t("settings"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        r.profile?.let {
            Text(r.t("signed_in") + ": " + (it.email ?: it.name ?: ""), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
        }
        Text(r.t("currency"), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("IDR", "USD", "EUR", "SGD", "GBP", "AUD", "JPY").forEach { c ->
                FilterChip(selected = r.mainCcy == c, onClick = { r.setCcy(c); onSync() }, label = { Text(c) })
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(r.t("language"), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = r.lang == "en", onClick = { r.changeLang("en") }, label = { Text("EN") })
            FilterChip(selected = r.lang == "id", onClick = { r.changeLang("id") }, label = { Text("ID") })
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(r.t("hide_amounts"))
            Switch(checked = r.hideAmounts, onCheckedChange = { r.toggleHide() })
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { showUnlock = true }) { Text(r.t("statement_unlock")) }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onSync, enabled = !r.loading) { Text(if (r.loading) r.t("syncing") else r.t("sync")) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onLogout) { Text(r.t("log_out")) }
    }
    if (showUnlock) StatementUnlockDialog(onDismiss = { showUnlock = false })
}

@Composable
private fun StatementUnlockDialog(onDismiss: () -> Unit) {
    val r = Repo
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(true) }
    val inputs = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(Unit) { checking = true; r.discover(); checking = false }
    AlertDialog(
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
                                OutlinedTextField(
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
