# Crumina, application package

This directory is the deployable Crumina web app: a static front end
(`index.html`, `app.js`, the service worker and web manifest), serverless API
handlers under `api/`, and shared server-side logic under `lib/`. The client has
no build step, the files here are what ships.

Most documentation lives at the repository root:

- Overview and features: [`../README.md`](../README.md)
- Architecture: [`../docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md)
- Configuration and environment variables: [`../docs/CONFIGURATION.md`](../docs/CONFIGURATION.md)
- Running your own instance: [`../docs/SELF-HOSTING.md`](../docs/SELF-HOSTING.md)

## Layout

- `index.html`, `app.js`, `sw.js`, `manifest.webmanifest`: the client
- `login.html`, `login.js`, `home.html`, `privacy.html`, `terms.html`: public pages
- `api/`: request handlers, one file per endpoint, written in Node `(req, res)` style
- `lib/`: session, crypto, the mailbox abstraction, statement/alert parsing, rate limiting
- `lib/banks.js`: the institution registry; add or remove a bank he
