# Bank parsers

Crumina understands two kinds of bank mail: monthly statement PDFs and real-time
transaction alerts. Everything specific to an institution, which emails to look for,
how to read a balance, how to label an alert, lives in one registry,
[`app/lib/banks.js`](../app/lib/banks.js). The parsing engines in `statements.js` and
`parse.js` are generic and read from that registry, so adding or removing a bank is an
edit to one file.

Account names in the registry are deliberately generic ("Savings account",
"Credit card"). The engine fills in the real balance, account number and statement
period from the document; it does not need a personal nickname.

## The registry

`banks.js` exports three tables.

### `DOMAIN_NAMES`

Maps a sender-domain fragment to a display name. It labels real-time alerts when no
dedicated alert adapter matches. Order matters, put the more specific fragment first.

```js
['cimbniaga', 'CIMB Niaga'], ['cimb', 'CIMB'], ['klikbca', 'BCA'], ['bca', 'BCA']
```

### `STATEMENT_ADAPTERS`

One entry per e-statement layout.

| Field | Meaning |
|---|---|
| `id` | Stable identifier used internally (e.g. `cimb_card`). |
| `institution` | Display name for the account. |
| `pwKey` | Which statement-password slot the user supplies. `null` for an open PDF. |
| `match` | Mailbox query that finds the statement email: `from`, optional `subject`, `hasAttachment`, `ext`. |
| `guard` | A cheap text test applied **only on the upload path**, where there is no sender. It stops a loose pattern from matching the wrong file. |
| `accounts` | Balance patterns to try. First match wins per account. `sign: -1` flips the balance negative (a card's amount owed). |

```js
{
  id: 'permata_card', institution: 'PermataBank', pwKey: 'permata',
  match: { from: 'permata-estatement@permatabank.co.id', hasAttachment: true, ext: 'pdf' },
  guard: /Permata/i,
  accounts: [
    { re: /Total Tagihan saat ini \(Rp\) ([\d,]+)/, name: 'Credit card', type: 'credit_card', sign: -1 },
    { re: /Total\s+Tagihan(?:\s+saat\s+ini)?[^\d-]{0,40}?([\d.,]{4,})/i, name: 'Credit card', type: 'credit_card', sign: -1 }
  ]
}
```

Listing more than one pattern per account gives you a fallback when a layout changes
slightly. Captured amounts are read with a format-tolerant parser that handles both
`1,234,567.89` and `1.234.567` styles, so you do not need to worry about whether a
statement uses commas or dots for thousands.

### `ALERT_ADAPTERS`

One entry per issuer that sends real-time alerts with a known format.

| Field | Meaning |
|---|---|
| `sender` | Substring matched against the email's `From`. |
| `parser` | Which field extractor in `parse.js` handles the body (`cimb` or `permata`). |

Senders not listed here fall through to the heuristic parser, which looks for an
amount, a date and a merchant and skips OTP and marketing mail.

## How a statement is read

- **From the mailbox** (`/api/statements`): the engine walks `STATEMENT_ADAPTERS`,
  finds the email with each adapter's `match`, fetches the PDF, decrypts it with the
  `pwKey` password, and applies that adapter's `accounts` patterns. Because the email
  sender already identifies the bank, the `guard` is not needed here.
- **From an upload** (`/api/upload`): there is no sender, so the engine tries every
  adapter whose `guard` matches the extracted text, then its `accounts` patterns. If
  nothing matches, a generic fallback handles common "closing balance" and "amount
  due" layouts.

## Adding a bank

To support a new institution's e-statement:

1. Open `app/lib/banks.js`.
2. Add an entry to `STATEMENT_ADAPTERS` with the sender address, the password slot (or
   `null`), a `guard` that appears in the statement text, and one or more balance
   patterns. Use a capture group around the number.
3. If the bank also sends real-time alerts in a custom format, add its domain to
   `DOMAIN_NAMES` (for the label) and, if you want precise field extraction, an
   `ALERT_ADAPTERS` entry plus a parser function in `parse.js`. Otherwise the heuristic
   parser will handle the alerts.

That is the whole change, the engines pick it up with no further edits.

## Testing a pattern

The parsing functions run in plain Node with no PDF library needed (the PDF import is
lazy), so you can check a balance pattern against sample text quickly:

```js
const { detectAccounts } = require('./app/lib/statements');
console.log(detectAccounts('PermataBank Total Tagihan saat ini (Rp) 5,000,000'));
// [ { institution: 'PermataBank', name: 'Credit card', type: 'credit_card', balance: -5000000 } ]
```

For alerts, call `parseAlert(from, subject, text)` from `app/lib/parse.js` with a
representative sender and body. Keep a few real (redacted) statement and alert samples
around as fixtures when you change a pattern.

## Dates and years

Statement text rarely repeats the year on every transaction line, so the parser
takes the day and month from the row and the year from the statement period. Three
cases are handled:

- A row that prints its own full date (`dd/mm/yyyy`) keeps that year.
- A statement from a previous year is dated by its period, not by today's date, so
  last year's stateme