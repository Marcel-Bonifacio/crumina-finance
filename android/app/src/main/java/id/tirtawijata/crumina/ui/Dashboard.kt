package id.tirtawijata.crumina.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.tirtawijata.crumina.data.Budget
import id.tirtawijata.crumina.data.Repo

private val Muted = Color(0xFF7A8AA0)
private val Red = Color(0xFFE0476A)
private val Green = Color(0xFF0FA98E)

@Composable
fun InsightsSection() {
    val r = Repo
    Text(r.t("insights"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(r.t("spent_30"), r.money(r.spent30))
        StatCard(r.t("daily_avg"), r.money(r.daily30))
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(r.t("biggest"), r.money(r.biggest30))
        StatCard(r.t("transactions"), r.count30.toString())
    }
}

@Composable
fun WhereItGoes() {
    val r = Repo
    val cats = r.categoryTotals(30).take(6)
    Text(r.t("where_goes"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    if (cats.isEmpty()) {
        Text(r.t("no_tx"), color = Muted)
    } else {
        val max = cats.maxOf { it.second }.coerceAtLeast(1.0)
        cats.forEach { (cat, amt) ->
            Column(Modifier.padding(vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(cat, style = MaterialTheme.typography.bodyMedium)
                    Text(r.money(amt), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = (amt / max).toFloat().coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun BudgetsSection() {
    val r = Repo
    var showSet by remember { mutableStateOf(false) }
    val days = if (r.budget.period == "weekly") 7L else 30L
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(r.t("budgets"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = { showSet = true }) { Text(r.t("set_budgets")) }
    }
    val hasBudget = r.budget.overall > 0.0 || r.budget.cats.values.any { it > 0.0 }
    if (!hasBudget) {
        Text(r.t("no_budgets"), color = Muted)
    } else {
        if (r.budget.overall > 0.0) BudgetBar(r.t("overall"), r.spentWindow(days), r.budget.overall)
        r.budget.cats.forEach { (cat, lim) -> if (lim > 0.0) BudgetBar(cat, r.spentInCategory(cat, days), lim) }
    }
    if (showSet) BudgetDialog(onDismiss = { showSet = false })
}

@Composable
private fun BudgetBar(label: String, spent: Double, limit: Double) {
    val r = Repo
    val frac = (spent / limit).toFloat().coerceIn(0f, 1f)
    val over = spent > limit
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(r.money(spent) + " / " + r.money(limit), style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(progress = frac, modifier = Modifier.fillMaxWidth(), color = if (over) Red else Green)
        Text(
            if (over) r.t("over") + " " + r.money(spent - limit) else r.money(limit - spent) + " " + r.t("left"),
            style = MaterialTheme.typography.bodySmall,
            color = if (over) Red else Muted
        )
    }
}

@Composable
private fun BudgetDialog(onDismiss: () -> Unit) {
    val r = Repo
    var period by remember { mutableStateOf(r.budget.period) }
    var overall by remember { mutableStateOf(if (r.budget.overall > 0.0) r.budget.overall.toLong().toString() else "") }
    val catInputs = remember {
        mutableStateMapOf<String, String>().apply {
            r.budgetCategories.forEach { c ->
                val v = r.budget.cats[c] ?: 0.0
                put(c, if (v > 0.0) v.toLong().toString() else "")
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val cats = r.budgetCategories.associateWith { (catInputs[it]?.toDoubleOrNull() ?: 0.0) }.filter { it.value > 0.0 }
                r.saveBudget(Budget(period, overall.toDoubleOrNull() ?: 0.0, cats))
                onDismiss()
            }) { Text(r.t("save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(r.t("cancel")) } },
        title = { Text(r.t("set_budgets")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(r.t("period"), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = period == "monthly", onClick = { period = "monthly" }, label = { Text(r.t("monthly")) })
                    FilterChip(selected = period == "weekly", onClick = { period = "weekly" }, label = { Text(r.t("weekly")) })
                }
                OutlinedTextField(value = overall, onValueChange = { overall = it }, label = { Text(r.t("overall")) }, singleLine = true)
                r.budgetCategories.forEach { c ->
                    OutlinedTextField(value = catInputs[c] ?: "", onValueChange = { catInputs[c] = it }, label = { Text(c) }, singleLine = true)
                }
            }
        }
    )
}
