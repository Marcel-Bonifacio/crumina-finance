// Lightweight in-memory rate limiter. Per-process (best-effort on serverless; fully
// effective on a single self-hosted instance). Keyed by endpoint + client IP.
const net = require('net');
const hits = new Map(), CAP = 5000;
// X-Real-IP / X-Forwarded-For are only trusted from a proxy in front of us: on Vercel, or when the
// peer is loopback/private (Caddy, nginx). TRUST_PROXY=1 always trusts them, TRUST_PROXY=0 never.
function privatePeer(a) { a = String(a || '').replace(/^::ffff:/i, ''); return a === '::1' || /^(127|10)\./.test(a) || /^192\.168\./.test(a) || /^172\.(1[6-9]|2\d|3[01])\./.test(a) || /^169\.254\./.test(a) || /^f[cd]/i.test(a) || /^fe80:/i.test(a); }
function clientIp(req) {
  const peer = (req.socket && req.socket.remoteAddress) || 'unknown', tp = process.env.TRUST_PROXY;
  if (tp === '0' || (tp !== '1' && !process.env.VERCEL && !privatePeer(peer))) return peer;
  const xff = String(req.headers['x-forwarded-for'] || '').split(',').map(s => s.trim()).filter(Boolean);
  const ip = String(req.headers['x-real-ip'] || '').trim() || xff[xff.length - 1] || '';
  return net.isIP(ip) ? ip : peer;
}
// One counter per endpoint, so cheap calls (e.g. /api/yf) don't use up the budget of others.
function route(req) { let p = String(req.url || '').split('?')[0]; try { p = decodeURIComponent(p); } catch (e) {} return p.replace(/\/+/g, '/').replace(/\/+$/, '').toLowerCase(); }
function rateLimit(req, res, opts) {
  const max = (opts && opts.max) || 20, windowMs = (opts && opts.windowMs) || 60000;
  const key = ((opts && opts.name) || route(req)) + '|' + clientIp(req);
  const now = Date.now(); const e = hits.get(key);
  if (!e || now > e.reset) {
    hits.delete(key); // re-insert, so Map order follows reset time
    for (const [k, v] of hits) { if (v.reset > now && hits.size < CAP) break; hits.delete(k); } // drop expired, then oldest if still full
    hits.set(key, { n: 1, reset: now + windowMs }); return true;
  }
  e.n++;
  if (e.n > max) {
    res.statusCode = 429;
    res.setHeader('Retry-After', Math.ceil((e.reset - now) / 1000));
    res.setHeader('Content-Type', 'application/json');
    res.end('{"error":"rate_limited"}');
    return false;
  }
  return true;
}
module.exports = { rateLimit };
