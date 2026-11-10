// Local auth endpoint (one serverless function to stay within plan limits):
//   GET  /api/guest            -> issue a local/guest session (no Google)
//   GET  /api/guest?client=android -> the same session as JSON {token} for the native app's local mode
//   GET  /api/guest?config=1   -> public: which auth methods this deployment supports
//   POST /api/guest            -> IMAP sign-in with an app password (gated by IMAP_ENABLED)
//   POST /api/guest?client=android -> the same, answered as JSON {ok, t, p, imap} for the native app
const { sign, cookie } = require('../lib/session');
const { encrypt } = require('../lib/crypto');
const { rateLimit } = require('../lib/ratelimit');
const { PROVIDERS, customAllowed, providerForEmail, publicList } = require('../lib/imapProviders');
// Pre-auth endpoint: the host is checked by netguard, which also pins the IP we connect to.
const { resolvePublic } = require('../lib/netguard');
function readBody(req){ return new Promise(function(resolve){ var b='',big=false; req.on('data',function(c){ b+=c; if(b.length>4096){ big=true; b=''; req.destroy(); } }); req.on('end',function(){ resolve(big?'':b); }); req.on('close',function(){ resolve(big?'':b); }); req.on('error',function(){ resolve(''); }); }); }
function back(res, code){ res.writeHead(302, { Location: '/login?imap=' + code }); res.end(); }
module.exports = async (req, res) => {
  const q = require('url').parse(req.url, true).query || {};

  // --- public config probe ---
  if (req.method === 'GET' && 'config' in q) {
    res.setHeader('Content-Type', 'application/json'); res.setHeader('Cache-Control', 'no-store');
    return res.end(JSON.stringify({ imap: !!process.env.IMAP_ENABLED, imapCustom: !!process.env.IMAP_ENABLED && customAllowed(), providers: process.env.IMAP_ENABLED ? publicList() : [], google: !!(process.env.GOOGLE_CLIENT_ID && process.env.GOOGLE_REDIRECT_URI), clientId: process.env.GOOGLE_CLIENT_ID || null }));
  }

  // --- IMAP sign-in with an app password ---
  if (req.method === 'POST') {
    if (!process.env.IMAP_ENABLED) { res.statusCode = 404; return res.end('Not found'); }
    const native = q.client === 'android';
    // Web answers with a redirect to /login?imap=<code>; the app gets JSON.
    const fail = native ? function (code) { res.statusCode = 200; res.setHeader('Content-Type', 'application/json'); res.setHeader('Cache-Control', 'no-store'); res.end(JSON.stringify({ ok: false, error: code })); } : function (code) { back(res, code); };
    const sfs = req.headers['sec-fetch-site'];
    if (sfs && sfs !== 'same-origin' && sfs !== 'none') { res.statusCode = 403; return res.end('Forbidden'); }
    const origin = req.headers['origin'];
    if (origin) { try { if (new URL(origin).host !== req.headers.host) { res.statusCode = 403; return res.end('Forbidden'); } } catch (e) { res.statusCode = 403; return res.end('Forbidden'); } }
    if (!rateLimit(req, res, { name: 'imap', max: 10 })) return;
    try {
      const params = new URLSearchParams(await readBody(req));
      const email = (params.get('email') || '').trim();
      const pass = params.get('password') || '';
      // App passwords are often shown in groups ("abcd efgh ijkl mnop"); IMAP wants them without spaces.
      const passNoSpaces = pass.replace(/\s+/g, '');
      let provider = (params.get('provider') || '').toLowerCase();
      if (!PROVIDERS[provider] || provider === 'auto') provider = providerForEmail(email) || provider;
      let host = (params.get('host') || '').trim(), port = parseInt(params.get('port') || '', 10);
      if (PROVIDERS[provider]) { host = PROVIDERS[provider].host; port = 993; }
      else if (!customAllowed()) return fail('provider'); // hosted: known providers only
      if (!email || !pass || !host || email.length > 254 || pass.length > 256) return fail('missing');
      // Also limit attempts per address, so the endpoint can't be used to guess someone's app password
      // from many IPs (per instance; on serverless a shared limiter such as the Vercel Firewall is stronger).
      if (!rateLimit(req, res, { name: 'imap-acct:' + email.toLowerCase(), max: 5, windowMs: 15 * 60000, key: 'acct' })) return;
      if (!port || isNaN(port)) port = 993;
      // Implicit TLS (secure: true) only works on 993, so that is the only port we connect to.
      const addr = port === 993 ? await resolvePublic(host) : null;
      if (!addr) return fail('host');
      const { ImapFlow } = require('imapflow');
      // Connect to the vetted address; TLS still verifies the certificate against the host name.
      async function tryLogin(p) {
        const client = new ImapFlow({ host: addr, servername: host, port, secure: true, auth: { user: email, pass: p }, logger: false, emitLogs: false, connectionTimeout: 10000, greetingTimeout: 10000 });
        try { await client.connect(); await client.logout(); return true; }
        catch (e) { try { client.close(); } catch (_) {} return false; }
      }
      let usePass = pass;
      if (!(await tryLogin(pass))) {
        if (passNoSpaces === pass || !(await tryLogin(passNoSpaces))) return fail('auth');
        usePass = passNoSpaces;
      }
      const creds = encrypt(JSON.stringify({ host, port, secure: true, user: email, pass: usePass }));
      const prof = Buffer.from(JSON.stringify({ email, name: (email.split('@')[0] || 'You'), picture: '' })).toString('base64url');
      if (native) {
        res.setHeader('Content-Type', 'application/json'); res.setHeader('Cache-Control', 'no-store');
        return res.end(JSON.stringify({ ok: true, t: sign(email), p: prof, imap: creds }));
      }
      res.setHeader('Set-Cookie', [
        cookie('tally_session', sign(email), 30 * 86400),
        cookie('tally_prof', prof, 30 * 86400),
        cookie('cr_imap', creds, 30 * 86400)
      ]);
      res.writeHead(302, { Location: '/' }); res.end();
    } catch (e) { console.error('imap connect', e && e.message); return fail('error'); }
    return;
  }

  // --- default: local/guest session ---
  if (!rateLimit(req, res, { name: 'guest', max: 20 })) return;
  // Native app (no cookies): the same guest session, returned as a Bearer token for local mode.
  if ('client' in q && q.client === 'android') {
    res.setHeader('Content-Type', 'application/json'); res.setHeader('Cache-Control', 'no-store');
    return res.end(JSON.stringify({ token: sign('guest') }));
  }
  const prof = Buffer.from(JSON.stringify({ email: 'guest', name: 'Local', picture: '' })).toString('base64url');
  res.setHeader('Set-Cookie', [ cookie('tally_session', sign('guest'), 30 * 86400), cookie('tally_prof', prof, 30 * 86400) ]);
  res.writeHead(302, { Location: '/' }); res.end();
};
