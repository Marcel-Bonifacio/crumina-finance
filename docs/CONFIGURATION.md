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
`gmail.readonly` scopes. Until the app passes Google's verification you can add up
to 100 test users. Crumina only ever requests read-only Gmail access.

## IMAP sign-in (optional, self-host)

| Variable | Effect |
|---|---|
| `IMAP_ENABLED` | Set to `1` to show the "or connect your email" options (Outlook.com, iCloud Mail, or any other IMAP server) on the login page. |
| `IMAP_ALLOW_PRIVATE` | Set to `1` to allow IMAP hosts that resolve to private, loopback or link-local addresses (a mail server on your LAN). Any other value leaves it off. |

With IMAP enabled, anyone using your instance can sign in with Gmail, Outlook /
Office 365, iCloud, or any IMAP server using an app-specific password. No Google
OAuth is required. Credentials are validated on connect, then stored AES-256-GCM
encrypted in an `HttpOnly` cookie; there is no credentials database. The cookie
needs HTTPS (or `localhost`) to be accepted. The managed deployment does not enable
this; it is meant for instances you run yourself.

A custom IMAP server must accept TLS on port 993; the sign-in refuses any other port.
Because the sign-in form is reachable before login, it also refuses hosts that resolve
to private, loopback, link-local or reserved addresses unless `IMAP_ALLOW_PRIVATE` is
set. Both cases send the user back to `/login?imap=host`.

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

Some values in the source belong to the maintainer's own deployment or are still
placeholders. Replace them before you operate Crumina as a public service, both for
correctness and to satisfy the AGPL's source-offer requirement.

| Where | Current value | Replace with |
|---|---|---|
| `app/privacy.html`, `app/terms.html` | `crumina.tirtawijata.com` and `contact@tirtawijata.com` (the maintainer's domain and address) | your domain and contact email |
| `app/app.js` | `REPO = 'https://github.com/Marcel-Bonifacio/crumina-finance'` | the URL where you publish your source |
| `app/api/auth/callback.js` | `APP_LINK = 'https://crumina.tirtawijata.com/auth/android'` | your domain, if you ship your own Android build |
| `android/app/build.gradle.kts`, `AndroidManifest.xml`, `network_security_config.xml` | `API_BASE` and the App Link host, both `crumina.tirtawijata.com` | your domain, if you ship your own Android build |
| `app/.well-known/assetlinks.json` | package `id.tirtawijata.crumina` and its signing fingerprint | your package id and signing key, if you ship your own Android build |
| `brand/`, `app/brand/` | the Crumina logo and icons | your own marks, if you rebrand |

The repository link matters under the AGPL: if you modify Crumina and run it as a
network service, you must offer users the source of your modified version, and the
"view source" link in the app opens `REPO`.

## How the app discovers what is configured

The login page calls `GET /api/guest?config=1`, which returns:

```json
{ "imap": true, "google": true }
```

`google` is true when a client ID and redirect URI are present; `imap` is true when
`IMAP_ENABLED` is set. The front end shows or hides the matching sign-in options, so
you never see a button for a method the instance cannot perform.
