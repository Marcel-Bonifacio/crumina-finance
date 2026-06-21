const { cookie } = require('../lib/session');
module.exports = (req, res) => { res.setHeader('Set-Cookie', cookie('tally_session', '', 0)); res.writeHead(302, { Location: '/login' }); res.end(); };
