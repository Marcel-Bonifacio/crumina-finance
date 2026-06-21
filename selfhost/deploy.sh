#!/usr/bin/env bash
# Pull latest + restart (systemd path). Run on the VPS.
set -e
cd "$(dirname "$0")/.."        # repo root
git pull --ff-only
( cd app && npm ci --omit=dev )
sudo systemctl restart crumina
echo "deployed $(git rev-parse --short HEAD)"
