const crypto = require('crypto');
function secret(){ const s=process.env.SESSION_SECRET; if(!s) throw new Error('SESSION_SECRET not set'); return s; }
function sign(email){
  const body = Buffer.from(JSON.stringify({e:email,t:Date.now()})).toString('base64url');
  const sig = crypto.createHmac('sha256',secret()).update(body).digest('base64url');
  return body+'.'+sig;
}
function verify(token){
  try{
    const [body,sig]=String(token).split('.');
    const exp=crypto.createHmac('sha256',secret()).update(body).digest('base64url');
    if(!sig||sig.length!==exp.length||!crypto.timingSafeEqual(Buffer.from(sig),Buffer.from(exp))) return null;
    const o=JSON.parse(Buffer.from(body,'base64url').toString());
    return (Date.now()-o.t>30*864e5)?null:o.e;
  }catch(e){return null;}
}
function cookie(name,val,maxAge){ return `${name}=${val}; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=${maxAge}`; }
function getCookie(req,name){ const c=req.headers.cookie||''; const m=c.split(';').map(s=>s.trim()).find(s=>s.startsWith(name+'=')); return m?m.slice(name.length+1):null; }
function fromReq(req){ const v=getCookie(req,'tally_session'); return v?verify(v):null; }
module.exports={sign,verify,cookie,getCookie,fromReq};
