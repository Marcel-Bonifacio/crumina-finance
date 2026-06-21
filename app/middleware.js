export const config = { matcher: ['/((?!api/|login.html|login|brand/|favicon.ico|manifest.webmanifest|sw.js|app.js|vendor/|privacy.html|privacy|terms.html|terms|home.html|home|icon-).*)'] };
function b64urlToBytes(s){ s=s.replace(/-/g,'+').replace(/_/g,'/'); while(s.length%4) s+='='; const bin=atob(s); const a=new Uint8Array(bin.length); for(let i=0;i<bin.length;i++) a[i]=bin.charCodeAt(i); return a; }
function bytesToB64url(buf){ let bin=''; const a=new Uint8Array(buf); for(let i=0;i<a.length;i++) bin+=String.fromCharCode(a[i]); return btoa(bin).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,''); }
async function valid(token){
  try{
    const secret=process.env.SESSION_SECRET||''; if(!secret) return false;
    const [body,sig]=String(token).split('.'); if(!body||!sig) return false;
    const k=await crypto.subtle.importKey('raw', new TextEncoder().encode(secret), {name:'HMAC',hash:'SHA-256'}, false, ['sign']);
    const mac=await crypto.subtle.sign('HMAC', k, new TextEncoder().encode(body));
    const got=bytesToB64url(mac); if(got.length!==sig.length) return false;
    let diff=0; for(let i=0;i<got.length;i++) diff|=got.charCodeAt(i)^sig.charCodeAt(i); if(diff!==0) return false;
    const o=JSON.parse(new TextDecoder().decode(b64urlToBytes(body)));
    return (Date.now()-o.t) <= 30*864e5;
  }catch(e){ return false; }
}
export default async function middleware(req){
  const c=req.headers.get('cookie')||'';
  const m=c.split(';').map(s=>s.trim()).find(s=>s.startsWith('tally_session='));
  if(m && await valid(m.slice(14))) return;
  const u=new URL(req.url); u.pathname='/login'; u.search=''; return Response.redirect(u,302);
}
