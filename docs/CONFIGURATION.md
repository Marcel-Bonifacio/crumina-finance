# Configuration

Crumina reads most of its configuration from environment variables. When an optional
variable is missing, the app hides the matching feature: the Google button disappears
if no Google client is configured, and the IMAP option only appears when IMAP is
enabled. A few values are still set in source; see
[Replace before you run a public instance](#replace-before-you-run-a-public-instance)
below.

When self-hosting, the variables are read from `selfhost/.env` (loaded by
`server.js`) or from the process environment (systemd `EnvironmentFile`, Docker
`env_file`, or your platform's dashboard). Values already in the environment are
never overwritten by the `.env` file.

## Required

| Variable | What it is | How to generate |
|---|---|---|
| `SESSION_SECRET` | Secret used to sign the session cookie (HMAC-SHA256). | `openssl rand -base64 48` |
| `TOKEN_ENC_KEY` | 32-byte key, base64-encoded, used to encrypt stored secrets (Google tokens, IMAP credentials, statement passwords) with AES-256-GCM. | `openssl rand -base64 32` |

`TOKEN_ENC_KEY` must decode to exactly 32 bytes. This is checked when a token is first
encrypted or decrypted, not at startup, so a bad key shows up as a failed Google
sign-in, IMAP connect or statement unlock, with `TOKEN_ENC_KEY must be 32 bytes
(base64)` in the server log. Treat both values as secrets. If you rotate them,
existing sessions and stored tokens stop working and users have to sign in again.
Nothing else breaks, because there is no other persistent state.

## Google sign-in (optional)

Set all three to enable the "Sign in with Google" button. Leave them unset to run
Crumina without Google (local mode and, if enabled, IMAP still work).

| Variable | Example |
|---|---|
| `GOOGLE_CLIENT_ID` | `1234567890-abc.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | `GOCSPX-...` |
| `GOOGLE_REDIRECT_URI` | `https://crumina.example.com/api/auth/callback` |

In Google Cloud, create an OAuth web client, add the redirect URI above (matching
your domain), and request the `openid`, `email`, `profile` and
`gmail.readonly` scopes. For the Google Identity Services popup on `/login`, also
add your site (for example `https://crumina.example.com`) under **Authorized
JavaScript origins**; without it the login page falls back to the redirect flow.
For the Android app's in-app sign-in, create an **Android** OAuth client in the
same project with package `id.tirtawijata.crumina` and your release signing
key's SHA-1, and set `GOOGLE_WEB_CLIENT_ID` in `android/app/build.gradle.kts`
to the web client's ID. Until the app passes Google's verification you can add up
to 100 test users. Crumina only ever requests read-only Gmail access.

## IMAP sign-in (email + app password)

| Variable | Effect |
|---|---|
| `IMAP_ENABLED` | Set to `1` to offer "Sign in with email" on the login page and in the Android app. |
| `IMAP_ALLOW_CUSTOM` | Set to `1` to also allow any IMAP server ("Other IMAP server"). Without it, only the built-in providers in `app/lib/imapProviders.js` are allowed: Gmail, Outlook / Hotmail, iCloud, Yahoo, AOL, Zoho, Fastmail, GMX, Yandex. |
| `IMAP_ALLOW_PRIVATE` | Set to `1` to allow IMAP hosts that resolve to private, loopback or link-local addresses (a mail server on your LAN). Any other value leaves it off. |

Users sign in with their email address and an **app password** from their provider
(their normal password won't work; Gmail needs 2-Step Verification on to create one).
The provider is picked from the address. No Google OAuth is involved, so there is no
"unverified app" screen and no 7-day token expiry. Credentials are checked on connect
(spaces in app passwords like `abcd efgh ijkl mnop` are tried with and without), then
encrypted with AES-256-GCM: on the web in an `HttpOnly` cookie, in the Android app in its
encrypted store, sent back as the `X-Cr-Imap` header. The server only uses them for the
session with the same email address. There is no credentials database.

The managed deployment enables IMAP with the built-in providers only. A public, pre-login
endpoint that connects to any host would let anyone use the server to probe or
brute-force other mail servers; keep `IMAP_ALLOW_CUSTOM` for instances you run for yourself.
A custom server must accept TLS on port 993 and must not resolve to a private address
(unless `IMAP_ALLOW_PRIVATE` is set); otherwise the user is sent back to `/login?imap=host`.

## Runtime (optional)

| Variable | Default | Effect |
|---|---|---|
| `PORT` | `3000` | Port the self-host `server.js` listens on. |
| `TRUST_PROXY` | unset (auto) | Whether to take the client IP for rate limiting from `X-Real-IP` (or, failing that, the last `X-Forwarded-For` entry). Unset: trust those headers only on Vercel or when the connection comes from a loopback or private address, such as Caddy or nginx on the same host or network. `1`: always trust them. `0`: never, and use the socket address. |

Set `TRUST_PROXY=1` only when every request reaches the app through a proxy that
overwrites `X-Real-IP`, for example a proxy that connects from a public IP. Otherwise
a client can send its own header and dodge the per-IP limits. See
[SELF-HOSTING.md](SELF-HOSTING.md#tls-and-reverse-proxy).

## Replace before you run a public instance

A few values still carry placeholder identity from the open-source template. Replace
them before you operate Crumina as a public service, for correctness and so users
know who operates the instance.

| Where | Current value | Replace with |
|---|---|---|
| `app/privacy.html`, `app/terms.html` | `crumina.tirtawijata.com` and `contact@tirtawijata.com` (the maintainer's domain and address) | your domain and contact email |
| `app/app.js` | `REPO = 'https://github.com/Marcel-Bonifacio/crumina-finance'` | the URL where you publish your source |
| `app/api/auth/callback.js` | `APP_LINK = 'https://crumina.tirtawijata.com/auth/android'` | your domain, if you ship your own Android build |
| `android/app/build.gradle.kts`, `AndroidManifest.xml`, `network_security_config.xml` | `API_BASE` and the App Link host, both `crumina.tirtawijata.com` | your domain, if you ship your own Android build |
| `app/.well-known/assetlinks.json` | package `id.tirtawijata.crumina` and its signing fingerprint | your package id and signing key, if you ship your own Android build |
| `brand/`, `app/brand/` | the Crumina logo and icons | your own marks, if you rebrand |

The "view source" link should point at the code you actually run.

## How the app discovers what is configured

The login page calls `GET /api/guest?config=1`, which returns:

```json
{ "imap": true, "google": true }
```

`google` is true when a client ID and redirect URI are present; `imap` is true when
`IMAP_ENABLED` is set. The front end shows or hides the matching sign-in options, so
you never see a button for a method the instance cannot perform.
