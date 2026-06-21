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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.tirtawijata.crumina.data.Repo

@Composable
fun SettingsScreen(onLogout: () -> Unit, onSync: () -> Unit) {
    val r = Repo
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
        Spacer(Modifier.height(20.dp))
        Button(onClick = onSync, enabled = !r.loading) { Text(if (r.loading) r.t("syncing") else r.t("sync")) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onLogout) { Text(r.t("log_out")) }
    }
}
