package id.tirtawijata.crumina.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.tirtawijata.crumina.data.ManualTx
import id.tirtawijata.crumina.data.Ocr
import id.tirtawijata.crumina.data.Repo

@Composable
fun ActivityScreen() {
    val r = Repo
    var showAdd by remember { mutableStateOf(false) }
    val feed = r.feed
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(r.t("activity"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = { showAdd = true }) { Text("+ " + r.t("add_expense")) }
        }
        Spacer(Modifier.height(8.dp))
        if (feed.isEmpty()) {
            Text(r.t("no_tx"), color = Color(0xFF7A8AA0))
        } else {
            LazyColumn(Modifier.fillMaxSize()) { items(feed) { TxRow(it) } }
        }
    }
    if (showAdd) AddExpenseDialog(onDismiss = { showAdd = false })
}

@Composable
private fun AddExpenseDialog(onDismiss: () -> Unit) {
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
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = amount.toDoubleOrNull() != null && merchant.isNotBlank(),
                onClick = {
                    r.addManualTx(ManualTx(amount.toDoubleOrNull() ?: 0.0, merchant.trim(), category, date.trim(), r.mainCcy))
                    onDismiss()
                }
            ) { Text(r.t("save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(r.t("cancel")) } },
        title = { Text(r.t("add_expense")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedButton(onClick = { scanPicker.launch("image/*") }, enabled = !scanning) {
                    Text(if (scanning) r.t("scanning") else r.t("scan_receipt"))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(r.t("amount")) }, singleLine = true)
                OutlinedTextField(value = merchant, onValueChange = { merchant = it }, label = { Text(r.t("merchant")) }, singleLine = true)
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text(r.t("date")) }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Text(r.t("category"), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cats.forEach { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) }) }
                }
            }
        }
    )
}
