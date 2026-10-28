const { rateLimit } = require('../lib/ratelimit');
// One upstream call per process per hour (at most one retry a minute on failure): every base is a
// cross rate of the cached USD table, so no request can bypass the cache.
const TTL = 3600e3; let cache = null, tried = 0, inflight = null;
function usdRates(){
  const now = Date.now();
  if (inflight) return inflight;
  if ((cache && now - cache.t < TTL) || now - tried < 60e3) return Promise.resolve(cache && cache.usd);
  tried = now;
  return inflight = fetch('https://open.er-api.com/v6/latest/USD', { signal: AbortSignal.timeout(8000) })
    .then(r => r.ok ? r.json() : null)
    .then(j => { if (j && j.rates && j.rates.USD) cache = { t: Date.now(), usd: j.rates }; })
    .catch(e => console.error('fx', e && e.message))
    .then(() => { inflight = null; return cache && cache.usd; });   // stale rates beat none
}
module.exports = async (req,res) => {
  res.setHeader('Content-Type','application/json');
  if (!rateLimit(req, res, { name: 'fx', max: 60 })) return;
  let base = 'IDR';
  try{
    base=(new URL(req.url,'http://localhost').searchParams.get('base')||'IDR').toUpperCase().replace(/[^A-Z]/g,'').slice(0,3)||'IDR';
    const usd=await usdRates(), k=usd && usd[base];
    if(!k) throw new Error('fx '+base);
    const rates={}; for(const c in usd) rates[c]=usd[c]/k;   // units of c per 1 base
    res.setHeader('Cache-Control','private, max-age=3600');
    res.end(JSON.stringify({base, rates}));
  }catch(e){ res.setHeader('Cache-Control','no-store'); res.statusCode=200; res.end(JSON.stringify({base,rates:{}})); }
};
