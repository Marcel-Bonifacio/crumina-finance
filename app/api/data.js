const { fromReq, getCookie } = require('../lib/session');
module.exports = (req, res) => {
  res.setHeader('Content-Type', 'application/json');
  const email = fromReq(req);
  if (!email) { res.statusCode = 401; return res.end('{"error":"unauthorized"}'); }
  let prof = { email };
  try { const p = getCookie(req, 'tally_prof'); if (p) prof = JSON.parse(Buffer.from(p, 'base64url').toString()); } catch (e) {}
  res.end(JSON.stringify({ profile: prof, data: null }));
};
