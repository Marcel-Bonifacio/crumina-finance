// Outbound-connection guard for user-supplied hosts (IMAP sign-in and every later IMAP sync).
// Resolve once, refuse loopback/private/link-local/multicast/reserved addresses, and hand back
// the vetted IP so the caller connects to exactly that address. Connecting by name instead would
// resolve again, and a DNS-rebinding server could answer the second lookup with an internal IP.
// IMAP_ALLOW_PRIVATE=1 allows a mail server on the LAN (self-host).
const net = require('net');
const BLOCKED = new net.BlockList();
[['0.0.0.0', 8], ['10.0.0.0', 8], ['100.64.0.0', 10], ['127.0.0.0', 8], ['169.254.0.0', 16], ['172.16.0.0', 12], ['192.0.0.0', 24], ['192.168.0.0', 16], ['198.18.0.0', 15], ['224.0.0.0', 3]]
  .forEach(function (s) { BLOCKED.addSubnet(s[0], s[1], 'ipv4'); });
[['::', 127], ['fc00::', 7], ['fe80::', 10], ['ff00::', 8], ['64:ff9b::', 96], ['64:ff9b:1::', 48], ['2002::', 16]]
  .forEach(function (s) { BLOCKED.addSubnet(s[0], s[1], 'ipv6'); });

function blocked(addr) {
  const ip = String(addr).replace(/^::ffff:(?=\d+\.\d+\.\d+\.\d+$)/i, '');
  return BLOCKED.check(ip, net.isIPv6(ip) ? 'ipv6' : 'ipv4');
}

/** Returns an address safe to connect to for `host`, or null. Every resolved address must be public. */
async function resolvePublic(host) {
  host = String(host || '').trim();
  if (!host || host.length > 253) return null;
  if (process.env.IMAP_ALLOW_PRIVATE === '1') return host;
  try {
    const a = net.isIP(host) ? [{ address: host }] : await require('dns').promises.lookup(host, { all: true, verbatim: true });
    if (!a.length || a.some(function (x) { return blocked(x.address); })) return null;
    return a[0].address;
  } catch (e) { return null; }
}

module.exports = { resolvePublic, blocked };
