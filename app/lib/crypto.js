const crypto = require('crypto');
function key(){ const b = Buffer.from(process.env.TOKEN_ENC_KEY || '', 'base64'); if (b.length !== 32) throw new Error('TOKEN_ENC_KEY must be 32 bytes (base64)'); return b; }
function encrypt(plain){ const iv = crypto.randomBytes(12); const c = crypto.createCipheriv('aes-256-gcm', key(), iv); const ct = Buffer.concat([c.update(String(plain),'utf8'), c.final()]); return Buffer.concat([iv, c.getAuthTag(), ct]).toString('base64'); }
function decrypt(b64){ const r = Buffer.from(b64,'base64'); const iv=r.subarray(0,12), tag=r.subarray(12,28), ct=r.subarray(28); const d=crypto.createDecipheriv('aes-256-gcm', key(), iv); d.setAuthTag(tag); return Buffer.concat([d.update(ct), d.final()]).toString('utf8'); }
module.exports = { encrypt, decrypt };
