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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import id.tirtawijata.crumina.data.Holding
import id.tirtawijata.crumina.data.Repo
import java.util.Locale

@Composable
fun PortfolioScreen() {
    val r = Repo
    val cr = LocalCr.current
    var showAdd by remember { mutableStateOf(false) }
    PageHeader(r.t("portfolio"), String.format(r.t("holdings_n"), r.holdings.size)) {
        PillButton(r.t("q_holdings"), { showAdd = true })
    }
    val value = r.holdings.sumOf { r.conv(it.units * it.price, it.ccy) }
    val cost = r.holdings.sumOf { r.conv(it.units * it.avg, it.ccy) }
    val gain = value - cost
    val pct = if (cost > 0) gain / cost * 100 else 0.0
    HeroCard {
        HeroLabel(r.t("pf_value"))
        HeroValue(r.money(value))
        if (r.holdings.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                (if (gain >= 0) "▲ +" else "▼ ") + r.money(gain, signed = gain < 0) + " (" + (if (pct >= 0) "+" else "") + String.format(Locale.US, "%.1f", pct) + "%) " + r.t("all_time"),
                color = if (gain >= 0) Color(0xFF2FE0C2) else Color(0xFFFF8BA5), fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
    Gap()
    CrCard {
        if (r.holdings.isEmpty()) EmptyText(r.t("no_holdings"))
        r.holdings.forEachIndexed { i, h ->
            val g = if (h.avg > 0.0) (h.price / h.avg - 1.0) * 100.0 else 0.0
            val tag = h.symbol.substringBefore('.').take(2).uppercase()
            ListRow(i == 0, { IconTile(tag, NavyGrad) }, if (h.name.isBlank()) h.symbol else h.name,
                fmtUnits(h.units) + " @ " + r.money(h.avg, h.ccy, false)) {
                Column(horizontalAlignment = Alignment.End) {
                    Ink(r.money(h.units * h.price, h.ccy, false), 14.sp, FontWeight.Bold)
                    Ink((if (g >= 0) "+" else "") + String.format(Locale.US, "%.1f", g) + "%", 11.5.sp, FontWeight.SemiBold, color = if (g >= 0) cr.green else cr.red)
                }
                RemoveX { r.removeHolding(i) }
            }
        }
    }
    if (showAdd) AddHoldingDialog(onDismiss = { showAdd = false }, onSave = { r.addHolding(it); showAdd = false })
}

private fun fmtUnits(u: Double): String = if (u == Math.floor(u)) String.format(Locale.US, "%,.0f", u) else String.format(Locale.US, "%,.4f", u).trimEnd('0')

@Composable
fun AddHoldingDialog(onDismiss: () -> Unit, onSave: (Holding) -> Unit) {
    val r = Repo
    var symbol by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("") }
    var avg by remember { mutableStateOf("") }
    var ccy by remember { mutableStateOf("USD") }
    val valid = symbol.isNotBlank() && units.toDoubleOrNull() != null
    CrDialog(
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
                CrField(value = symbol, onValueChange = { symbol = it }, label = { Text(r.t("symbol")) }, singleLine = true)
                CrField(value = name, onValueChange = { name = it }, label = { Text(r.t("name")) }, singleLine = true)
                CrField(value = units, onValueChange = { units = it }, label = { Text(r.t("units")) }, singleLine = true)
                CrField(value = avg, onValueChange = { avg = it }, label = { Text(r.t("avg_cost")) }, singleLine = true)
                CrField(value = ccy, onValueChange = { ccy = it }, label = { Text(r.t("currency")) }, singleLine = true)
            }
        }
    )
}
