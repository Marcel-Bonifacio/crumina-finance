// IMAP providers Crumina can sign in to with an app password, and how an email address maps to one.
// Outlook.com / Hotmail is not listed: Microsoft no longer accepts app passwords for IMAP there (OAuth only).
// The hosted service only connects to these; IMAP_ALLOW_CUSTOM=1 (self-host) also allows any public
// IMAP host on port 993. Keeping the hosted list closed stops the pre-auth sign-in endpoint from being
// used to probe or brute-force arbitrary mail servers through our IPs.
const PROVIDERS = {
  gmail:    { name: 'Gmail',              host: 'imap.gmail.com',          domains: ['gmail.com', 'googlemail.com'], help: 'https://myaccount.google.com/apppasswords' },
  icloud:   { name: 'iCloud Mail',        host: 'imap.mail.me.com',        domains: ['icloud.com', 'me.com', 'mac.com'], help: 'https://support.apple.com/102654' },
  yahoo:    { name: 'Yahoo Mail',         host: 'imap.mail.yahoo.com',     domains: ['yahoo.com', 'yahoo.co.id', 'ymail.com', 'rocketmail.com'], help: 'https://help.yahoo.com/kb/SLN15241.html' },
  aol:      { name: 'AOL Mail',           host: 'imap.aol.com',            domains: ['aol.com'], help: 'https://help.aol.com/articles/Create-and-manage-app-password' },
  zoho:     { name: 'Zoho Mail',          host: 'imap.zoho.com',           domains: ['zoho.com', 'zohomail.com'], help: 'https://www.zoho.com/mail/help/adminconsole/two-factor-authentication.html' },
  fastmail: { name: 'Fastmail',           host: 'imap.fastmail.com',       domains: ['fastmail.com', 'fastmail.fm'], help: 'https://www.fastmail.help/hc/en-us/articles/360058752854' },
  gmx:      { name: 'GMX',                host: 'imap.gmx.com',            domains: ['gmx.com', 'gmx.net', 'gmx.de'], help: 'https://support.gmx.com/pop-imap/toggle.html' },
  yandex:   { name: 'Yandex Mail',        host: 'imap.yandex.com',         domains: ['yandex.com', 'yandex.ru'], help: 'https://yandex.com/support/id/authorization/app-passwords.html' }
};
function customAllowed() { return process.env.IMAP_ALLOW_CUSTOM === '1'; }
function providerForEmail(email) {
  const d = String(email || '').split('@').pop().toLowerCase();
  return Object.keys(PROVIDERS).find(k => PROVIDERS[k].domains.includes(d)) || null;
}
/** Public list for the sign-in screens. */
function publicList() { return Object.keys(PROVIDERS).map(k => ({ id: k, name: PROVIDERS[k].name, domains: PROVIDERS[k].domains, help: PROVIDERS[k].help })); }
module.exports = { PROVIDERS, customAllowed, providerForEmail, publicList };
