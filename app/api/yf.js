const { fromReq } = require('../lib/session');
const { rateLimit } = require('../lib/ratelimit');
const UA = { headers: { 'User-Agent': 'Mozilla/5.0 (compatible; Crumina/1.0)' } };
module.exports = async (req, res) => {
  res.setHeader('Content-Type', 'application/json');
  if (!fromReq(req)) { res.statusCode = 401; return res.end('{"error":"unauthorized"}'); }
  if (!rateLimit(req, res, { max: 40 })) return;
  try {
    const u = new URL(req.url, 'https://' + req.headers.host);
    const q = u.searchParams.get('q');
    const symbol = u.searchParams.get('symbol');
    if (q) {
      const r = await fetch('https://query1.finance.yahoo.com/v1/finance/search?quotesCount=8&newsCount=0&q=' + encodeURIComponent(q), UA);
      const j = await r.json();
      const ok = { EQUITY:1, ETF:1, MUTUALFUND:1, CRYPTOCURRENCY:1, INDEX:1, CURRENCY:1, FUTURE:1 };
      const out = (j.quotes || []).filter(x => x.symbol && ok[x.quoteType]).slice(0, 8)
        .map(x => ({ symbol: x.symbol, name: x.longname || x.shortname || x.symbol, exch: x.exchDisp || x.exchange || '', type: x.quoteType }));
      res.setHeader('Cache-Control', 'public, max-age=120');
      return res.end(JSON.stringify({ quotes: out }));
    }
    if (symbol) {
      const range = (u.searchParams.get('range') || '6mo').replace(/[^a-z0-9]/gi, '') || '6mo';
      const r = await fetch('https://query1.finance.yahoo.com/v8/finance/chart/' + encodeURIComponent(symbol) + '?range=' + range + '&interval=1d', UA);
      const j = await r.json();
      const R = j && j.chart && j.chart.result && j.chart.result[0];
      if (!R || !R.meta) { res.statusCode = 404; return res.end('{"error":"not_found"}'); }
      const m = R.meta, ts = R.timestamp || [];
      const cl = (R.indicators && R.indicators.quote && R.indicators.quote[0] && R.indicators.quote[0].close) || [];
      const history = [];
      for (let i = 0; i < ts.length; i++) if (cl[i] != null) history.push([ts[i] * 1000, +(+cl[i]).toFixed(4)]);
      res.setHeader('Cache-Control', 'public, max-age=300');
      return res.end(JSON.stringify({ symbol: m.symbol, price: m.regularMarketPrice, currency: m.currency || 'USD', name: m.longName || m.shortName || m.symbol, exch: m.fullExchangeName || m.exchangeName || '', history: history }));
    }
    res.statusCode = 400; res.end('{"error":"q_or_symbol_required"}');
  } catch (e) { console.error('yf', e); res.statusCode = 200; res.end('{"quotes":[],"history":[],"error":"yf"}'); }
};
