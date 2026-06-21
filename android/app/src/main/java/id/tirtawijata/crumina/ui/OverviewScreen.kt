package id.tirtawijata.crumina.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.tirtawijata.crumina.data.Net
import id.tirtawijata.crumina.data.SecureStore

@Composable
fun OverviewScreen(store: SecureStore, onLogout: () -> Unit) {
    var status by remember { mutableStateOf("Loading…") }

    LaunchedEffect(Unit) {
        status = try {
            val data = Net.api(store).data()
            val who = data.profile?.name ?: data.profile?.email ?: "You"
            "Signed in as $who"
        } catch (e: Exception) {
            "Could not reach the server: ${e.message ?: "unknown error"}"
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Crumina", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Text(
            "Net worth, accounts, activity and insights render here as the data screens are ported.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onLogout) { Text("Log out") }
    }
}
