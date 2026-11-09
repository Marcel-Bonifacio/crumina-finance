package id.tirtawijata.crumina.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import java.util.Locale
import androidx.compose.ui.unit.dp
import id.tirtawijata.crumina.data.Budget
import id.tirtawijata.crumina.data.Repo

@Composable
fun InsightsSection() {
    val r = Repo
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(r.t("spent_30"), r.money(-r.spent30))
        StatTile(r.t("daily_avg"), r.money(r.daily30))
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(r.t("biggest"), r.money(r.biggest30))
        StatTile(r.t("transactions"), r.count30.toString())
    }
    val top = r.topMerchants(30).take(3)
    if (top.isNotEmpty()) {
        Spacer(Modifier.height(14.dp))
        Muted(r.t("top_spending"), 12.sp)
        val max = top.maxOf { it.second }.coerceAtLeast(1.0)
        top.forEach { (m, amt) ->
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                Ink(m, 14.sp, FontWeight.SemiBold, Modifier.weight(1f))
                Ink(r.money(amt), 14.sp)
            }
            Spacer(Modifier.height(6.dp))
            BarBrush((amt / max).toFloat(), Brush.horizontalGradient(listOf(Color(0xFF16C7A8), Color(0xFF16336E))))
        }
    }
}

@Composable
fun WhereItGoes() {
    val r = Repo
    val cats = r.categoryTotals(30).take(6)
    if (cats.isEmpty()) { EmptyText(r.t("no_tx")); return }
    val total = cats.sumOf { it.second }.coerceAtLeast(1.0)
    cats.forEachIndexed { i, (cat, amt) ->
        if (i > 0) Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Ink(cat, 14.sp, modifier = Modifier.weight(1f))
            Muted(String.format(Locale.US, "%.0f%%", amt / total * 100) + " · " + r.money(amt), 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        Bar((amt / total).toFloat(), catColor(cat))
    }
}

@Composable
fun BudgetsCard() {
    val r = Repo
    var showSet by remember { mutableStateOf(false) }
    val days = if (r.budget.period == "weekly") 7L else 30L
    CrCard(title = r.t("budgets"), action = r.t("set_budgets"), onAction = { showSet = true }) {
        val hasBudget = r.budget.overall > 0.0 || r.budget.cats.values.any { it > 0.0 }
        if (!hasBudget) EmptyText(r.t("no_budgets")) else {
            var first = true
            if (r.budget.overall > 0.0) { BudgetBar(r.t("overall"), r.spentWindow(days), r.budget.overall, first); first = false }
            r.budget.cats.forEach { (cat, lim) -> if (lim > 0.0) { BudgetBar(cat, r.spentInCategory(cat, days), lim, first); first = false } }
        }
    }
    if (showSet) BudgetDialog(onDismiss = { showSet = false })
}

@Composable
private fun BudgetBar(label: String, spent: Double, limit: Double, first: Boolean) {
    val r = Repo
    val cr = LocalCr.current
    val over = spent > limit
    if (!first) Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth()) {
        Ink(label, 14.sp, FontWeight.SemiBold, Modifier.weight(1f))
        Muted(r.money(spent) + " / " + r.money(limit), 12.sp)
    }
    Spacer(Modifier.height(6.dp))
    Bar((spent / limit).toFloat(), if (over) cr.red else cr.green)
    Spacer(Modifier.height(4.dp))
    Ink(
        if (over) r.t("over") + " " + r.money(spent - limit) else r.money(limit - spent) + " " + r.t("left"),
        12.sp, color = if (over) cr.red else cr.muted
    )
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
    CrDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val cats = r.budgetCategories.associateWith { id.tirtawijata.crumina.data.Num.parse(catInputs[it]) ?: 0.0 }.filter { it.value > 0.0 }
                r.saveBudget(Budget(period, id.tirtawijata.crumina.data.Num.parse(overall) ?: 0.0, cats))
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
                    Chip(r.t("monthly"), period == "monthly") { period = "monthly" }
                    Chip(r.t("weekly"), period == "weekly") { period = "weekly" }
                }
                CrField(value = overall, onValueChange = { overall = it }, label = { Text(r.t("overall")) }, singleLine = true)
                r.budgetCategories.forEach { c ->
                    CrField(value = catInputs[c] ?: "", onValueChange = { catInputs[c] = it }, label = { Text(c) }, singleLine = true)
                }
            }
        }
    )
}

@Composable
fun RecurringSection() {
    val r = Repo
    val rec = r.recurring().take(8)
    if (rec.isEmpty()) { EmptyText(r.t("no_recurring")); return }
    rec.forEachIndexed { i, (merchant, amt) ->
        ListRow(i == 0, { IconTile(merchant.take(1).uppercase(), NeutralGrad) }, merchant, null) {
            Ink(r.money(amt), 14.sp, FontWeight.Bold)
        }
    }
}

@Composable
fun CarbonSection() {
    val r = Repo
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(r.t("co2_month"), String.format(Locale.US, "%,.0f kg", r.carbonMonthlyKg))
        StatTile(r.t("trees_year"), String.format(Locale.US, "%.1f", r.carbonTreesYear))
    }
    Spacer(Modifier.height(10.dp))
    Muted(r.t("driving") + ": " + String.format(Locale.US, "%,.0f", r.carbonDrivingKm) + " " + r.t("km"), 12.sp)
}

@Composable
fun GoalsCard() {
    val r = Repo
    val cr = LocalCr.current
    var dialog by remember { mutableStateOf<Int?>(null) }
    CrCard(title = r.t("goals"), action = "+ " + r.t("add_goal"), onAction = { dialog = -1 }) {
        if (r.goals.isEmpty()) EmptyText(r.t("no_goals"))
        r.goals.forEachIndexed { i, g ->
            val frac = if (g.target > 0.0) (g.saved / g.target).toFloat() else 0f
            Column(Modifier.fillMaxWidth().clickable { dialog = i }.padding(vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Ink(g.name, 14.sp, FontWeight.SemiBold, Modifier.weight(1f))
                    Muted(r.money(g.saved) + " / " + r.money(g.target), 12.sp)
                }
                Spacer(Modifier.height(6.dp))
                Bar(frac, cr.teal)
                if (g.target > 0.0 && g.saved >= g.target) Ink(r.t("reached"), 12.sp, color = cr.green, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
    dialog?.let { idx -> GoalDialog(idx, onDismiss = { dialog = null }) }
}

@Composable
private fun GoalDialog(index: Int, onDismiss: () -> Unit) {
    val r = Repo
    val existing = if (index >= 0) r.goals.getOrNull(index) else null
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var target by remember { mutableStateOf(existing?.target?.let { if (it > 0.0) it.toLong().toString() else "" } ?: "") }
    var saved by remember { mutableStateOf(existing?.saved?.let { if (it > 0.0) it.toLong().toString() else "" } ?: "") }
    CrDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val g = id.tirtawijata.crumina.data.Goal(name.trim(), id.tirtawijata.crumina.data.Num.parse(target) ?: 0.0, id.tirtawijata.crumina.data.Num.parse(saved) ?: 0.0)
                    if (index >= 0) r.updateGoal(index, g) else r.addGoal(g)
                    onDismiss()
                }
            ) { Text(r.t("save")) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (index >= 0) TextButton(onClick = { r.removeGoal(index); onDismiss() }) { Text(r.t("remove")) }
                TextButton(onClick = onDismiss) { Text(r.t("cancel")) }
            }
        },
        title = { Text(r.t("add_goal")) },
        text = {
            Column {
                CrField(value = name, onValueChange = { name = it }, label = { Text(r.t("goal_name")) }, singleLine = true)
                CrField(value = target, onValueChange = { target = it }, label = { Text(r.t("target")) }, singleLine = true)
                CrField(value = saved, onValueChange = { saved = it }, label = { Text(r.t("saved")) }, singleLine = true)
            }
        }
    )
}

@Composable
fun TrendSection() {
    val r = Repo
    val cr = LocalCr.current
    val data = r.monthlyTrend()
    val max = (data.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        data.forEachIndexed { i, (ym, amt) ->
            val frac = (amt / max).toFloat().coerceIn(0.03f, 1f)
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.weight((1f - frac).coerceAtLeast(0.001f)))
                Box(
                    Modifier.weight(frac).fillMaxWidth(0.7f).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(if (i == data.lastIndex) Brush.verticalGradient(listOf(Color(0xFF16C7A8), Color(0xFF13B97A))) else Brush.verticalGradient(listOf(Color(0xFF2D5BD0), Color(0xFF16336E))))
                )
                Spacer(Modifier.height(6.dp))
                Muted(monthLabel(ym), 11.sp)
            }
        }
    }
}

private fun monthLabel(ym: String): String =
    runCatching { java.time.YearMonth.parse(ym).month.getDisplayName(java.time.format.TextStyle.SHORT, uiLocale()) }.getOrNull() ?: ym.takeLast(2)
