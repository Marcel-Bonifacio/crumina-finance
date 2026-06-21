const { fromReq, getCookie } = require('../lib/session');
const { decrypt } = require('../lib/crypto');
const { getMailbox } = require('../lib/mailbox');
const { readStatements, discoverBanks } = require('../lib/statements');
const { rateLimit } = require('../lib/ratelimit');
module.exports = async (req, res) => {
  const email = fromReq(req); res.setHeader('Content-Type', 'application/json');
  if (!email) { res.statusCode = 401; return res.end('{"error":"unauthorized"}'); }
  if (!rateLimit(req, res, { max: 10 })) return;
  let mbox = null;
  try {
    const isDiscover = /[?&]discover=1(?:&|$)/.test(req.url || '');
    mbox = await getMailbox(req);
    if (!mbox) { if (isDiscover) return res.end(JSON.stringify({ ok: true, banks: [] })); res.statusCode = 400; return res.end('{"error":"sign in again"}'); }
    if (isDiscover) { const banks = await discoverBanks(mbox); return res.end(JSON.stringify({ ok: true, banks })); }
    let pwMap = {};
    try { const c = getCookie(req, 'tally_pw'); if (c) pwMap = JSON.parse(decrypt(c)); } catch (e) {}
    // Native clients have no cookie: they send the password map as base64 JSON in X-Cr-Pw (over TLS).
    if (!Object.keys(pwMap).length) { try { const h = req.headers['x-cr-pw']; if (h) pwMap = JSON.parse(Buffer.from(String(h), 'base64').toString('utf8')); } catch (e) {} }
    const accounts = await readStatements(mbox, pwMap);
    res.end(JSON.stringify({ ok: true, accounts }));
  } catch (e) { console.error('statements', e); res.statusCode = 500; res.end('{"error":"failed"}'); }
  finally { if (mbox && mbox.close) { try { await mbox.close(); } catch (e) {} } }
};
