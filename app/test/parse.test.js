// Parser tests. Run with `npm test` from app/, or `node test/parse.test.js`.
// No framework and no PDF needed: the parsing modules import pdfjs lazily, so
// they load in plain Node and we feed them sample text from fixtures.js.

const assert = require('assert');
const { detectAccounts, parseStatementTxns } = require('../lib/statements');
const F = require('./fixtures');

let pass = 0, fail = 0;
function eq(label, got, want) {
  try { assert.deepStrictEqual(got, want); pass++; console.log('ok   ' + label); }
  catch (e) { fail++; console.log('FAIL ' + label + '\n     got  ' + JSON.stringify(got) + '\n     want ' + JSON.stringify(want)); }
}

// --- Running-balance parser ---------------------------------------------------

// Year-less dd/mm dates are now extracted, and the amount/sign come from the
// balance delta (50,000 out, then 5,000,000 in).
let r = parseStatementTxns(F.rbYearless);
eq('RB year-less: row count', r.length, 2);
eq('RB year-less: amounts', r.map(x => x.amount), [-50000, 5000000]);

// Full dates are kept verbatim (the year resolver reads them later) and the
// credit row comes out positive via the balance delta.
r = parseStatementTxns(F.rbFullDate);
eq('RB full-date: amounts', r.map(x => x.amount), [-120000, 80000]);
eq('RB full-date: keeps year on the row', r[0].date, '03/06/2024');

// --- Balance detection --------------------------------------------------------

eq('Permata picks current bill, not Sebelumnya',
  detectAccounts(F.permataPrev),
  [{ institution: 'PermataBank', name: 'Credit card', type: 'credit_card', balance: -7250000 }]);

eq('US closing balance', detectAccounts(F.usGeneric)[0].balance, 12345.67);
eq('EUR dot-thousands balance', detectAccounts(F.eurGeneric)[0].balance, 1234567.89);

// --- Flat parser --------------------------------------------------------------

// "QR-PAYMENT" is outgoing; it must not be classed as a credit.
r = parseStatementTxns(F.qrFlat);
eq('flat: QR-PAYMENT is a spend (negative)', r[0].amount < 0, true);

// --- Year resolution ----------------------------------------------------------
// These mirror stmtTs + the period logic in app/app.js (stmtTxFeed). Keep the
// two copies in sync; this guards the previous-year and Dec->Jan behaviour.
function stmtTs(d, yr) {
  const m = String(d || '').match(/(\d{1,2})[\/-](\d{1,2})(?:[\/-](\d{2,4}))?/);
  if (!m) return NaN;
  const y = m[3] ? (+m[3] < 100 ? 2000 + (+m[3]) : +m[3]) : (+yr || 0);
  return new Date(y, (+m[2] || 1) - 1, +m[1] || 1).getTime();
}
function ry(ds, y0, y1) {
  let mm = String(ds || '').match(/\d{1,2}[\/-](\d{1,2})/); mm = mm ? +mm[1] : 0;
  if (y0 && y1 && y1 > y0) return mm === 12 ? y0 : y1;
  return y0 || 0;
}
const yearOf = (d, y0, y1) => new Date(stmtTs(d, ry(d, y0, y1))).getFullYear();

eq('previous-year statement (2024)', yearOf('15/05', 2024, 2024), 2024);
eq('boundary: December stays in start year', yearOf('20/12', 2024, 2025), 2024);
eq('boundary: January rolls to end year', yearOf('05/01', 2024, 2025), 2025);
eq('embedded row year overrides the period', yearOf('03/06/2023', 2026, 2026), 2023);

console.log('\n' + pass + ' passed, ' + fail + ' failed');
process.exit(fail ? 1 : 0);
