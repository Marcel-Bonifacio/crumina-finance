# API reference

Every endpoint is one file under `app/api/`, written as a Node `(req, res)` handler.
Responses are JSON unless noted. Authentication is a signed session cookie
(`tally_session`); handlers that need it return `401` when it is absent or invalid.
Several endpoints are rate-limited per IP and return `429` with a `Retry-After`
header when the limit is exceeded.

## Summary

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/auth/start` | none | Begin Google OAuth (redirect to Google) |
| GET | `/api/auth/callback` | none | OAuth return; establishes a session |
| GET | `/api/guest` | none | Start a local (no-account) session |
| GET | `/api/guest?config=1` | none | Report which sign-in methods are available |
| POST | `/api/guest` | none | IMAP sign-in (only when `IMAP_ENABLED`) |
| GET | `/api/logout` | none | Clear the session and redirect to login |
| GET | `/api/data` | session | Return the signed-in profile |
| GET | `/api/sync` | session | Parse recent alert emails into transactions |
| GET | `/api/statements` | session | Read monthly statement PDFs (or discover banks) |
| POST | `/api/unlock` | session | Store a per-bank statement password |
| POST | `/api/upload` | session | Parse an uploaded statement PDF |
| GET | `/api/fx` | none | Exchange-rate proxy |
| GET | `/api/yf` | session | Price search and history proxy |

## Authentication and session

### `GET /api/auth/start`

Generates a one-time `state` nonce (stored in the `oauth_state` cookie) and redirects
to Google's consent screen for the `openid email profile gmail.readonly` scopes.
Rate-limited.

### `GET /api/auth/callback`

Google redirects here with `code` and `state`. The handler checks `state` against the
`oauth_state` cookie, exchanges the code for tokens, and requires a verified Google
email. On success it sets the session cookie, a profile cookie, and, if Google
returned one, the encrypted refresh token, then redirects to `/`.

### `GET /api/guest`

Issues a local session (`email` = `guest`) and redirects to `/`. This is the
"use locally, no account" path; all data stays in the browser.

### `GET /api/guest?config=1`

Public. Returns the sign-in methods the instance supports, so the login page can show
the right buttons.

```json
{ "imap": false, "google": true }
```

### `POST /api/guest`

IMAP sign-in. Returns `404` unless `IMAP_ENABLED` is set. Rejects cross-site requests
(checks `Sec-Fetch-Site` and `Origin`), is rate-limited, and caps the body at 4 KB.
Form fields: `provider` (`gmail`, `outlook`, `icloud`, or empty for a custom server),
`email`, `password` (an app-specific password), and for a custom server `host` and
`port`. The credentials are tested with a real IMAP connection; on success they are
stored AES-256-GCM encrypted in the `cr_imap` cookie and the user is redirected to
`/`. On failure the login page is shown again with an error code.

### `GET /api/logout`

Clears the session cookie and redirects to `/login`.

## Data

### `GET /api/data`

Returns the signed-in user's profile (read from the profile cookie). Transaction data
is not returned here; the client pulls it from `/api/sync` and `/api/statements`.

```json
{ "profile": { "email": "you@example.com", "name": "You", "picture": "" }, "data": null }
```

### `GET /api/sync`

Searches the mailbox for recent bank and card alert emails (last 120 days, across a
list of known issuer domains, excluding statement mail), parses each into a
transaction, removes duplicates, classifies transfers between accounts, and returns
the feed with 30-day spending and income totals. Rate-limited.

```json
{
  "ok": true,
  "data": {
    "generatedAt": "2026-06-21T03:00:00.000Z",
    "source": "Bank & card alerts",
    "count": 42,
    "spent30": 5120000,
    "income30": 18000000,
    "transactions": [
      { "date": "2026-06-18", "amount": -85000, "merchant": "Coffee Shop", "account": "CIMB Niaga", "class": "spend", "source": "alert", "status": "pending" }
    ]
  }
}
```

### `GET /api/statements`

Reads monthly statement PDFs from the mailbox and returns the detected accounts with
balances and, when present, transaction rows. Statement passwords come from the
`tally_pw` cookie (see `/api/unlock`). Rate-limited.

With `?discover=1`, it instead returns the institutions found in the mailbox that
require a password, so the UI can prompt for the right ones:

```json
{ "ok": true, "banks": [ { "key": "cimb", "institution": "CIMB Niaga" } ] }
```

Without it:

```json
{
  "ok": true,
  "accounts": [
    { "institution": "CIMB Niaga", "name": "Savings account", "type": "savings", "balance": 12500000, "source": "statement", "bank": "cimb_savings", "acctNo": "1234xxxx", "period": "01 May 2026 - 31 May 2026" }
  ]
}
```

### `POST /api/unlock`

Stores a statement password for one institution. The map of passwords is kept
AES-256-GCM encrypted in the `tally_pw` cookie and used by `/api/statements`.

Request: `{ "bank": "cimb", "password": "..." }` → Response: `{ "ok": true, "banks": ["cimb"] }`

### `POST /api/upload`

Parses a statement PDF the user uploads directly (for accounts not in the mailbox).
The body is JSON with a base64 (or data-URL) `pdf` and an optional `password`. The
file is capped at 4 MB. Returns the detected accounts, or an error code when the PDF
is password-protected or unreadable.

```json
{ "ok": true, "accounts": [ { "institution": "Statement", "name": "Credit card", "type": "credit_card", "balance": -4900000, "source": "upload", "ccy": "IDR" } ] }
```

```json
{ "ok": false, "error": "wrong_or_missing_password" }
```

## Proxies

### `GET /api/fx`

Returns exchange rates for a base currency (default `IDR`), cached for an hour. Public,
because it exposes no user data.

```json
{ "base": "IDR", "rates": { "USD": 0.000061, "SGD": 0.000083 } }
```

### `GET /api/yf`

A thin proxy for price data, used by the portfolio. `?q=<text>` searches for a ticker
and returns matches; `?symbol=<ticker>&range=<range>` returns the latest price and a
daily close history. Requires a session and is rate-limited; the server adds no key,
it only forwards the request and trims the response.

```json
{ "symbol": "AAPL", "price": 195.2, "currency": "USD", "name": "Apple Inc.", "history": [[1718000000000, 194.1]] }
```
