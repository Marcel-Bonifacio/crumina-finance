package id.tirtawijata.crumina.data

import com.google.gson.annotations.SerializedName

// ---- API responses ----
data class Profile(val email: String? = null, val name: String? = null, val picture: String? = null)
data class DataResponse(val profile: Profile? = null)

data class FxResponse(val base: String? = null, val rates: Map<String, Double>? = null)

data class Txn(
    val date: String? = null,
    val amount: Double = 0.0,
    val merchant: String? = null,
    val account: String? = null,
    @SerializedName("class") val cls: String? = null,
    val status: String? = null,
    val source: String? = null
)

data class SyncData(
    val transactions: List<Txn>? = null,
    val spent30: Double = 0.0,
    val income30: Double = 0.0,
    val count: Int = 0
)
data class SyncResponse(val ok: Boolean = false, val data: SyncData? = null)

data class StmtAccount(
    val institution: String? = null,
    val name: String? = null,
    val type: String? = null,
    val balance: Double? = null,
    val ccy: String? = null,
    val bank: String? = null,
    val uid: String? = null,
    val error: String? = null,
    val txns: List<Txn>? = null
)
data class StatementsResponse(val ok: Boolean = false, val accounts: List<StmtAccount>? = null)

data class Quote(val symbol: String? = null, val name: String? = null, val exch: String? = null, val type: String? = null)
data class SearchResponse(val quotes: List<Quote>? = null)
data class QuoteResponse(
    val symbol: String? = null,
    val price: Double? = null,
    val currency: String? = null,
    val name: String? = null,
    val exch: String? = null
)

// ---- Local (on-device) data ----
data class Account(val name: String, val type: String, val ccy: String = "IDR", val bal: Double = 0.0)
data class Holding(
    val symbol: String,
    val name: String,
    val units: Double,
    val avg: Double,
    val price: Double = 0.0,
    val ccy: String = "USD"
)

// statement-unlock discovery
data class BankSlot(val key: String? = null, val institution: String? = null)
data class DiscoverResponse(val ok: Boolean = false, val banks: List<BankSlot>? = null)

// budgets
data class Budget(
    val period: String = "monthly",
    val overall: Double = 0.0,
    val cats: Map<String, Double> = emptyMap()
)

// savings goals (local)
data class Goal(val name: String, val target: Double, val saved: Double = 0.0)

// manual expense + PDF upload
data class ManualTx(val amount: Double, val merchant: String, val category: String, val date: String, val ccy: String = "IDR")
data class UploadReq(val pdf: String, val password: String? = null)
data class UploadResponse(val ok: Boolean = false, val accounts: List<StmtAccount>? = null, val error: String? = null, val note: String? = null)
