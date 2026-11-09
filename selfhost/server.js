// Crumina self-host server (any Node host).
// Runs the SAME app/ that the Vercel deployment serves: static frontend + the app/api/*.js handlers
// (which are written in Node (req,res) style) + the session gate that app/middleware.js
// (Vercel edge middleware) applies. No framework, only Node built-ins. Node 18+ (for global fetch).
const http = require('http'), fs = require('fs'), path = require('path'), url = require('url');
const APP = path.join(__dirname, '..', 'app');

// Load selfhost/.env (KEY=VALUE) if present; never overrides values already in the environment.
try {
  fs.readFileSync(path.join(__dirname, '.env'), 'utf8').split(/\r?\n/).forEach(function (l) {
    const m = l.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/);
    if (m && process.env[m[1]] === undefined) process.env[m[1]] = m[2].replace(/^["']|["']$/g, '');
  });
} catch (e) {}

const PORT = process.env.PORT || 3000;
const { fromReq } = require(path.join(APP, 'lib', 'session'));

const MIME = { '.html': 'text/html; charset=utf-8', '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css', '.json': 'application/json', '.webmanifest': 'application/manifest+json',
  '.svg': 'image/svg+xml', '.png': 'image/png', '.jpg': 'image/jpeg', '.ico': 'image/x-icon', '.txt': 'text/plain' };
// Security headers: same as the response headers in app/vercel.json, so behaviour matches production.
const SEC = {
  'X-Frame-Options': 'DENY', 'X-Content-Type-Options': 'nosniff',
  'Referrer-Policy': 'strict-origin-when-cross-origin',
  'Strict-Transport-Security': 'max-age=63072000; includeSubDomains; preload',
  'Content-Security-Policy': "default-src 'self'; img-src 'self' data: https://*.googleusercontent.com https://t1.gstatic.com https://www.google.com; script-src 'self' 'wasm-unsafe-eval'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'; manifest-src 'self'; worker-src 'self' blob:"
};
function sec(res) { Object.keys(SEC).forEach(function (k) { res.setHeader(k, SEC[k]); }); }
function serveFile(res, file, noStore) {
  fs.readFile(file, function (e, buf) {
    if (e) { res.statusCode = 404; sec(res); return res.end('Not found'); }
    sec(res);
    res.setHeader('Content-Type', MIME[path.extname(file).toLowerCase()] || 'application/octet-stream');
    if (noStore) res.setHeader('Cache-Control', 'no-store, must-revalidate');
    res.end(buf);
  });
}
// Public (no auth): keep in sync with the matcher exclusions in app/middleware.js.
function isPublic(p) {
  return p.startsWith('/api/') || p === '/login' || p === '/login.html' || p === '/home' || p === '/home.html' || p === '/privacy' || p === '/privacy.html' || p === '/terms' || p === '/terms.html' || p.startsWith('/brand/') || p.startsWith('/vendor/')
    || p === '/favicon.ico' || p === '/manifest.webmanifest' || p === '/sw.js' || p === '/app.js' || p === '/login.js' || p.startsWith('/icon-')
    || p.startsWith('/.well-known/') || p.startsWith('/auth/android');
}
// Log, never crash: an unhandled rejection would otherwise take the whole server down.
process.on('unhandledRejection', function (e) { console.error('unhandledRejection', e); });

http.createServer(async function (req, res) {
  try {
    let pathname = null;
    try { pathname = decodeURIComponent(url.parse(req.url).pathname || '/'); } catch (e) {}
    // Route and authorize canonical paths only: no dot segments (incl. decoded %2e/%2f), '//', backslashes or NUL.
    if (!pathname || pathname.includes('\0') || pathname.includes('\\') || path.posix.normalize(pathname) !== pathname) { res.statusCode = 400; sec(res); return res.end('Bad request'); }
    if (pathname === '/healthz') { res.statusCode = 200; res.setHeader('Content-Type', 'text/plain'); return res.end('ok'); }
    // API -> the existing serverless handlers (each enforces its own auth)
    if (pathname.startsWith('/api/')) {
      const f = path.join(APP, pathname.replace(/\/+$/, '') + '.js');
      if (!f.startsWith(path.join(APP, 'api') + path.sep) || !fs.existsSync(f)) { res.statusCode = 404; sec(res); return res.end('Not found'); }
      sec(res);
      return await require(f)(req, res);
    }
    // Clean URL: /login (and /login.html) -> login page
    if (pathname === '/login' || pathname === '/login.html') return serveFile(res, path.join(APP, 'login.html'), true);
    // Clean URLs for public legal/landing pages
    if (pathname === '/privacy' || pathname === '/terms' || pathname === '/home') return serveFile(res, path.join(APP, pathname.slice(1) + '.html'), true);
    // Android App Link fallback page (public, as on Vercel)
    if (pathname === '/auth/android' || pathname === '/auth/android.html') return serveFile(res, path.join(APP, 'auth', 'android.html'), true);
    // Static public assets
    if (pathname !== '/' && isPublic(pathname)) {
      const f = path.join(APP, pathname);
      if ((f === APP || f.startsWith(APP + path.sep)) && fs.existsSync(f) && fs.statSync(f).isFile()) return serveFile(res, f, pathname === '/app.js');
      res.statusCode = 404; sec(res); return res.end('Not found');
    }
    // Gated app shell (everything else, incl. "/")
    if (!fromReq(req)) { res.statusCode = 302; sec(res); res.setHeader('Location', '/login'); return res.end(); }
    return serveFile(res, path.join(APP, 'index.html'), true);
  } catch (e) {
    console.error('server', e);
    if (!res.headersSent) { res.statusCode = 500; sec(res); res.end('Server error'); } else if (!res.writableEnded) res.destroy();
  }
}).listen(PORT, function () { console.log('Crumina self-host listening on :' + PORT); });
