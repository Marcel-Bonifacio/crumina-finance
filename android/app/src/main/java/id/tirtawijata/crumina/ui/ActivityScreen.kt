package id.tirtawijata.crumina.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.tirtawijata.crumina.data.ManualTx
import id.tirtawijata.crumina.data.Ocr
import id.tirtawijata.crumina.data.Repo

private const val PAGE = 100

@Composable
fun ActivityScreen() {
    val r = Repo
    val cr = LocalCr.current
    var showAdd by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var range by rememberSaveable { mutableStateOf(30L) }
    PageHeader(r.t("activity"), r.t("act_sub")) {
        PillButton(r.t("q_expense"), { showAdd = true })
    }
    // Search field (`.search`)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cr.card).border(1.dp, cr.line, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = cr.muted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            textStyle = TextStyle(color = cr.ink, fontSize = 14.sp, fontFamily = Jakarta),
            cursorBrush = SolidColor(cr.ink),
            modifier = Modifier.weight(1f).padding(vertical = 14.dp),
            decorationBox = { inner -> Box { if (query.isEmpty()) Muted(r.t("search"), 14.sp); inner() } }
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(7L to "r7", 30L to "r30", 90L to "r90", 365L to "r365", -1L to "rall").forEach { (d, k) ->
            Chip(r.t(k), range == d) { range = d }
        }
    }
    val all = r.feed
    val feed = remember(all, query, range) {
        all.filter { t ->
            (query.isBlank() || (t.merchant ?: t.desc ?: "").contains(query.trim(), ignoreCase = true)) &&
                (range < 0 || r.daysAgo(t.date) <= range)
        }
    }
    // Render in pages: the list lives inside the page scroll, so it is not lazy.
    var shown by remember(query, range) { mutableStateOf(PAGE) }
    CrCard {
        if (feed.isEmpty()) EmptyText(r.t("no_tx"))
        feed.take(shown).forEachIndexed { i, t -> TxRow(t, i == 0) }
        if (feed.size > shown) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GhostButton(String.format(r.t("show_more"), feed.size - shown), { shown += PAGE })
            }
        }
    }
    if (showAdd) AddExpenseDialog(onDismiss = { showAdd = false })
}

@Composable
fun AddExpenseDialog(onDismiss: () -> Unit) {
    val r = Repo
    val ctx = LocalContext.current
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var date by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var scanning by remember { mutableStateOf(false) }
    val cats = listOf("Food & Dining", "Groceries", "Transport", "Travel", "Health", "Entertainment", "Bills & Subs", "Shopping", "Other")
    val scanPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scanning = true
            Ocr.scan(ctx, uri) { amt ->
                scanning = false
                if (amt != null) amount = amt.toLong().toString()
            }
        }
    }
    CrDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = id.tirtawijata.crumina.data.Num.parse(amount) != null && merchant.isNotBlank(),
                onClick = {
                    r.addManualTx(ManualTx(id.tirtawijata.crumina.data.Num.parse(amount) ?: 0.0, merchant.trim(), category, date.trim(), r.mainCcy))
                    onDismiss()
                }
            ) { Text(r.t("save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(r.t("cancel")) } },
        title = { Text(r.t("add_expense")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                GhostButton(if (scanning) r.t("scanning") else r.t("scan_receipt"), { scanPicker.launch("image/*") }, enabled = !scanning)
                Spacer(Modifier.height(8.dp))
                CrField(value = amount, onValueChange = { amount = it }, label = { Text(r.t("amount")) }, singleLine = true)
                CrField(value = merchant, onValueChange = { merchant = it }, label = { Text(r.t("merchant")) }, singleLine = true)
                CrField(value = date, onValueChange = { date = it }, label = { Text(r.t("date")) }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Text(r.t("category"), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cats.forEach { c -> Chip(c, category == c) { category = c } }
                }
            }
        }
    )
}
