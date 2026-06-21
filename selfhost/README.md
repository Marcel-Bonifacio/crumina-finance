# selfhost/

Deployment artifacts for running Crumina on your own infrastructure. The full guide,
with step-by-step instructions for Docker, systemd and bare Node, is in
[`../docs/SELF-HOSTING.md`](../docs/SELF-HOSTING.md).

## What's here

- `server.js`: a small Node server (built-ins only) that serves `app/`, mounts the
  `app/api/*.js` handlers, applies the session gate, and sets the security headers.
  It is what makes the app run anywhere, not just on a serverless platform.
- `Dockerfile`, `docker-compose.yml`: container build; Compose also runs Caddy for
  automatic HTTPS.
- `Caddyfile`: reverse proxy with automatic Let's Encrypt certificates. Edit the
  hostname before use.
- `crumina.service`, `deploy.sh`: systemd unit and update script for a no-Docker
  setup.
- `.env.example`: environment template. Copy to `.env` (gitignored) and fill in.

## Quick start

```bash
cp .env.example .env        # set SESSION_SECRET and TOKEN_ENC_KEY
docker compose up -d --build
```

See [`../docs/CONFIGURATION.md`](../docs/CONFIGURATION.md) for every variable and
[`../docs/SELF-HOSTING.md`](../docs/SELF-HOSTING.md) for the other deployment paths.

