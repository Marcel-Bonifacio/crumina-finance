const { exchangeCode, userInfo } = require('../../lib/google');
const { sign, cookie, getCookie } = require('../../lib/session');
const { encrypt } = require('../../lib/crypto');
const { rateLimit } = require('../../lib/ratelimit');
const APP_LINK = 'https://crumina.tirtawijata.com/auth/android';
// POST: a one-time authorization code from Google Identity Services instead of the redirect flow.
//   {client:'web', code}     from the GIS popup code client on /login (exchanged with redirect_uri 'postmessage')
//   {client:'android', code} a server auth code from the Android AuthorizationClient (redirect_uri '')
// Google binds each code to our client ID and it can be redeemed once. The web call is also limited to
// same-origin XHR (GIS's documented CSRF check: the X-Requested-With header plus the Origin).
async function readJson(req){ let b=''; for await (const c of req){ b+=c; if(b.length>8192) throw new Error('too large'); } return JSON.parse(b||'{}'); }
function json(res, code, obj){ res.statusCode=code; res.setHeader('Content-Type','application/json'); res.setHeader('Cache-Control','no-store'); res.end(JSON.stringify(obj)); }
async function exchangePost(req, res){
  if (!rateLimit(req, res, { name: 'auth-code', max: 12 })) return;
  let body; try { body = await readJson(req); } catch (e) { return json(res, 400, { ok:false, error:'bad_body' }); }
  const code = typeof body.code === 'string' ? body.code : '', client = body.client;
  if (!code || code.length > 2048 || (client !== 'web' && client !== 'android')) return json(res, 400, { ok:false, error:'bad_request' });
  if (client === 'web') {
    const origin = req.headers.origin, sfs = req.headers['sec-fetch-site'];
    let sameOrigin = false; try { sameOrigin = !!origin && new URL(origin).host === req.headers.host; } catch (e) {}
    if (req.headers['x-requested-with'] !== 'XMLHttpRequest' || !sameOrigin || (sfs && sfs !== 'same-origin')) return json(res, 403, { ok:false, error:'forbidden' });
  }
  let tok, info;
  try { tok = await exchangeCode(code, client === 'web' ? 'postmessage' : ''); info = await userInfo(tok.access_token); }
  catch (e) { console.error('auth code', e && e.message); return json(res, 400, { ok:false, error:'exchange_failed' }); }
  const verified = info.verified_email !== undefined ? info.verified_email : info.email_verified;
  if (!info.email || verified === false) return json(res, 403, { ok:false, error:'unverified' });
  // Gmail access is the point of signing in; without it the app can't read statements.
  const scopes = String(tok.scope || '');
  if (!/gmail\.readonly/.test(scopes)) return json(res, 200, { ok:false, error:'gmail_scope_missing' });
  const prof = Buffer.from(JSON.stringify({ email: info.email, name: info.name || info.given_name || '', picture: info.picture || '' })).toString('base64url');
  if (client === 'android') {
    return json(res, 200, { ok:true, t: sign(info.email), p: prof, rt: tok.refresh_token ? encrypt(tok.refresh_token) : null });
  }
  // A popup sign-in only returns a refresh token on first consent. Without one (a new browser) mail sync
  // would fail, so send the browser through the redirect flow, which asks for consent and returns one.
  if (!tok.refresh_token && !getCookie(req, 'tally_rt')) return json(res, 200, { ok:false, consent:true });
  const cookies = [ cookie('tally_session', sign(info.email), 30*86400), cookie('tally_prof', prof, 30*86400) ];
  if (tok.refresh_token) cookies.push(cookie('tally_rt', encrypt(tok.refresh_token), 30*86400));
  res.setHeader('Set-Cookie', cookies);
  return json(res, 200, { ok:true });
}
module.exports = async (req, res) => {
  if (req.method === 'POST') { try { return await exchangePost(req, res); } catch (e) { console.error('auth code', e); return json(res, 500, { ok:false, error:'server' }); } }
  try {
    const url = new URL(req.url, 'https://' + req.headers.host);
    const state = url.searchParams.get('state') || '', nonce = getCookie(req, 'oauth_state');
    const [stateNonce, client, appState] = state.split('.');
    if (!stateNonce || !nonce || stateNonce !== nonce) { res.statusCode = 400; return res.end('Sign-in failed, please try again.'); }
    const code = url.searchParams.get('code'); if (!code) { res.statusCode = 400; return res.end('Sign-in failed.'); }
    const tok = await exchangeCode(code);
    const info = await userInfo(tok.access_token);
    const verified = info.verified_email !== undefined ? info.verified_email : info.email_verified;
    if (!info.email || verified === false) { res.statusCode = 403; return res.end('A verified Google email is required.'); }
    const prof = Buffer.from(JSON.stringify({ email: info.email, name: info.name || info.given_name || '', picture: info.picture || '' })).toString('base64url');
    if (client === 'android') {
      // Native client: no cookies. Hand the session to the app through the verified App Link
      // fragment (fragments are never sent to a server, never logged).
      res.setHeader('Set-Cookie', cookie('oauth_state', '', 0));
      const frag = 't=' + encodeURIComponent(sign(info.email)) + '&p=' + encodeURIComponent(prof)
        + (tok.refresh_token ? '&rt=' + encodeURIComponent(encrypt(tok.refresh_token)) : '')
        + (appState ? '&s=' + encodeURIComponent(appState) : '');
      res.writeHead(302, { Location: APP_LINK + '#' + frag }); return res.end();
    }
    const cookies = [ cookie('tally_session', sign(info.email), 30*86400), cookie('tally_prof', prof, 30*86400), cookie('oauth_state', '', 0) ];
    if (tok.refresh_token) cookies.push(cookie('tally_rt', encrypt(tok.refresh_token), 30*86400));
    res.setHeader('Set-Cookie', cookies);
    res.writeHead(302, { Location: '/' }); res.end();
  } catch (e) { console.error('callback', e); res.statusCode = 500; res.end('Sign-in error, please try again.'); }
};
