// Provider-agnostic mailbox: one {search, fetch, close} interface over Gmail (OAuth) or IMAP (self-host).
// Normalized message: { from, subject, dateMs, html, text, attachments:[{filename,mimeType,data:Buffer}] }
const { getCookie } = require('./session');
const { decrypt } = require('./crypto');

// ---- search spec -> Gmail query string (Gmail search operators) ----
function gmailQuery(spec){
  var q=[];
  if(spec.sinceDays) q.push('newer_than:'+spec.sinceDays+'d');
  if(spec.from) q.push('from:'+spec.from);
  if(spec.fromDomains&&spec.fromDomains.length) q.push('from:('+spec.fromDomains.join(' OR ')+')');
  if(spec.subject) q.push('subject:'+spec.subject);
  if(spec.hasAttachment) q.push('has:attachment');
  if(spec.ext) q.push('filename:'+spec.ext);
  if(spec.notSubject&&spec.notSubject.length) q.push('-subject:('+spec.notSubject.join(' OR ')+')');
  return q.join(' ');
}
function gHeader(payload,name){ var h=((payload&&payload.headers)||[]).find(function(x){return (x.name||'').toLowerCase()===name;}); return h?h.value:''; }
function gDecode(d){ return Buffer.from(d||'','base64url').toString('utf8'); }
function gBody(payload){
  var html='',text='';
  (function walk(p){ if(!p) return;
    if(p.mimeType==='text/html'&&p.body&&p.body.data){ if(!html) html=gDecode(p.body.data); }
    else if(p.mimeType==='text/plain'&&p.body&&p.body.data){ if(!text) text=gDecode(p.body.data); }
    (p.parts||[]).forEach(walk);
  })(payload);
  return {html:html,text:text};
}
// Only PDFs are ever parsed, so only PDFs are downloaded, and never one over MAX_ATT bytes:
// anyone can email the user a huge "statement" from a spoofed bank address.
var MAX_ATT=8*1024*1024;
function isPdf(a){ return /\.pdf$/i.test(a.filename||'') || a.mimeType==='application/pdf'; }
function gAttList(payload){
  var out=[];
  (function walk(p){ if(!p) return; if(p.body&&p.body.attachmentId&&isPdf({filename:p.filename,mimeType:p.mimeType})&&(p.body.size||0)<=MAX_ATT) out.push({filename:p.filename||'',mimeType:p.mimeType||'',attId:p.body.attachmentId}); (p.parts||[]).forEach(walk); })(payload);
  return out;
}
// The receiving server's verdict on the sender (Authentication-Results, RFC 8601). Only the top
// header counts: it is the one our own mail server added; lower ones can come from the sender.
// A bank message is trusted when DMARC passes, or when SPF or DKIM passes for the From domain
// itself (aligned). Anything else claiming a bank's From address is a spoof: drop it, so a forged
// "bank alert" cannot add transactions or balances. No header at all (some self-hosted IMAP
// servers) means we cannot judge, so the message is kept.
function domainOf(from){ var m=String(from||'').match(/@([A-Za-z0-9.-]+)/); return m?m[1].toLowerCase().replace(/\.$/,''):''; }
function aligned(d,fromDom){ d=String(d||'').toLowerCase().replace(/^@/,''); return !!d&&!!fromDom&&(d===fromDom||fromDom.endsWith('.'+d)||d.endsWith('.'+fromDom)); }
function spoofed(authResults, from){
  var a=String(authResults||'').toLowerCase(); if(!a) return false;
  if(/\bdmarc=pass\b/.test(a)) return false;
  var fd=domainOf(from), m, re=/\bdkim=pass\b[^;]*?header\.(?:d|i)=([^\s;]+)/g;
  while((m=re.exec(a))) if(aligned(m[1].split('@').pop(),fd)) return false;
  var sp=a.match(/\bspf=pass\b[^;]*?smtp\.mailfrom=([^\s;]+)/);
  if(sp&&aligned(sp[1].split('@').pop(),fd)) return false;
  return true;
}
function gmailMailbox(at){
  var g=require('./google');
  return {
    kind:'gmail',
    search:async function(spec){ var r=await g.gmailList(at, gmailQuery(spec), spec.max||20); return ((r&&r.messages)||[]).map(function(m){return {uid:m.id};}); },
    fetch:async function(uid,opts){ opts=opts||{}; var msg=await g.gmailGet(at,uid); var p=msg.payload||{}; var b=gBody(p);
      var atts=[];
      if(opts.attachments){ var list=gAttList(p); for(var i=0;i<list.length;i++){ var d=await g.gmailAttachment(at,uid,list[i].attId); atts.push({filename:list[i].filename,mimeType:list[i].mimeType,data:Buffer.from(d||'','base64url')}); } }
      return { from:gHeader(p,'from'), subject:gHeader(p,'subject'), dateMs:Number(msg.internalDate)||Date.parse(gHeader(p,'date'))||Date.now(), html:b.html, text:b.text, attachments:atts, spoofed:spoofed(gHeader(p,'authentication-results'), gHeader(p,'from')) };
    },
    close:async function(){}
  };
}

// ---- search spec -> IMAP SEARCH criteria (imapflow) ----
function imapSearchQuery(spec){
  var q={};
  if(spec.sinceDays) q.since=new Date(Date.now()-spec.sinceDays*864e5);
  if(spec.subject) q.subject=spec.subject;
  var doms=spec.fromDomains||[];
  if(spec.from) q.from=spec.from;
  else if(doms.length===1) q.from=doms[0];
  else if(doms.length>1) q.or=doms.map(function(d){return {from:d};});
  return q;
}
function imapMailbox(creds){
  var client=null;
  async function conn(){
    if(client) return client;
    var ImapFlow=require('imapflow').ImapFlow;
    // Re-check the host on every connection: DNS can change after sign-in (rebinding to an internal IP).
    var addr=await require('./netguard').resolvePublic(creds.host);
    if(!addr) throw new Error('imap host not allowed');
    client=new ImapFlow({ host:addr, servername:creds.host, port:993, secure:true, auth:{user:creds.user,pass:creds.pass}, logger:false, emitLogs:false, connectionTimeout:10000, greetingTimeout:10000 });
    await client.connect();
    await client.mailboxOpen('INBOX');
    return client;
  }
  return {
    kind:'imap',
    search:async function(spec){ var cl=await conn(); var uids;
      try{ uids=await cl.search(imapSearchQuery(spec),{uid:true}); }
      catch(e){ uids=await cl.search(spec.sinceDays?{since:new Date(Date.now()-spec.sinceDays*864e5)}:{all:true},{uid:true}); }
      uids=(uids||[]).slice(); uids.sort(function(a,b){return b-a;});
      return uids.slice(0, spec.max||20).map(function(u){return {uid:u};}); },
    fetch:async function(uid){ var cl=await conn(); var simpleParser=require('mailparser').simpleParser;
      // Check the size first: a huge message would be downloaded and parsed in memory before the cap applied.
      var info=await cl.fetchOne(String(uid),{size:true},{uid:true});
      if(info&&info.size>3*MAX_ATT) return {from:'',subject:'',dateMs:Date.now(),html:'',text:'',attachments:[]};
      var m=await cl.fetchOne(String(uid),{source:true},{uid:true});
      if(!m||!m.source) return {from:'',subject:'',dateMs:Date.now(),html:'',text:'',attachments:[]};
      var p=await simpleParser(m.source);
      var atts=(p.attachments||[]).map(function(a){return {filename:a.filename||'',mimeType:a.contentType||'',data:a.content};}).filter(function(a){return isPdf(a)&&a.data&&a.data.length<=MAX_ATT;});
      var ar=p.headers&&p.headers.get('authentication-results'); ar=Array.isArray(ar)?ar[0]:ar; ar=(ar&&ar.value)||ar||'';
      var fromText=(p.from&&p.from.text)||'';
      return { from:fromText, subject:p.subject||'', dateMs:(p.date&&p.date.getTime())||Date.now(), html:p.html||'', text:p.text||'', attachments:atts, spoofed:spoofed(String(ar), fromText) };
    },
    close:async function(){ if(client){ try{ await client.logout(); }catch(e){ try{ client.close(); }catch(_){} } client=null; } }
  };
}

function imapEnabled(){ return !!process.env.IMAP_ENABLED; }
// email: the signed-in session's address. Stored IMAP credentials (cookie on the web, X-Cr-Imap header
// from the app) are only used for that same address.
async function getMailbox(req, email){
  if(imapEnabled()){
    var enc=getCookie(req,'cr_imap') || (req.headers && req.headers['x-cr-imap']);
    if(enc){ try{ var c=JSON.parse(decrypt(String(enc))); if(c&&c.host&&c.user&&c.pass&&(!email||String(c.user).toLowerCase()===String(email).toLowerCase())) return imapMailbox(c); }catch(e){} }
  }
  var rt=getCookie(req,'tally_rt') || (req.headers && req.headers['x-cr-rt']);
  if(rt){ try{ var at=await require('./google').refresh(decrypt(rt)); return gmailMailbox(at); }catch(e){ return null; } }
  return null;
}
module.exports={ getMailbox, gmailMailbox, imapMailbox, gmailQuery, imapSearchQuery, imapEnabled, spoofed, MAX_ATT };
