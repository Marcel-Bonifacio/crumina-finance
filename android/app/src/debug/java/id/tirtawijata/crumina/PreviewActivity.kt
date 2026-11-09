package id.tirtawijata.crumina

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import id.tirtawijata.crumina.data.Account
import id.tirtawijata.crumina.data.Holding
import id.tirtawijata.crumina.data.Profile
import id.tirtawijata.crumina.data.Repo
import id.tirtawijata.crumina.data.Txn
import id.tirtawijata.crumina.ui.AppScaffold
import id.tirtawijata.crumina.ui.CruminaTheme
import id.tirtawijata.crumina.ui.LoginScreen
import id.tirtawijata.crumina.ui.Tab
import java.time.LocalDate

/**
 * Debug-only. Shows a screen filled with sample data, without signing in or calling the API:
 *   adb shell am start -n id.tirtawijata.crumina/.PreviewActivity --es screen activity
 * screen = login | expired | overview | accounts | portfolio | activity | settings
 */
class PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Repo.init(this)
        seed()
        val screen = intent.getStringExtra("screen") ?: "overview"
        setContent {
            CruminaTheme {
                when (screen) {
                    "login" -> LoginScreen(expired = false) {}
                    "expired" -> LoginScreen(expired = true) {}
                    else -> AppScaffold(onLogout = {}, startTab = Tab.values().firstOrNull { it.key == screen } ?: Tab.Overview, autoRefresh = false)
                }
            }
        }
    }

    private fun seed() {
        val d = { n: Long -> LocalDate.now().minusDays(n).toString() }
        Repo.profile = Profile(email = "marcel@example.com", name = "Marcel Example")
        Repo.fx = mapOf("IDR" to 1.0, "USD" to 1 / 16300.0)
        Repo.accounts = listOf(
            Account("BCA Tahapan", "savings", "IDR", 42_500_000.0),
            Account("Wise USD", "savings", "USD", 1_250.0),
            Account("Cash wallet", "cash", "IDR", 850_000.0)
        )
        Repo.holdings = listOf(
            Holding("BBCA.JK", "Bank Central Asia", 500.0, 8_900.0, 9_875.0, "IDR"),
            Holding("AAPL", "Apple", 4.0, 180.0, 228.0, "USD")
        )
        Repo.transactions = listOf(
            Txn(d(1), -85_000.0, "GrabFood", "BCA", "spend"),
            Txn(d(2), -142_500.0, "Indomaret", "BCA", "spend"),
            Txn(d(4), -186_000.0, "Netflix", "Mandiri Visa", "spend"),
            Txn(d(6), 18_500_000.0, "Salary", "BCA", "income"),
            Txn(d(7), -350_000.0, "Shell", "Mandiri Visa", "spend"),
            Txn(d(9), -62_000.0, "Starbucks", "BCA", "spend"),
            Txn(d(12), -499_000.0, "Tokopedia", "Mandiri Visa", "spend"),
            Txn(d(34), -186_000.0, "Netflix", "Mandiri Visa", "spend"),
            Txn(d(40), -1_250_000.0, "Traveloka", "Mandiri Visa", "spend"),
            Txn(d(70), -900_000.0, "Superindo", "BCA", "spend")
        )
    }
}
