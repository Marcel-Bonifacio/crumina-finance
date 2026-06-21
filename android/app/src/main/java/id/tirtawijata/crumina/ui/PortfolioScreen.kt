package id.tirtawijata.crumina.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.tirtawijata.crumina.data.Holding
import id.tirtawijata.crumina.data.Repo
import java.util.Locale

@Composable
fun PortfolioScreen() {
    val r = Repo
    var showAdd by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(r.t("portfolio"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = { showAdd = true }) { Text("+ " + r.t("add_holding")) }
        }
        Spacer(Modifier.height(8.dp))
        if (r.holdings.isEmpty()) Text(r.t("no_holdings"), color = Color(0xFF7A8AA0))
        r.holdings.forEachIndexed { i, h ->
            val value = h.units * h.price
            val gain = if (h.avg > 0.0) (h.price / h.avg - 1.0) * 100.0 else 0.0
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (h.name.isBlank()) h.symbol else h.name, style = MaterialTheme.typography.bodyLarge)
                    Text(h.symbol + " · " + h.units.toString() + " @ " + r.money(h.avg, h.ccy, false), style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A8AA0))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(r.money(value, h.ccy, false))
                    Text(
                        (if (gain >= 0) "+" else "") + String.format(Locale.US, "%.1f", gain) + "%",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (gain >= 0) Color(0xFF0FA98E) else Color(0xFFE0476A)
                    )
                }
                IconButton(onClick = { r.removeHolding(i) }) { Icon(Icons.Filled.Delete, contentDescription = r.t("remove")) }
            }
        }
    }
    if (showAdd) AddHoldingDialog(onDismiss = { showAdd = false }, onSave = { r.addHolding(it); showAdd = false })
}

@Composable
private fun AddHoldingDialog(onDismiss: () -> Unit, onSave: (Holding) -> Unit) {
    val r = Repo
    var symbol by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("") }
    var avg by remember { mutableStateOf("") }
    var ccy by remember { mutableStateOf("USD") }
    val valid = symbol.isNotBlank() && units.toDoubleOrNull() != null
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        Holding(
                            symbol.trim().uppercase(),
                            name.trim(),
                            units.toDoubleOrNull() ?: 0.0,
                            avg.toDoubleOrNull() ?: 0.0,
                            0.0,
                            ccy.trim().ifBlank { "USD" }.uppercase()
                        )
                    )
                }
            ) { Text(r.t("save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(r.t("cancel")) } },
        title = { Text(r.t("add_holding")) },
        text = {
            Column {
                OutlinedTextField(value = symbol, onValueChange = { symbol = it }, label = { Text(r.t("symbol")) }, singleLine = true)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(r.t("name")) }, singleLine = true)
                OutlinedTextField(value = units, onValueChange = { units = it }, label = { Text(r.t("units")) }, singleLine = true)
                OutlinedTextField(value = avg, onValueChange = { avg = it }, label = { Text(r.t("avg_cost")) }, singleLine = true)
                OutlinedTextField(value = ccy, onValueChange = { ccy = it }, label = { Text(r.t("currency")) }, singleLine = true)
            }
        }
    )
}
