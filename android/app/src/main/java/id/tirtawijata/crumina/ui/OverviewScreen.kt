package id.tirtawijata.crumina.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import id.tirtawijata.crumina.data.Repo

@Composable
fun OverviewScreen(onSync: () -> Unit, go: (Tab) -> Unit) {
    val r = Repo
    val cr = LocalCr.current
    var addExpense by remember { mutableStateOf(false) }
    var addAccount by remember { mutableStateOf(false) }
    var addHolding by remember { mutableStateOf(false) }

    val hour = java.time.LocalTime.now().hour
    val greet = r.t(if (hour < 12) "greet_m" else if (hour < 18) "greet_a" else "greet_e")
    PageHeader(greet + (r.firstName?.let { ", $it" } ?: ""), r.t("home_sub")) {
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(cr.card).border(1.dp, cr.line, CircleShape).clickable { r.toggleHide() },
            contentAlignment = Alignment.Center
        ) {
            Icon(if (r.hideAmounts) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = r.t("hide_amounts"), tint = cr.ink, modifier = Modifier.size(20.dp))
        }
    }

    HeroCard {
        HeroLabel(r.t("net_worth") + " · " + r.mainCcy)
        HeroValue(r.money(r.netWorth))
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroChip(r.t("cash"), r.money(r.cashTotal))
            HeroChip(r.t("investments"), r.money(r.investTotal))
        }
        Spacer(Modifier.height(8.dp))
        HeroChipBox(r.t("expenses_30"), r.money(-r.spent30), Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroAction(r.t("q_expense")) { addExpense = true }
            HeroAction(r.t("q_account")) { addAccount = true }
            HeroAction(r.t("q_holdings")) { addHolding = true }
        }
    }
    r.error?.let {
        Spacer(Modifier.height(10.dp))
        Muted(it, modifier = Modifier.fillMaxWidth())
    }
    Gap()

    CrCard(title = r.t("recent"), action = r.t("view_all"), onAction = { go(Tab.Activity) }) {
        val feed = r.feed.take(6)
        if (feed.isEmpty()) EmptyText(r.t("no_tx")) else feed.forEachIndexed { i, t -> TxRow(t, i == 0) }
    }
    Gap()
    CrCard(title = r.t("where_goes")) { WhereItGoes() }
    Gap()
    CrCard(title = r.t("insights")) { InsightsSection() }
    Gap()
    CrCard(title = r.t("trend")) { TrendSection() }
    Gap()
    BudgetsCard()
    Gap()
    CrCard(title = r.t("recurring")) { RecurringSection() }
    Gap()
    CrCard(title = r.t("carbon")) { CarbonSection() }
    Gap()
    GoalsCard()
    Spacer(Modifier.height(4.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        GhostButton(if (r.loading) r.t("syncing") else r.t("sync"), onSync, enabled = !r.loading)
    }

    if (addExpense) AddExpenseDialog(onDismiss = { addExpense = false })
    if (addAccount) AddAccountDialog(onDismiss = { addAccount = false }, onSave = { r.addAccount(it); addAccount = false })
    if (addHolding) AddHoldingDialog(onDismiss = { addHolding = false }, onSave = { r.addHolding(it); addHolding = false })
}

/** Vertical space between cards (`.card` margin-bottom). */
@Composable
fun Gap() = Spacer(Modifier.height(16.dp))
