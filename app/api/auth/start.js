const { authUrl } = require('../../lib/google');
const { cookie } = require('../../lib/session');
const crypto = require('crypto');
const { rateLimit } = require('../../lib/ratelimit');
module.exports = (req, res) => {
  if (!rateLimit(req, res, { max: 12 })) return;
  const nonce = crypto.randomBytes(16).toString('hex');
  res.setHeader('Set-Cookie', cookie('oauth_state', nonce, 600));
  res.writeHead(302, { Location: authUrl(nonce) }); res.end();
};
