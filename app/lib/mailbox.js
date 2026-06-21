// Provider-agnostic mailbox: one {search, fetch, close} interface over Gmail (OAuth) or IMAP (self-host).
// Normalized message: { from, subject, dateMs, html, text, attachments:[{filename,mimeType,data:Buffer}] }
const { getCookie } = require('./session');
const { decrypt } = require('./crypto');

// ---- search spec -> Gmail query string (kept byte-identical to the historical queries) ----
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
function gAttList(payload){
  var out=[];
  (function walk(p){ if(!p) return; if(p.body&&p.body.attachmentId) out.push({filename:p.filename||'',mimeType:p.mimeType||'',attId:p.body.attachmentId}); (p.parts||[]).forEach(walk); })(payload);
  return out;
}
function gmailMailbox(at){
  var g=require('./google');
  return {
    kind:'gmail',
    search:async function(spec){ var r=await g.gmailList(at, gmailQuery(spec), spec.max||20); return ((r&&r.messages)||[]).map(function(m){return {uid:m.id};}); },
    fetch:async function(uid,opts){ opts=opts||{}; var msg=await g.gmailGet(at,uid); var p=msg.payload||{}; var b=gBody(p);
      var atts=[];
      if(opts.attachments){ var list=gAttList(p); for(var i=0;i<list.length;i++){ var d=await g.gmailAttachment(at,uid,list[i].attId); atts.push({filename:list[i].filename,mimeType:list[i].mimeType,data:Buffer.from(d||'','base64url')}); } }
      return { from:gHeader(p,'from'), subject:gHeader(p,'subject'), dateMs:Number(msg.internalDate)||Date.parse(gHeader(p,'date'))||Date.now(), html:b.html, text:b.text, attachments:atts };
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
    client=new ImapFlow({ host:creds.host, port:creds.port||993, secure:creds.secure!==false, auth:{user:creds.user,pass:creds.pass}, logger:false, emitLogs:false });
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
      var m=await cl.fetchOne(String(uid),{source:true},{uid:true});
      if(!m||!m.source) return {from:'',subject:'',dateMs:Date.now(),html:'',text:'',attachments:[]};
      var p=await simpleParser(m.source);
      var atts=(p.attachments||[]).map(function(a){return {filename:a.filename||'',mimeType:a.contentType||'',data:a.content};});
      return { from:(p.from&&p.from.text)||'', subject:p.subject||'', dateMs:(p.date&&p.date.getTime())||Date.now(), html:p.html||'', text:p.text||'', attachments:atts };
    },
    close:async function(){ if(client){ try{ await client.logout(); }catch(e){ try{ client.close(); }catch(_){} } client=null; } }
  };
}

function imapEnabled(){ return !!process.env.IMAP_ENABLED; }
async function getMailbox(req){
  if(imapEnabled()){
    var enc=getCookie(req,'cr_imap');
    if(enc){ try{ var c=JSON.parse(decrypt(enc)); if(c&&c.host&&c.user&&c.pass) return imapMailbox(c); }catch(e){} }
  }
  var rt=getCookie(req,'tally_rt') || (req.headers && req.headers['x-cr-rt']);
  if(rt){ try{ var at=await require('./google').refresh(decrypt(rt)); return gmailMailbox(at); }catch(e){ return null; } }
  return null;
}
module.exports={ getMailbox, gmailMailbox, imapMailbox, gmailQuery, imapSearchQuery, imapEnabled };
