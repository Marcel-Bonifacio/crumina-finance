const { fromReq, getCookie, cookie } = require('../lib/session');
const { encrypt, decrypt } = require('../lib/crypto');
const { rateLimit } = require('../lib/ratelimit');
module.exports = async (req,res) => {
  const email=fromReq(req); res.setHeader('Content-Type','application/json');
  if(!email){res.statusCode=401;return res.end('{"error":"unauthorized"}');}
  if(req.method!=='POST'){res.statusCode=405;return res.end('{"error":"method"}');}
  if(!rateLimit(req,res,{max:20}))return;
  let body=''; for await(const c of req){ body+=c; if(body.length>65536){res.statusCode=413;return res.end('{"error":"too_large"}');} }
  let bank,password; try{const j=JSON.parse(body);bank=j.bank;password=j.password;}catch(e){const p=new URLSearchParams(body);bank=p.get('bank');password=p.get('password');}
  if(!bank||!password){res.statusCode=400;return res.end('{"error":"missing"}');}
  let map={}; try{const cur=getCookie(req,'tally_pw'); if(cur) map=JSON.parse(decrypt(cur));}catch(e){}
  map[bank]=password;
  res.setHeader('Set-Cookie', cookie('tally_pw', encrypt(JSON.stringify(map)), 30*86400));
  res.end(JSON.stringify({ok:true, banks:Object.keys(map)}));
};
