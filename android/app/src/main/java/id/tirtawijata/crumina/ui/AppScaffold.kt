package id.tirtawijata.crumina.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import id.tirtawijata.crumina.data.Repo
import kotlinx.coroutines.launch

private enum class Tab(val key: String, val icon: ImageVector) {
    Overview("overview", Icons.Filled.Home),
    Accounts("accounts", Icons.Filled.AccountBalanceWallet),
    Activity("activity", Icons.Filled.ReceiptLong),
    Portfolio("portfolio", Icons.Filled.ShowChart),
    Settings("settings", Icons.Filled.Settings)
}

@Composable
fun AppScaffold(onLogout: () -> Unit) {
    var tab by remember { mutableStateOf(Tab.Overview) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { Repo.refresh() }
    Scaffold(bottomBar = {
        NavigationBar {
            Tab.values().forEach { tb ->
                NavigationBarItem(
                    selected = tab == tb,
                    onClick = { tab = tb },
                    icon = { Icon(tb.icon, contentDescription = Repo.t(tb.key)) },
                    label = { Text(Repo.t(tb.key)) }
                )
            }
        }
    }) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                Tab.Overview -> OverviewScreen(onSync = { scope.launch { Repo.refresh() } })
                Tab.Accounts -> AccountsScreen()
                Tab.Activity -> ActivityScreen()
                Tab.Portfolio -> PortfolioScreen()
                Tab.Settings -> SettingsScreen(onLogout = onLogout, onSync = { scope.launch { Repo.refresh() } })
            }
        }
    }
}
