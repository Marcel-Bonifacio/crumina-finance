# Self-hosting

Crumina runs anywhere Node 18+ runs. The `selfhost/` folder contains a small server
and ready-made deployment files, so you can put it on a VPS with Docker, run it under
systemd, or drop it onto any platform that hosts a Node process. This guide leads
with Docker because it is the least error-prone, then covers the alternatives.

The same `app/` directory that the managed platform serves is what you run here.
`selfhost/server.js` reproduces what a managed host does for you: it serves the
static front end, mounts each `app/api/*.js` handler, applies the session gate that
edge middleware would otherwise do, and sets the same security headers.

## What you need

- A host with Node 18 or newer (Node 18 is the floor because the server uses the
  built-in `fetch`). A 1 vCPU / 1 GB machine is enough.
- A domain name you control, if you want HTTPS and Google sign-in.
- The two required secrets, `SESSION_SECRET` and `TOKEN_ENC_KEY` (see
  [CONFIGURATION.md](CONFIGURATION.md)).
- Ports 80 and 443 open if you terminate TLS on the box. The Node app itself listens
  on `localhost:3000` behind a reverse proxy.

## Get the code

```bash
git clone https://github.com/your-org/crumina.git
cd crumina
```

## Configure

```bash
cp selfhost/.env.example selfhost/.env
# edit selfhost/.env, at minimum set SESSION_SECRET and TOKEN_ENC_KEY
```

Generate the secrets:

```bash
openssl rand -base64 48   # SESSION_SECRET
openssl rand -base64 32   # TOKEN_ENC_KEY
```

`selfhost/.env` is gitignored. Do not commit it. If you want Google sign-in, fill in
the three `GOOGLE_*` values and set the redirect URI to your domain; if you want IMAP
sign-in, set `IMAP_ENABLED=1`. See [CONFIGURATION.md](CONFIGURATION.md) for the full
list.

## Option A, Docker Compose (recommended)

This brings up the app plus Caddy, which obtains and renews a Let's Encrypt
certificate for you.

```bash
cd selfhost
# edit Caddyfile: replace crumina.example.com with your hostname
docker compose up -d --build
```

Point your hostname's DNS at the server's public IP. Once DNS resolves, Caddy issues
the certificate on first request. Update later with:

```bash
cd crumina && git pull && cd selfhost && docker compose up -d --build
```

## Option B, systemd and Caddy (no Docker)

```bash
# create a dedicated user and install the code under /opt
sudo useradd -r -m -d /opt/crumina crumina
sudo git clone https://github.com/your-org/crumina.git /opt/crumina
cd /opt/crumina

# Node 18+ and the app's dependencies
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs
sudo -u crumina bash -c 'cd app && npm ci --omit=dev'

# configure
sudo cp selfhost/.env.example selfhost/.env
sudo nano selfhost/.env

# run as a service (listens on :3000)
sudo cp selfhost/crumina.service /etc/systemd/system/crumina.service
sudo systemctl daemon-reload
sudo systemctl enable --now crumina

# TLS reverse proxy
sudo apt-get install -y caddy
sudo cp selfhost/Caddyfile /etc/caddy/Caddyfile   # edit the hostname first
sudo systemctl restart caddy
```

`selfhost/deploy.sh` does the update cycle (git pull, `npm ci`, restart).

## Option C, bare Node (for development or your own platform)

```bash
cd app && npm ci --omit=dev && cd ..
SESSION_SECRET=dev TOKEN_ENC_KEY=$(openssl rand -base64 32) node selfhost/server.js
```

The app serves on `http://localhost:3000`. For a platform that runs a Node process
(a PaaS, a container service, your own box), point its start command at
`node selfhost/server.js`, set the environment variables in the platform's
dashboard, and let the platform terminate TLS. `GET /healthz` returns `ok` for
health checks.

## TLS and reverse proxy

The app speaks plain HTTP on a local port; something in front terminates TLS. The
included `Caddyfile` does this with automatic certificates. If you already run nginx
or another proxy, forward your hostname to `127.0.0.1:3000` and keep the security
headers the app already sets. The session and credential cookies are marked `Secure`,
so they are only sent over HTTPS, sign-in will not work over plain HTTP except on
`localhost`.

## Verify

Before switching production DNS, test against the IP directly:

```bash
curl --resolve crumina.example.com:443:YOUR_IP https://crumina.example.com/login   # 200
curl --resolve crumina.example.com:443:YOUR_IP https://crumina.example.com/api/data # 401 (not signed in)
curl http://YOUR_IP:3000/healthz                                                    # ok
```

Then point DNS at the server, open the site, and sign in.

## Updating

```bash
cd crumina && git pull
cd app && npm ci --omit=dev          # only if dependencies changed
# Docker:   cd ../selfhost && docker compose up -d --build
# systemd:  sudo systemctl restart crumina
```

## Backup

There is no database. A backup is the repository plus your `.env`. Keep `.env`
somewhere safe and private; with it, a fresh clone is a full restore.

## Hardening

- Expose only 80 and 443; keep the Node app on `localhost:3000` behind the proxy.
- Keep secrets in `.env` (gitignored) or the systemd `EnvironmentFile`, readable
  only by the service user.
- The server sends the same CSP, HSTS and `X-Frame-Options` headers as the managed
  build. Keep them.
- Patch the OS, run a firewall (`ufw` allowing 22/80/443), and consider `fail2ban`.
- If you host other apps on the same machine, run them as separate users so a
  vulnerability elsewhere cannot read Crumina's `.env`.

See [SECURITY.md](SECURITY.md) for the full security model.
