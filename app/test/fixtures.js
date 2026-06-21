// Synthetic statement samples for the parser tests. All values are made up;
// there is no real account data here. PDF text arrives as one whitespace-collapsed
// string, so these fixtures are single lines on purpose.

module.exports = {
  // Running-balance statement with YEAR-LESS dates (dd/mm). The year comes from
  // the statement period at display time, not from the rows.
  rbYearless:
    'SALDO AWAL 1,000,000.00 ' +
    '01/05 GROCERY STORE 50,000.00 950,000.00 ' +
    '02/05 SALARY 5,000,000.00 5,950,000.00 ' +
    'SALDO AKHIR 5,950,000.00',

  // Running-balance statement with FULL dates (dd/mm/yyyy) and a credit row.
  rbFullDate:
    'OPENING BALANCE 2,000,000.00 ' +
    '03/06/2024 CAFE 120,000.00 1,880,000.00 ' +
    '04/06/2024 REFUND CR 80,000.00 1,960,000.00 ' +
    'CLOSING BALANCE 1,960,000.00',

  // Card statement that prints the PREVIOUS bill before the current one. The
  // balance reader must pick the current total, not "Sebelumnya".
  permataPrev:
    'PermataBank Kartu Kredit ' +
    'Total Tagihan Sebelumnya (Rp) 3.000.000 ' +
    'Total Tagihan 7.250.000 Pembayaran Minimum 362.500',

  // Flat statement (no balance column, month-name dates). "QR-PAYMENT" is an
  // outgoing payment and must not be read as a credit.
  qrFlat:
    '05 Jun QR-PAYMENT WARUNG KOPI 18.500 ' +
    '06 Jun TOKO BUKU 25.000 ' +
    '07 Jun TRANSFER KE BUDI 200.000',

  // Generic layouts in other locales / currencies.
  usGeneric: 'ACME BANK Statement Closing Balance $ 12,345.67 Available Balance $ 12,345.67',
  eurGeneric: 'Konto Closing balance EUR 1.234.567,89'
};
