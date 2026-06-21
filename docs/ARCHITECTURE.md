# Architecture

This document describes how Crumina is put together: the major components, where
state lives, and what happens on a request. It assumes you have read the
[README](../README.md).

## Design goals

Crumina was built around a few constraints that explain most of the decisions
below.

- **The server should not become a copy of your bank.** It reads your mailbox on
  demand and returns parsed results, but it does not persist your transactions in a
  database. If the server is compromised, there is no ledger to steal.
- **The client should work on its own.** The app shell, parsing of locally entered
  data, categorisation and the charts all run in the browser. A network round-trip
  is needed only to pull fresh mail or live prices.
- **Adding a bank should not mean touching the engine.** Institution details live
  in a single registry; the parsers are generic.
- **No build step.** The client is plain HTML, CSS and JavaScript so it can be read,
  audited and modified without a toolchain.

## Components

```
Browser (PWA)                         Server (stateless handlers)
┌───────────────────────────┐         ┌─────────────────────────────┐
│ index.html  app.js        │  fetch  │ api/*.js   one per endpoint  │
│ service worker (sw.js)    │ ──────▶ │ lib/session  signed cookies  │
│ on-device OCR (Tesseract) │         │ lib/crypto   AES-256-GCM     │
│ local state (in-memory +  │         │ lib/mailbox  Gmail | IMAP    │
│ browser storage)          │ ◀────── │ lib/parse    alert parsing   │
└───────────────────────────┘  JSON   │ lib/statements  PDF parsing  │
                                       │ lib/banks    institution reg │
                                       └──────────────┬──────────────┘
                                                      │
                                       Gmail API / IMAP    Price quotes
```

### Client (`app/`)

`index.html` and `app.js` are the whole front end. There is no framework. State is
held in JavaScript and mirrored to browser storage so a reload (or an offline start)
restores the last view. The service worker caches the shell and assets, which is
what makes the app installable and usable without a connection.

The client owns: rendering, categorisation, the spending and Insights math, manual
transactions and the portfolio, the carbon estimate, theming, and language. It calls
the server only to sign in, pull mail-derived data, and fetch prices.

### Server (`app/api/` and `app/lib/`)

Each file in `api/` is one endpoint, written as a Node `(req, res)` handler. On the
managed platform these run as serverless functions; when self-hosting, the same
files are required and called by `selfhost/server.js`. The handlers share the
libraries in `lib/`:

| Library | Responsibility |
|---|---|
| `session.js` | Sign and verify the session cookie (HMAC-SHA256, 30-day expiry) |
| `crypto.js` | Encrypt and decrypt secrets at rest (AES-256-GCM) |
| `mailbox.js` | One `{search, fetch, close}` interface over Gmail or IMAP |
| `google.js` | Google OAuth token exchange and Gmail REST calls |
| `parse.js` | Turn real-time alert emails into transactions |
| `statements.js` | Extract text from statement PDFs and parse balances and rows |
| `banks.js` | The institution registry the two parsers read from |
| `ratelimit.js` | Per-IP request throttling |

## Where state lives

There is no application database. State is spread across three places, each chosen
for what it holds.

- **Browser.** Accounts, manual transactions, the portfolio, categories and settings
  persist in browser storage on the user's device. This is the bulk of the data.
- **Cookies (signed or encrypted).** The session, a small profile blob, and any
  stored secrets. Secrets are encrypted before they touch a cookie:

  | Cookie | Contents | Protection |
  |---|---|---|
  | `tally_session` | `{email, issued-at}` | HMAC-SHA256 signature |
  | `tally_prof` | display name, email, avatar URL | base64 (not secret) |
  | `tally_rt` | Google refresh token | AES-256-GCM |
  | `tally_pw` | per-bank statement passwords | AES-256-GCM |
  | `cr_imap` | IMAP host, user and app password | AES-256-GCM |

- **The mailbox.** The source of truth for statements and alerts. Crumina reads it
  per request and keeps nothing server-side afterward.

## Request lifecycle

A typical "refresh my data" cycle:

1. The browser calls `GET /api/sync` (alerts) and `GET /api/statements` (monthly
   PDFs). The session cookie rides along.
2. The handler verifies the session, then asks `lib/mailbox` for a mailbox. The
   mailbox is Gmail if a Google refresh token is present, or IMAP if the instance
   has IMAP enabled and stored credentials.
3. `mailbox.search(spec)` runs a provider-specific query (a Gmail query string or an
   IMAP SEARCH) built from a single spec object, so the calling code does not care
   which provider answered.
4. For statements, the PDF attachment is fetched and decrypted with the stored
   per-bank password, its text is extracted, and `lib/statements` reads the balance
   and transaction rows. For alerts, `lib/parse` pulls the amount, date and merchant
   out of the email body.
5. The handler returns JSON. The mailbox connection is closed. Nothing is written to
   disk.
6. The browser merges the results with locally held data, reconciles, categorises,
   and re-renders.

## The parsing pipeline

Two inputs become one transaction feed.

- **Monthly statements** (`lib/statements.js`). Text is extracted from the PDF with
  `pdfjs-dist`. Balances are matched by the patterns in the institution registry;
  transaction rows are read by two strategies, a running-balance parser that checks
  each row's delta against the printed balance, and a flat parser for layouts without
  a balance column. Both reject promotional lines.
- **Real-time alerts** (`lib/parse.js`). The sender selects a field extractor from the
  registry; unknown senders fall through to a heuristic parser that looks for an
  amount, a date and a merchant, and skips OTP and marketing mail.

Merchant strings from either source pass through `cleanDesc`, which strips bank
boilerplate, reference numbers and location tails, and maps common billers to a
readable name. The same `cleanDesc` exists on the client so locally entered and
server-parsed transactions look alike.

## Classification and reconciliation

Each transaction is given a class: `spend`, `fee`, `income`, `transfer_in`,
`transfer_out`, `transfer`, `topup`, or `card_payment`. The class decides whether it
counts toward spending, income, or neither.

Two cases need care, because both can double-count money:

- **Transfers between your own accounts.** When a debit on one account matches a
  credit of the same amount on another within a few days, both legs are marked
  `transfer` and excluded from spending and income. This is `classifyPair` in
  `lib/parse.js`.
- **Credit-card bill payments.** Paying a card moves money from savings to settle a
  balance; it is not new spending, and the card's purchases are already counted. Both
  the savings-side payment and the card-side credit are classed `card_payment` and
  excluded from spending and income. Without this, the bill payment would be counted
  again on top of the purchases.

## Mailbox abstraction

`lib/mailbox.js` is the seam that lets the same handlers work with Gmail or any IMAP
server. It exposes `getMailbox(req)`, which returns an object with `search`, `fetch`
and `close`. A search spec (sender, since-days, subject, has-attachment, file
extension) is translated to a Gmail query or an IMAP SEARCH, and messages are
normalised to `{from, subject, dateMs, html, text, attachments}`. Callers in
`api/sync.js` and `api/statements.js` never branch on the provider.

## Further reading

- [API reference](API.md): every endpoint, its method, auth and response
- [Configuration](CONFIGURATION.md): environment variables and feature flags
- [Bank parsers](BANK-PARSERS.md): the registry format and how to add an institution
- [Security](SECURITY.md): the threat model and cryptography in detail
