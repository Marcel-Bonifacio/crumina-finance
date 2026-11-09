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
- Ports 80 and 443 open if you terminate TLS on the box. The Node app listens on port
  3000 on all interfaces. Under Docker Compose that port is not published, so only
  Caddy reaches it. With systemd or bare Node, block 3000 at the firewall and reach the
  app only through the proxy.

## Get the code

```bash
git clone https://github.com/Marcel-Bonifacio/crumina-finance.git
cd crumina-finance
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

## Option A: Docker Compose (recommended)

This brings up the app plus Caddy, which obtains and renews a Let's Encrypt
certificate for you.

```bash
cd selfhost
# edit Caddyfile: replace crumina.example.com with your hostname
docker compose up -d --build
```

The image holds no secrets: `.dockerignore` keeps `.env` files out of the build, and
only `app/` and `selfhost/server.js` are copied in. The variables have to be supplied
when the container starts. Compose does this with `env_file`; without Compose, use
`docker run --env-file selfhost/.env`. The app runs as the `node` user, and Compose
also gives it a read-only root filesystem (with a `tmpfs` on `/tmp`), drops all
capabilities and sets `no-new-privileges`.

Older versions of the Dockerfile copied the whole `selfhost/` folder, so an image built
with one of them while `selfhost/.env` existed contains your secrets. If such an image
was pushed to a registry or shared, rotate `SESSION_SECRET`, `TOKEN_ENC_KEY` and
`GOOGLE_CLIENT_SECRET`, and delete the image.

Point your hostname's DNS at the server's public IP. Once DNS resolves, Caddy issues
the certificate on first request. Update later with:

```bash
cd crumina-finance && git pull && cd selfhost && docker compose up -d --build
```

## Option B: systemd and Caddy (no Docker)

```bash
# create a dedicated user and install the code under /opt
sudo useradd -r -d /opt/crumina -s /usr/sbin/nologin crumina
sudo git clone https://github.com/Marcel-Bonifacio/crumina-finance.git /opt/crumina
sudo chown -R crumina:crumina /opt/crumina
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
sudo cp selfhost/Caddyfile /etc/caddy/Caddyfile
# edit /etc/caddy/Caddyfile: set your hostname, and change "reverse_proxy app:3000"
# to "reverse_proxy 127.0.0.1:3000" ("app" only resolves inside Docker Compose)
sudo systemctl restart caddy
```

`selfhost/deploy.sh` does the update cycle (git pull, `npm ci`, restart).

## Option C: bare Node (for development or your own platform)

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
so they are only sent over HTTPS; sign-in will not work over plain HTTP except on
`localhost`.

Rate limits are counted per endpoint and client IP, and the app reads the client IP
from `X-Real-IP` (falling back to the last `X-Forwarded-For` entry). The proxy must
set or overwrite `X-Real-IP` rather than pass on whatever the client sent. The included `Caddyfile` does this with
`header_up X-Real-IP {remote_host}`; in nginx, add:

```nginx
proxy_set_header X-Real-IP $remote_addr;
```

By default the app trusts the header only when the proxy connects from a loopback or
private address. If your proxy reaches the app from a public IP, set `TRUST_PROXY=1`
(see [CONFIGURATION.md](CONFIGURATION.md#runtime-optional)); without it, every request
is counted against the proxy's own address.

## Verify

Before switching production DNS, test against the IP directly:

```bash
curl --resolve crumina.example.com:443:YOUR_IP https://crumina.example.com/login   # 200
curl --resolve crumina.example.com:443:YOUR_IP https://crumina.example.com/api/data # 401 (not signed in)
curl --resolve crumina.example.com:443:YOUR_IP https://crumina.example.com/healthz  # ok
```

Then point DNS at the server, open the site, and sign in.

## Updating

```bash
cd crumina-finance && git pull       # /opt/crumina for the systemd setup
cd app && npm ci --omit=dev          # only if dependencies changed
# Docker:   cd ../selfhost && docker compose up -d --build
# systemd:  sudo systemctl restart crumina
```

## Backup

There is no database. A backup is the repository plus your `.env`. Keep `.env`
somewhere safe and private; with it, a fresh clone is a full restore.

## Hardening

- Expose only 80 and 443. The Node app listens on port 3000 on all interfaces, so
  block 3000 at the firewall and reach it only through the proxy (under Docker
  Compose the port is not published on the host).
- Keep secrets in `.env` (gitignored) or the systemd `EnvironmentFile`, readable
  only by the service user.
- The server sends the same CSP, HSTS and `X-Frame-Options` headers as the managed
  build. Keep them.
- Patch the OS, run a firewall (`ufw` allowing 22/80/443), and consider `fail2ban`.
- If you host other apps on the same machine, run them as separate users so a
  vulnerability elsewhere cannot read Crumina's `.env`.

See [SECURITY.md](SECURITY.md) for the full security model.
