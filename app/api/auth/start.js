const { authUrl } = require('../../lib/google');
const { cookie } = require('../../lib/session');
const crypto = require('crypto');
const { rateLimit } = require('../../lib/ratelimit');
module.exports = (req, res) => {
  if (!rateLimit(req, res, { max: 12 })) return;
  const nonce = crypto.randomBytes(16).toString('hex');
  // Native app starts the flow with ?client=android; carry that in the OAuth state
  // (the nonce still does CSRF) so the callback knows to hand back tokens via App Link.
  const isAndroid = /[?&]client=android(?:&|$)/.test(req.url || '');
  const state = isAndroid ? nonce + '.android' : nonce;
  res.setHeader('Set-Cookie', cookie('oauth_state', nonce, 600));
  res.writeHead(302, { Location: authUrl(state) }); res.end();
};
