// Local auth endpoint (one serverless function to stay within plan limits):
//   GET  /api/guest            -> issue a local/guest session (no Google)
//   GET  /api/guest?config=1   -> public: which auth methods this deployment supports
//   POST /api/guest            -> IMAP sign-in (self-host only; gated by IMAP_ENABLED)
const { sign, cookie } = require('../lib/session');
const { encrypt } = require('../lib/crypto');
const { rateLimit } = require('../lib/ratelimit');
const PRESETS = { gmail:{host:'imap.gmail.com',port:993}, outlook:{host:'outlook.office365.com',port:993}, icloud:{host:'imap.mail.me.com',port:993} };
function readBody(req){ return new Promise(function(resolve){ var b=''; req.on('data',function(c){ b+=c; if(b.length>4096){ b=b.slice(0,4096); req.destroy(); } }); req.on('end',function(){ resolve(b); }); req.on('error',function(){ resolve(''); }); }); }
function back(res, code){ res.writeHead(302, { Location: '/login?imap=' + code }); res.end(); }
module.exports = async (req, res) => {
  const q = require('url').parse(req.url, true).query || {};

  // --- public config probe ---
  if (req.method === 'GET' && 'config' in q) {
    res.setHeader('Content-Type', 'application/json'); res.setHeader('Cache-Control', 'no-store');
    return res.end(JSON.stringify({ imap: !!process.env.IMAP_ENABLED, google: !!(process.env.GOOGLE_CLIENT_ID && process.env.GOOGLE_REDIRECT_URI) }));
  }

  // --- IMAP sign-in (self-host only) ---
  if (req.method === 'POST') {
    if (!process.env.IMAP_ENABLED) { res.statusCode = 404; return res.end('Not found'); }
    const sfs = req.headers['sec-fetch-site'];
    if (sfs && sfs !== 'same-origin' && sfs !== 'none') { res.statusCode = 403; return res.end('Forbidden'); }
    const origin = req.headers['origin'];
    if (origin) { try { if (new URL(origin).host !== req.headers.host) { res.statusCode = 403; return res.end('Forbidden'); } } catch (e) { res.statusCode = 403; return res.end('Forbidden'); } }
    if (!rateLimit(req, res, { max: 10 })) return;
    try {
      const params = new URLSearchParams(await readBody(req));
      const provider = (params.get('provider') || '').toLowerCase();
      const email = (params.get('email') || '').trim();
      const pass = params.get('password') || '';
      let host = (params.get('host') || '').trim();
      let port = parseInt(params.get('port') || '', 10);
      if (PRESETS[provider]) { host = PRESETS[provider].host; port = PRESETS[provider].port; }
      if (!email || !pass || !host) return back(res, 'missing');
      if (!port || isNaN(port)) port = 993;
      const { ImapFlow } = require('imapflow');
      const client = new ImapFlow({ host, port, secure: true, auth: { user: email, pass }, logger: false, emitLogs: false });
      try { await client.connect(); await client.logout(); }
      catch (e) { try { client.close(); } catch (_) {} return back(res, 'auth'); }
      const creds = encrypt(JSON.stringify({ host, port, secure: true, user: email, pass }));
      const prof = Buffer.from(JSON.stringify({ email, name: (email.split('@')[0] || 'You'), picture: '' })).toString('base64url');
      res.setHeader('Set-Cookie', [
        cookie('tally_session', sign(email), 30 * 86400),
        cookie('tally_prof', prof, 30 * 86400),
        cookie('cr_imap', creds, 30 * 86400)
      ]);
      res.writeHead(302, { Location: '/' }); res.end();
    } catch (e) { console.error('imap connect', e && e.message); return back(res, 'error'); }
    return;
  }

  // --- default: local/guest session ---
  if (!rateLimit(req, res, { max: 20 })) return;
  const prof = Buffer.from(JSON.stringify({ email: 'guest', name: 'Local', picture: '' })).toString('base64url');
  res.setHeader('Set-Cookie', [ cookie('tally_session', sign('guest'), 30 * 86400), cookie('tally_prof', prof, 30 * 86400) ]);
  res.writeHead(302, { Location: '/' }); res.end();
};
