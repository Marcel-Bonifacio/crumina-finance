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
        "no_accounts" to "No accounts yet", "no_holdings" to "No holdings yet", "spent_30" to "Spent · 30d",
        "statement_unlock" to "Statement unlock", "password" to "Password",
        "none_found" to "No protected statements found", "done" to "Done", "set" to "Set",
        "checking" to "Checking your email…",
        "insights" to "Insights", "daily_avg" to "Daily average", "biggest" to "Biggest",
        "transactions" to "Transactions", "where_goes" to "Where it goes · 30d",
        "budgets" to "Budgets", "set_budgets" to "Set budgets", "overall" to "Overall",
        "period" to "Period", "weekly" to "Weekly", "monthly" to "Monthly",
        "left" to "left", "over" to "over by",
        "no_budgets" to "No budgets set",
        "recurring" to "Recurring", "no_recurring" to "No recurring payments yet",
        "carbon" to "Carbon footprint · 30d", "co2_month" to "Monthly CO\u2082e",
        "trees_year" to "Trees · 1yr", "driving" to "Same as driving", "km" to "km",
        "goals" to "Goals", "add_goal" to "Add goal", "goal_name" to "Goal name",
        "target" to "Target", "saved" to "Saved", "no_goals" to "No goals yet", "reached" to "Reached"
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
        "no_accounts" to "Belum ada rekening", "no_holdings" to "Belum ada investasi", "spent_30" to "Pengeluaran · 30h",
        "statement_unlock" to "Buka kunci rekening koran", "password" to "Kata sandi",
        "none_found" to "Tidak ada rekening koran terkunci", "done" to "Selesai", "set" to "Simpan",
        "checking" to "Memeriksa email…",
        "insights" to "Wawasan", "daily_avg" to "Rata-rata harian", "biggest" to "Terbesar",
        "transactions" to "Transaksi", "where_goes" to "Ke mana perginya · 30h",
        "budgets" to "Anggaran", "set_budgets" to "Atur anggaran", "overall" to "Total",
        "period" to "Periode", "weekly" to "Mingguan", "monthly" to "Bulanan",
        "left" to "tersisa", "over" to "lebih",
        "no_budgets" to "Belum ada anggaran",
        "recurring" to "Berulang", "no_recurring" to "Belum ada pembayaran berulang",
        "carbon" to "Jejak karbon · 30h", "co2_month" to "CO\u2082e bulanan",
        "trees_year" to "Pohon · 1th", "driving" to "Setara berkendara", "km" to "km",
        "goals" to "Target", "add_goal" to "Tambah target", "goal_name" to "Nama target",
        "target" to "Target", "saved" to "Terkumpul", "no_goals" to "Belum ada target", "reached" to "Tercapai"
    )
    fun t(key: String, lang: String): String =
        (if (lang == "id") id else en)[key] ?: en[key] ?: key
}
