// Security guard tests. Run with `npm test` from app/ (no network: only IP literals are resolved).
const assert = require('assert');
const { resolvePublic, blocked } = require('../lib/netguard');
const { spoofed } = require('../lib/mailbox');

let pass = 0, fail = 0;
async function eq(label, got, want) {
  got = await got;
  try { assert.deepStrictEqual(got, want); pass++; console.log('ok   ' + label); }
  catch (e) { fail++; console.log('FAIL ' + label + '\n     got  ' + JSON.stringify(got) + '\n     want ' + JSON.stringify(want)); }
}

(async () => {
  delete process.env.IMAP_ALLOW_PRIVATE;
  // --- netguard: internal addresses are refused, public ones are returned as-is
  for (const ip of ['127.0.0.1', '10.1.2.3', '169.254.169.254', '192.168.1.1', '172.20.0.1', '100.64.0.1', '0.0.0.0', '::1', 'fe80::1', 'fd00::1', '::ffff:127.0.0.1', '64:ff9b::7f00:1'])
    await eq('netguard refuses ' + ip, resolvePublic(ip), null);
  await eq('netguard allows a public IPv4', resolvePublic('8.8.8.8'), '8.8.8.8');
  await eq('netguard allows a public IPv6', resolvePublic('2001:4860:4860::8888'), '2001:4860:4860::8888');
  await eq('netguard refuses empty host', resolvePublic(''), null);
  await eq('blocked() maps IPv4-in-IPv6', blocked('::ffff:10.0.0.1'), true);
  process.env.IMAP_ALLOW_PRIVATE = '1';
  await eq('IMAP_ALLOW_PRIVATE=1 lets a LAN host through', resolvePublic('192.168.1.10'), '192.168.1.10');
  delete process.env.IMAP_ALLOW_PRIVATE;

  // --- spoofed(): forged bank mail is dropped, real mail is kept
  await eq('no Authentication-Results: kept', spoofed(''), false);
  await eq('dmarc=pass: kept', spoofed('mx.google.com; dkim=pass header.i=@bca.co.id; spf=pass; dmarc=pass (p=REJECT) header.from=bca.co.id'), false);
  await eq('dmarc=fail: dropped', spoofed('mx.google.com; spf=fail smtp.mailfrom=evil.example; dmarc=fail (p=NONE) header.from=bca.co.id'), true);
  await eq('spf=softfail, no dkim: dropped', spoofed('mx.google.com; spf=softfail smtp.mailfrom=bca.co.id'), true);
  await eq('spf=fail but dkim=pass (forwarded): kept', spoofed('mx.google.com; dkim=pass header.i=@bca.co.id; spf=fail'), false);

  console.log('\n' + pass + ' passed, ' + fail + ' failed');
  process.exit(fail ? 1 : 0);
})();
