// Institution adapters.
//
// This is the only file that knows about specific banks. The parsing engine in
// statements.js and parse.js stays generic and reads everything bank-specific
// from the three tables below, so supporting a new institution (or dropping one)
// is an edit here and nowhere else.
//
// Account names are deliberately generic ("Savings account", "Credit card").
// The engine fills in the real balances, account numbers and periods from the
// statement text; we only need the layout cues to find them.

// Sender-domain -> display name. Used to label real-time alerts when no
// dedicated alert adapter matches. Order matters: more specific fragments first
// (e.g. "cimbniaga" before "cimb", "klikbca" before "bca").
const DOMAIN_NAMES = [
  ['cimbniaga', 'CIMB Niaga'], ['cimb', 'CIMB'], ['permata', 'PermataBank'],
  ['klikbca', 'BCA'], ['blubybca', 'blu by BCA'], ['bca', 'BCA'],
  ['mandiri', 'Bank Mandiri'], ['bni', 'BNI'], ['bri', 'BRI'],
  ['jago', 'Bank Jago'], ['seabank', 'SeaBank'], ['allobank', 'Allo Bank'],
  ['ocbc', 'OCBC'], ['danamon', 'Danamon'], ['uob', 'UOB'],
  ['maybank', 'Maybank'], ['hsbc', 'HSBC'], ['btpn', 'BTPN'],
  ['btn', 'BTN'], ['panin', 'Panin'], ['dbs', 'DBS'],
  ['standardchartered', 'Standard Chartered']
];

// E-statement (PDF) adapters.
//   match    - mailbox query that finds the statement email
//   pwKey    - which statement-password slot the user supplies (null = open PDF)
//   guard    - cheap text test before the balance patterns run. It only matters
//              for uploaded PDFs, where there is no sender to go on, and keeps a
//              loose pattern (e.g. "Cash 1,234") from matching the wrong file.
//   accounts - balance patterns to try, first match wins per account.
//              sign:-1 flips a balance negative (e.g. a card's amount owed).
// More than one pattern per account lets a layout change fall back gracefully.
const STATEMENT_ADAPTERS = [
  {
    id: 'cimb_savings', institution: 'CIMB Niaga', pwKey: 'cimb',
    match: { from: 'Statement@cimbniaga.co.id', hasAttachment: true, ext: 'pdf' },
    guard: /OCTO|CIMB|cimbniaga/i,
    accounts: [
      { re: /OCTO Savers IDR ([\d,]+\.\d{2})/, name: 'Savings account', type: 'savings' },
      { re: /SALDO AKHIR ([\d,]+\.\d{2})/, name: 'Savings account', type: 'savings' }
    ]
  },
  {
    id: 'cimb_card', institution: 'CIMB Niaga', pwKey: 'cimb',
    match: { from: 'CreditCard.Estatement@cimbniaga.co.id', hasAttachment: true, ext: 'pdf' },
    guard: /CIMB|cimbniaga/i,
    accounts: [
      { re: /ENDING BALANCE ([\d,]+\.\d{2})/, name: 'Credit card', type: 'credit_card', sign: -1 }
    ]
  },
  {
    id: 'permata_card', institution: 'PermataBank', pwKey: 'permata',
    match: { from: 'permata-estatement@permatabank.co.id', hasAttachment: true, ext: 'pdf' },
    guard: /Permata/i,
    accounts: [
      { re: /Total Tagihan saat ini \(Rp\) ([\d,]+)/, name: 'Credit card', type: 'credit_card', sign: -1 },
      // Spacing and label wording drift between the card layouts, so keep a
      // looser fallback that still anchors on the "total bill" line.
      { re: /Total\s+Tagihan(?!\s+(?:Sebelumnya|Previous|Terakhir))(?:\s+saat\s+ini)?[^\d-]{0,40}?([\d.,]{4,})/i, name: 'Credit card', type: 'credit_card', sign: -1 },
      { re: /(?:Total\s+Amount\s+Due|Current\s+Balance)[^\d-]{0,24}?([\d.,]{4,})/i, name: 'Credit card', type: 'credit_card', sign: -1 }
    ]
  },
  {
    id: 'bca_securities', institution: 'BCA', pwKey: 'bca',
    match: { from: 'eStatement@klikbca.com', hasAttachment: true, ext: 'pdf' },
    guard: /BCA|klikbca/i,
    accounts: [
      { re: /SALDO AKHIR :? ?([\d,]+\.\d{2})/, name: 'Securities cash', type: 'bank' }
    ]
  },
  {
    id: 'broker', institution: 'Brokerage', pwKey: null,
    match: { from: 'no-reply@stockbit.com', subject: 'statement', hasAttachment: true, ext: 'pdf' },
    guard: /Stockbit|Brokerage|sekuritas|securit/i,
    accounts: [
      { re: /Cash ([\d,]+\.?\d*)/, name: 'Brokerage cash', type: 'investment' }
    ]
  }
];

// Real-time alert adapters. `sender` is matched as a substring of the From
// header; `parser` picks which field extractor in parse.js handles the body.
// Anything not listed here falls through to the generic alert parser.
const ALERT_ADAPTERS = [
  { id: 'cimb_card', sender: 'creditcard.notification@cimbniaga', parser: 'cimb' },
  { id: 'permata', sender: 'contact.center@permatabank', parser: 'permata' }
];

module.exports = { DOMAIN_NAMES, STATEMENT_ADAPTERS, ALERT_ADAPTERS };
