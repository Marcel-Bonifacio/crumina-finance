package id.tirtawijata.crumina.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.tirtawijata.crumina.data.Account
import id.tirtawijata.crumina.data.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AccountsScreen() {
    val r = Repo
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val b64 = withContext(Dispatchers.IO) {
                ctx.contentResolver.openInputStream(uri)?.readBytes()
                    ?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) }
            }
            if (b64 != null) r.upload(b64, null)
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(r.t("accounts"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = { showAdd = true }) { Text("+ " + r.t("add_account")) }
        }
        Spacer(Modifier.height(8.dp))
        if (r.accounts.isEmpty() && r.allStmt.isEmpty()) {
            Text(r.t("no_accounts"), color = Color(0xFF7A8AA0))
        }
        r.accounts.forEachIndexed { i, a ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(a.name, style = MaterialTheme.typography.bodyLarge)
                    Text(a.type + " · " + a.ccy, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A8AA0))
                }
                Text(r.money(a.bal, a.ccy))
                IconButton(onClick = { r.removeAccount(i) }) { Icon(Icons.Filled.Delete, contentDescription = r.t("remove")) }
            }
        }
        val stmt = r.allStmt
        if (stmt.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(r.t("from_statements"), style = MaterialTheme.typography.titleMedium)
            stmt.forEach { s ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(s.name ?: "Account", style = MaterialTheme.typography.bodyLarge)
                        Text(listOfNotNull(s.type, s.institution).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A8AA0))
                    }
                    Text(r.money(s.balance ?: 0.0, s.ccy ?: "IDR"))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { pdfPicker.launch("application/pdf") }, enabled = !r.loading) {
            Text(if (r.loading) r.t("uploading") else r.t("upload_statement"))
        }
    }
    if (showAdd) AddAccountDialog(onDismiss = { showAdd = false }, onSave = { r.addAccount(it); showAdd = false })
}

@Composable
private fun AddAccountDialog(onDismiss: () -> Unit, onSave: (Account) -> Unit) {
    val r = Repo
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("cash") }
    var ccy by remember { mutableStateOf(r.mainCcy) }
    var bal by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(Account(name.trim(), type.trim().ifBlank { "cash" }, ccy.trim().ifBlank { "IDR" }.uppercase(), bal.toDoubleOrNull() ?: 0.0)) }
            ) { Text(r.t("save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(r.t("cancel")) } },
        title = { Text(r.t("add_account")) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(r.t("name")) }, singleLine = true)
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text(r.t("type")) }, singleLine = true)
                OutlinedTextField(value = ccy, onValueChange = { ccy = it }, label = { Text(r.t("currency")) }, singleLine = true)
                OutlinedTextField(value = bal, onValueChange = { bal = it }, label = { Text(r.t("balance")) }, singleLine = true)
            }
        }
    )
}
