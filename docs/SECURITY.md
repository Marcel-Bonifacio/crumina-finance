# Security

Crumina handles financial data, so the design tries to hold as little of it as
possible and to protect what it must hold. This document states the threat model,
the cryptography in use, and how to report a problem.

## Threat model

**What Crumina protects.** Your mailbox access (OAuth refresh token or IMAP
credentials), your statement passwords, and your session. These are the secrets that
would let someone read your financial mail or act as you.

**How it limits exposure.** The server keeps no database of your transactions. It
reads your mailbox on request, returns parsed results, and forgets them. Secrets that
must persist are encrypted before they are stored, and are only ever stored in
cookies on your own browser, never on a server disk.

**Out of scope.** Crumina cannot protect you from a compromised device or browser, a
malicious browser extension, a phishing site that imitates the login page, or a
hostile mail provider. If your `SESSION_SECRET` or `TOKEN_ENC_KEY` leaks, treat all
issued cookies as compromised and rotate both.

## Trust boundaries

- **Browser ↔ server.** All requests carry the signed session cookie; the server
  re-verifies it on every call. Cross-site requests to the IMAP sign-in endpoint are
  rejected.
- **Server ↔ mail provider.** Gmail over OAuth with a read-only scope, or IMAP over
  TLS. Crumina never sends, deletes, or modifies mail.
- **Server ↔ price and rate APIs.** Outbound, read-only, and free of user data.

## Cryptography

### Sessions

A session cookie is `base64url(JSON{email, issued-at}) + "." + HMAC-SHA256` over that
body, keyed by `SESSION_SECRET`. Verification recomputes the HMAC and compares it with
`crypto.timingSafeEqual`, so a wrong signature is rejected without leaking timing.
Sessions expire 30 days after they are issued.

### Secrets at rest

The Google refresh token, the per-bank statement passwords, and the IMAP credentials
are encrypted with **AES-256-GCM** before being written to a cookie. Each value uses a
fresh 12-byte random IV; the stored blob is `IV || auth-tag || ciphertext`, base64. The
key comes from `TOKEN_ENC_KEY`, which must decode to exactly 32 bytes, the app
refuses to start otherwise. GCM's authentication tag means a tampered cookie fails to
decrypt rather than yielding garbage.

### Cookies

| Cookie | Contents | Flags |
|---|---|---|
| `tally_session` | signed session | `HttpOnly; Secure; SameSite=Lax` |
| `tally_prof` | display profile (not a secret) | `HttpOnly; Secure; SameSite=Lax` |
| `tally_rt` | Google refresh token (encrypted) | `HttpOnly; Secure; SameSite=Lax` |
| `tally_pw` | statement passwords (encrypted) | `HttpOnly; Secure; SameSite=Lax` |
| `cr_imap` | IMAP credentials (encrypted) | `HttpOnly; Secure; SameSite=Lax` |

`HttpOnly` keeps the values out of JavaScript, `Secure` keeps them off plain HTTP, and
`SameSite=Lax` limits cross-site sending.

## Authentication paths

- **Google OAuth.** The flow uses a `state` nonce stored in a short-lived cookie and
  checked on return, which blocks login-CSRF. A verified Google email is required. The
  only Gmail scope requested is `gmail.readonly`.
- **IMAP.** Enabled only when the operator sets `IMAP_ENABLED`. The sign-in endpoint
  rejects cross-site requests (`Sec-Fetch-Site` and `Origin` checks), is rate-limited,
  and caps the request body. Credentials are verified with a live IMAP connection
  before being accepted, then encrypted into a cookie. Use an app-specific password,
  not your main account password.

## Application hardening

- **Content-Security-Policy.** `default-src 'self'`; scripts limited to `'self'` plus
  `'wasm-unsafe-eval'` (for the OCR WebAssembly); no inline event handlers (the client
  uses event delegation); framing denied. Sent on every response, both on the managed
  platform and from the self-host server.
- **Other headers.** HSTS with a long max-age, `X-Frame-Options: DENY`,
  `X-Content-Type-Options: nosniff`, and a strict referrer policy.
- **Output escaping.** Values rendered into the DOM are escaped, so a merchant name or
  statement field cannot inject markup.
- **PDF parsing.** `pdfjs-dist` runs with `isEvalSupported: false`, fonts and system
  fonts disabled, and only text extraction, no page rendering. Uploaded PDFs are
  capped at 4 MB.
- **Rate limiting.** Per-IP throttling on the sign-in, sync, statement, upload and
  price endpoints.
- **On-device OCR.** Receipt images are processed in the browser with a bundled
  Tesseract build; the image is never uploaded.

## Third-party calls

| Destination | Why | Carries user data? |
|---|---|---|
| Google OAuth and Gmail API | Sign-in and reading your mail | Your mailbox, read-only |
| Your IMAP server | Reading your mail | Your mailbox, read-only |
| Yahoo Finance (via `/api/yf`) | Ticker search and prices | No, only the symbol you look up |
| `open.er-api.com` (via `/api/fx`) | Currency conversion | No |

## Operator responsibilities

- Set strong, unique `SESSION_SECRET` and `TOKEN_ENC_KEY` values and keep them secret.
- Terminate TLS in front of the app; the `Secure` cookies require HTTPS.
- Replace the placeholder domain, contact email and repository link before running a
  public instance (see [CONFIGURATION.md](CONFIGURATION.md)).
- Keep the host and dependencies patched.

## Reporting a vulnerability

Please report security issues privately rather than opening a public issue. Email the
contact listed in your instance's privacy page (the upstream template uses
`security@example.com` as a placeholder, replace it with your real contact). Include
the steps to reproduce and the affected version or commit. We aim to acknowledge a
report within a few days and to credit reporters who want it once a fix ships.
