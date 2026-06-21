# Configuration

Crumina reads its configuration from environment variables. Nothing is hard-coded,
and the app degrades sensibly when an optional variable is missing, for example,
the Google button disappears if no Google client is configured, and the IMAP option
only appears when IMAP is enabled.

When self-hosting, the variables are read from `selfhost/.env` (loaded by
`server.js`) or from the process environment (systemd `EnvironmentFile`, Docker
`env_file`, or your platform's dashboard). Values already in the environment are
never overwritten by the `.env` file.

## Required

| Variable | What it is | How to generate |
|---|---|---|
| `SESSION_SECRET` | Secret used to sign the session cookie (HMAC-SHA256). | `openssl rand -base64 48` |
| `TOKEN_ENC_KEY` | 32-byte key, base64-encoded, used to encrypt stored secrets (Google tokens, IMAP credentials, statement passwords) with AES-256-GCM. | `openssl rand -base64 32` |

`TOKEN_ENC_KEY` must decode to exactly 32 bytes; the app throws at startup if it
does not. Treat both values as secrets. If you rotate them, existing sessions and
any stored tokens stop working and users sign in again, nothing else breaks,
because there is no other persistent state.

## Optional, Google sign-in

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

## Optional, IMAP sign-in (self-host)

| Variable | Effect |
|---|---|
| `IMAP_ENABLED` | Set to `1` to show "Connect your email (IMAP)" on the login page. |

With IMAP enabled, anyone using your instance can sign in with Gmail, Outlook /
Office 365, iCloud, or any IMAP server using an app-specific password, no Google
OAuth required. Credentials are validated on connect, then stored AES-256-GCM
encrypted in an `HttpOnly` cookie; there is no credentials database. The cookie
needs HTTPS (or `localhost`) to be accepted. The managed deployment does not enable
this; it is meant for instances you run yourself.

## Optional, runtime

| Variable | Default | Effect |
|---|---|---|
| `PORT` | `3000` | Port the self-host `server.js` listens on. |

## Replace before you run a public instance

A few values still carry placeholder identity from the open-source template. Replace
them before you operate Crumina as a public service, both for correctness and to
satisfy the AGPL's source-offer requirement.

| Where | Placeholder | Replace with |
|---|---|---|
| `app/privacy.html` | `crumina.example.com`, `privacy@example.com` | your domain and contact email |
| `app/terms.html` | `crumina.example.com`, `privacy@example.com` | your domain and contact email |
| `app/app.js` | `REPO = 'https://github.com/your-org/crumina'` | the URL where you publish your source |
| `brand/`, `app/brand/` | the Crumina logo and icons | your own marks, if you rebrand |

The repository link matters under the AGPL: if you modify Crumina and run it as a
network service, you must offer users the source of your modified version, and that
"view source" link is how they reach it.

## How the app discovers what is configured

The login page calls `GET /api/guest?config=1`, which returns:

```json
{ "imap": true, "google": true }
```

`google` is true when a client ID and redirect URI are present; `imap` is true when
`IMAP_ENABLED` is set. The front end shows or hides the matching sign-in options, so
you never see a button for a method the instance cannot perform.
