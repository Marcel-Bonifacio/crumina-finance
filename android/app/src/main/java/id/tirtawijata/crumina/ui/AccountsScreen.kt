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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import id.tirtawijata.crumina.data.Account
import id.tirtawijata.crumina.data.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AccountsScreen() {
    val r = Repo
    val cr = LocalCr.current
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
    val stmt = r.allStmt
    val count = r.accounts.size + stmt.size
    PageHeader(r.t("accounts"), String.format(r.t("accounts_n"), count))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 16.dp)) {
        GhostButton(if (r.loading) r.t("uploading") else r.t("upload_pdf"), { pdfPicker.launch("application/pdf") }, enabled = !r.loading)
        PillButton(r.t("q_account"), { showAdd = true })
    }
    r.error?.let { Muted(it, 12.sp, Modifier.padding(bottom = 12.dp)) }
    CrCard {
        if (count == 0) EmptyText(r.t("no_accounts"))
        r.accounts.forEachIndexed { i, a ->
            ListRow(i == 0, { IconTile(a.name.take(1).uppercase(), accountGrad(a.type)) }, a.name, typeLabel(a.type) + " · " + a.ccy) {
                Ink(r.money(a.bal, a.ccy, false), 14.sp, FontWeight.Bold)
                RemoveX { r.removeAccount(i) }
            }
        }
        stmt.forEachIndexed { i, s ->
            val name = s.name ?: s.institution ?: "Account"
            ListRow(r.accounts.isEmpty() && i == 0, { IconTile((s.institution ?: name).take(1).uppercase(), accountGrad(s.type)) },
                name, listOfNotNull(typeLabel(s.type), s.institution).joinToString(" · ")) {
                Ink(r.money(s.balance ?: 0.0, s.ccy ?: "IDR"), 14.sp, FontWeight.Bold, color = if ((s.balance ?: 0.0) < 0) cr.red else null)
            }
        }
    }
    if (showAdd) AddAccountDialog(onDismiss = { showAdd = false }, onSave = { r.addAccount(it); showAdd = false })
}

private fun typeLabel(t: String?): String = when (t) {
    null, "" -> "Account"
    "credit_card" -> "Credit card"
    "ewallet" -> "E-wallet"
    else -> t.replaceFirstChar { it.uppercase() }
}

@Composable
fun AddAccountDialog(onDismiss: () -> Unit, onSave: (Account) -> Unit) {
    val r = Repo
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("cash") }
    var ccy by remember { mutableStateOf(r.mainCcy) }
    var bal by remember { mutableStateOf("") }
    CrDialog(
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
                CrField(value = name, onValueChange = { name = it }, label = { Text(r.t("name")) }, singleLine = true)
                CrField(value = type, onValueChange = { type = it }, label = { Text(r.t("type")) }, singleLine = true)
                CrField(value = ccy, onValueChange = { ccy = it }, label = { Text(r.t("currency")) }, singleLine = true)
                CrField(value = bal, onValueChange = { bal = it }, label = { Text(r.t("balance")) }, singleLine = true)
            }
        }
    )
}
