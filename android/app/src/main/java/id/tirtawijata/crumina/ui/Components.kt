package id.tirtawijata.crumina.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.tirtawijata.crumina.data.Repo
import id.tirtawijata.crumina.data.Txn

private val Muted = Color(0xFF7A8AA0)
private val Red = Color(0xFFE0476A)
private val Green = Color(0xFF0FA98E)

@Composable
fun RowScope.StatCard(label: String, value: String) {
    Card(Modifier.weight(1f)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Muted)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun TxRow(t: Txn) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(t.merchant ?: "Transaction", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(listOfNotNull(t.date, t.account).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            Repo.inMain(t.amount, "IDR"),
            style = MaterialTheme.typography.bodyMedium,
            color = if (t.amount < 0) Red else Green
        )
    }
}
