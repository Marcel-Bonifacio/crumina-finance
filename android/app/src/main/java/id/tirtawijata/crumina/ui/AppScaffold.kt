package id.tirtawijata.crumina.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tirtawijata.crumina.data.Repo
import kotlinx.coroutines.launch

/** Same tabs, order and icons as the web app's bottom bar (`.mnav`). */
enum class Tab(val key: String, val icon: ImageVector) {
    Overview("overview", Icons.Outlined.Home),
    Accounts("accounts", Icons.Outlined.CreditCard),
    Portfolio("portfolio", Icons.Outlined.BarChart),
    Activity("activity", Icons.Outlined.ShowChart),
    Settings("settings", Icons.Outlined.Tune)
}

@Composable
fun AppScaffold(onLogout: () -> Unit, startTab: Tab = Tab.Overview, autoRefresh: Boolean = true) {
    var tab by rememberSaveable { mutableStateOf(startTab) }
    val scope = rememberCoroutineScope()
    if (autoRefresh) LaunchedEffect(Unit) { Repo.refresh() }
    val sync: () -> Unit = { scope.launch { Repo.refresh() } }
    val scroll = rememberScrollState()
    AppBackground {
        // Every tab scrolls as one page under the floating bar, like the web layout.
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp))
        ) {
            TopBar(Repo.initial)
            when (tab) {
                Tab.Overview -> OverviewScreen(onSync = sync, go = { tab = it })
                Tab.Accounts -> AccountsScreen()
                Tab.Portfolio -> PortfolioScreen()
                Tab.Activity -> ActivityScreen()
                Tab.Settings -> SettingsScreen(onLogout = onLogout, onSync = sync)
            }
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(104.dp))
        }
        // Keep the status bar readable once content scrolls under it.
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(LocalCr.current.bg.copy(alpha = if (scroll.value > 0) 0.96f else 0f)))
        Box(Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
            BottomNav(tab) { tab = it }
        }
    }
}

@Composable
private fun BottomNav(selected: Tab, onSelect: (Tab) -> Unit) {
    val cr = LocalCr.current
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth()
            .shadow(16.dp, shape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
            .clip(shape).background(cr.card) // opaque: Android has no backdrop blur like the web bar
            .border(1.dp, if (cr.dark) Color.White.copy(alpha = 0.08f) else cr.line, shape)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Tab.values().forEach { t ->
            val on = t == selected
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .background(if (on) cr.teal.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onSelect(t) }.padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val c = if (on) cr.ink else cr.muted
                Icon(t.icon, contentDescription = Repo.t(t.key), tint = c, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(3.dp))
                Text(Repo.t(t.key), color = c, fontSize = 10.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
            }
        }
    }
}
