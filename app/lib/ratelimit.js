// Lightweight in-memory rate limiter. Per-process (best-effort on serverless; fully
// effective on a single self-hosted instance). Keyed by client IP.
const hits = new Map();
function rateLimit(req, res, opts) {
  const max = (opts && opts.max) || 20, windowMs = (opts && opts.windowMs) || 60000;
  const ip = String(req.headers['x-forwarded-for'] || '').split(',')[0].trim()
    || (req.socket && req.socket.remoteAddress) || 'unknown';
  const now = Date.now(); const e = hits.get(ip);
  if (!e || now > e.reset) { if (hits.size > 5000) hits.clear(); hits.set(ip, { n: 1, reset: now + windowMs }); return true; }
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
