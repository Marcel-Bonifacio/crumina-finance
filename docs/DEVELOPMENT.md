# Development

Crumina is small on purpose and has no build step, so getting a development copy
running takes a clone and a Node install.

## Setup

```bash
git clone https://github.com/your-org/crumina.git
cd crumina/app
npm ci                      # PDF and IMAP libraries
cd ..
SESSION_SECRET=dev TOKEN_ENC_KEY=$(openssl rand -base64 32) \
  node selfhost/server.js   # http://localhost:3000
```

Open `http://localhost:3000` and choose **Use locally** to work without any mail
accounts. To exercise the mail paths, set `IMAP_ENABLED=1` and connect a test mailbox,
or configure the `GOOGLE_*` variables. Cookies are marked `Secure`, which the browser
accepts on `localhost`; on any other host you need HTTPS.

## How the code is organised

The split between client and server is the thing to keep in mind.

- **Client** (`app/index.html`, `app/app.js`) is vanilla HTML, CSS and JavaScript with
  no framework and no bundler. `app.js` holds the state, rendering, categorisation, the
  charts, and the local data model. It is one large file; search by function name. The
  service worker (`app/sw.js`) caches the shell for offline use, and the web manifest
  makes it installable.
- **Server** (`app/api/*.js`, `app/lib/*.js`) is plain Node. Each `api/` file is one
  endpoint in `(req, res)` form. Shared logic lives in `lib/`. The same handlers run as
  serverless functions on a managed host and behind `selfhost/server.js` when you host
  it yourself.

A few conventions are worth knowing before you change things:

- **`cleanDesc` exists twice**, once in `app/lib/statements.js` (server) and once in
  `app/app.js` (client), and the two must stay identical so a transaction looks the
  same whether it was parsed on the server or entered locally. If you change one,
  change the other.
- **Bank specifics go in `app/lib/banks.js`**, never in the parsing engines. See
  [BANK-PARSERS.md](BANK-PARSERS.md).
- **Text rendered into the DOM is escaped.** Keep it that way; the CSP forbids inline
  scripts, so event handlers are wired with delegation rather than `onclick`.
- **Endpoints are sometimes combined.** `api/guest.js`, for example, handles the local
  session, the config probe and IMAP sign-in in one file. Some serverless plans cap the
  number of functions, so folding related routes into one handler is intentional.
- **Strings are localised** through the `I18N` dictionary and the `t(key)` helper in
  `app.js`. Add both the English and Bahasa Indonesia entries when you add a string.
- **Themes** are CSS custom properties with light, dark and high-contrast variants;
  style with the variables rather than hard-coded colours.

## Testing parsers

There is a small test suite under `app/test/`. Run it from `app/`:

```bash
npm test          # node test/parse.test.js
```

The samples live in `app/test/fixtures.js` (all synthetic, no real account data),
and `parse.test.js` asserts balances, transaction amounts and signs, and the year
resolution. Add a fixture and a case whenever you add or change a pattern.

The modules need no browser or real PDF: the PDF library is imported lazily, so you
can also poke at them directly in Node:

```bash
node -e "console.log(require('./app/lib/statements').detectAccounts('Closing Balance IDR 1,500,000.00'))"
node -e "console.log(require('./app/lib/parse').parseAlert('alerts@bank.example','Transaksi','Rp 50.000 ... '))"
```

## Before you commit

- Run `node --check` on any server file you touched (`node --check app/app.js`, etc.).
- Editing `app/app.js` or `app/index.html`: they are large single files with some
  multi-byte characters (icons, currency glyphs). Prefer targeted edits over wholesale
  rewrites, and confirm the file still parses afterward.
- If you changed a request handler, hit it once locally and check the JSON.

## Releasing a build

There is nothing to