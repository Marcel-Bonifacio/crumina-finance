package id.tirtawijata.crumina.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.tirtawijata.crumina.data.Repo

@Composable
fun OverviewScreen(onSync: () -> Unit) {
    val r = Repo
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(r.t("net_worth"), style = MaterialTheme.typography.labelLarge, color = Color(0xFF7A8AA0))
        Text(r.money(r.netWorth), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(r.t("cash"), r.money(r.cashTotal))
            StatCard(r.t("investments"), r.money(r.investTotal))
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSync, enabled = !r.loading) {
            Text(if (r.loading) r.t("syncing") else r.t("sync"))
        }
        r.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color(0xFFE0476A), style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        InsightsSection()

        Spacer(Modifier.height(20.dp))
        Divider()
        Spacer(Modifier.height(20.dp))
        WhereItGoes()

        Spacer(Modifier.height(20.dp))
        Divider()
        Spacer(Modifier.height(20.dp))
        BudgetsSection()

        Spacer(Modifier.height(20.dp))
        Divider()
        Spacer(Modifier.height(20.dp))
        Text(r.t("recent"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        val feed = r.feed.take(12)
        if (feed.isEmpty()) Text(r.t("no_tx"), color = Color(0xFF7A8AA0))
        else feed.forEach { TxRow(it) }
    }
}
