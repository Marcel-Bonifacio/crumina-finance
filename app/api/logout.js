const { cookie } = require('../lib/session');
module.exports = (req, res) => {
  // Clear every auth/secret cookie (stateless backend can't revoke, so expire them all).
  const names = ['tally_session', 'tally_prof', 'tally_rt', 'tally_pw', 'cr_imap'];
  res.setHeader('Set-Cookie', names.map(n => cookie(n, '', 0)));
  res.writeHead(302, { Location: '/login' }); res.end();
};
