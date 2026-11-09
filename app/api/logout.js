const { cookie } = require('../lib/session');
module.exports = (req, res) => {
  // Clear every auth/secret cookie (stateless backend can't revoke, so expire them all).
  const names = ['tally_session', 'tally_prof', 'tally_rt', 'tally_pw', 'cr_imap'];
  res.setHeader('Set-Cookie', names.map(n => cookie(n, '', 0)));
  // Also drop the offline app-shell cache (it can hold a rendered copy of the signed-in pages).
  res.setHeader('Clear-Site-Data', '"cache"');
  res.writeHead(302, { Location: '/login' }); res.end();
};
