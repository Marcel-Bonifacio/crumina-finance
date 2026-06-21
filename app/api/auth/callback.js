const { exchangeCode, userInfo } = require('../../lib/google');
const { sign, cookie, getCookie } = require('../../lib/session');
const { encrypt } = require('../../lib/crypto');
const APP_LINK = 'https://crumina.tirtawijata.com/auth/android';
module.exports = async (req, res) => {
  try {
    const url = new URL(req.url, 'https://' + req.headers.host);
    const state = url.searchParams.get('state') || '', nonce = getCookie(req, 'oauth_state');
    const [stateNonce, client] = state.split('.');
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
        + (tok.refresh_token ? '&rt=' + encodeURIComponent(encrypt(tok.refresh_token)) : '');
      res.writeHead(302, { Location: APP_LINK + '#' + frag }); return res.end();
    }
    const cookies = [ cookie('tally_session', sign(info.email), 30*86400), cookie('tally_prof', prof, 30*86400), cookie('oauth_state', '', 0) ];
    if (tok.refresh_token) cookies.push(cookie('tally_rt', encrypt(tok.refresh_token), 30*86400));
    res.setHeader('Set-Cookie', cookies);
    res.writeHead(302, { Location: '/' }); res.end();
  } catch (e) { console.error('callback', e); res.statusCode = 500; res.end('Sign-in error, please try again.'); }
};
