const { fromReq } = require('../lib/session');
const { getMailbox } = require('../lib/mailbox');
const { strip, parseAlert, classifyPair } = require('../lib/parse');
const { rateLimit } = require('../lib/ratelimit');
// Bank/card transaction alerts across many issuers (precise parsers for known senders, heuristic for the rest).
const BANK_SPEC = { fromDomains: ['cimbniaga.co.id','permatabank.co.id','klikbca.com','bca.co.id','blubybcadigital.id','bankmandiri.co.id','livin.co.id','bni.co.id','bri.co.id','jago.com','bankjago.com','seabank.co.id','allobank.com','ocbc.id','ocbcnisp.com','danamon.co.id','uob.co.id','maybank.co.id','hsbc.co.id','btpn.com','jenius.com'], sinceDays: 120, notSubject: ['statement','e-statement','billing','tagihan'], max: 45 };
module.exports = async (req, res) => {
  const email = fromReq(req);
  if (!email) { res.statusCode = 401; return res.end('unauthorized'); }
  if (!rateLimit(req, res, { max: 15 })) return;
  let mbox = null;
  try {
    mbox = await getMailbox(req);
    if (!mbox) { res.statusCode = 400; return res.end('Please sign in again.'); }
    const refs = await mbox.search(BANK_SPEC);
    const raw = [];
    for (const r of refs) {
      const msg = await mbox.fetch(r.uid);
      const body = msg.html || msg.text || ''; if (!body) continue;
      const tx = parseAlert(msg.from, msg.subject, strip(body));
      if (tx) raw.push(tx);
    }
    const seen = new Set(); const txns = [];
    for (const tx of raw) { const k = tx.date + '|' + Math.round(Math.abs(tx.amount)) + '|' + tx.account; if (seen.has(k)) continue; seen.add(k); txns.push(tx); }
    classifyPair(txns);
    txns.sort((a, b) => b.date.localeCompare(a.date));
    const cutoff = Date.now() - 30 * 864e5;
    const spent30 = txns.filter(t => (t.class === 'spend' || t.class === 'fee') && new Date(t.date).getTime() >= cutoff).reduce((s, t) => s - t.amount, 0);
    const income30 = txns.filter(t => (t.class === 'income' || t.class === 'transfer_in') && new Date(t.date).getTime() >= cutoff).reduce((s, t) => s + t.amount, 0);
    const data = { generatedAt: new Date().toISOString(), source: 'Bank & card alerts', count: txns.length, spent30, income30, transactions: txns };
    res.setHeader('Content-Type', 'application/json'); res.end(JSON.stringify({ ok: true, data }));
  } catch (e) { console.error('sync', e); res.statusCode = 500; res.end('Sync failed, please try again.'); }
  finally { if (mbox && mbox.close) { try { await mbox.close(); } catch (e) {} } }
};
