package id.tirtawijata.crumina.data

object Strings {
    private val en = mapOf(
        "overview" to "Overview", "accounts" to "Accounts", "activity" to "Activity",
        "portfolio" to "Portfolio", "settings" to "Settings",
        "net_worth" to "Net worth", "cash" to "Cash & accounts", "investments" to "Investments",
        "recent" to "Recent transactions", "no_tx" to "No transactions yet",
        "add_account" to "Add account", "add_holding" to "Add holding",
        "name" to "Name", "type" to "Type", "currency" to "Currency", "balance" to "Balance",
        "symbol" to "Symbol", "units" to "Units", "avg_cost" to "Average cost",
        "sync" to "Sync from email", "syncing" to "Syncing…", "language" to "Language",
        "hide_amounts" to "Hide amounts", "log_out" to "Log out", "signed_in" to "Signed in as",
        "save" to "Save", "cancel" to "Cancel", "remove" to "Remove", "from_statements" to "From your statements",
        "no_accounts" to "No accounts yet", "no_holdings" to "No holdings yet", "spent_30" to "Spent · 30d"
    )
    private val id = mapOf(
        "overview" to "Ringkasan", "accounts" to "Rekening", "activity" to "Aktivitas",
        "portfolio" to "Portofolio", "settings" to "Pengaturan",
        "net_worth" to "Kekayaan bersih", "cash" to "Kas & rekening", "investments" to "Investasi",
        "recent" to "Transaksi terbaru", "no_tx" to "Belum ada transaksi",
        "add_account" to "Tambah rekening", "add_holding" to "Tambah investasi",
        "name" to "Nama", "type" to "Jenis", "currency" to "Mata uang", "balance" to "Saldo",
        "symbol" to "Simbol", "units" to "Unit", "avg_cost" to "Harga rata-rata",
        "sync" to "Sinkron dari email", "syncing" to "Menyinkronkan…", "language" to "Bahasa",
        "hide_amounts" to "Sembunyikan nominal", "log_out" to "Keluar", "signed_in" to "Masuk sebagai",
        "save" to "Simpan", "cancel" to "Batal", "remove" to "Hapus", "from_statements" to "Dari rekening koran",
        "no_accounts" to "Belum ada rekening", "no_holdings" to "Belum ada investasi", "spent_30" to "Pengeluaran · 30h"
    )
    fun t(key: String, lang: String): String =
        (if (lang == "id") id else en)[key] ?: en[key] ?: key
}
