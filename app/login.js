(function(){
  function $(id){ return document.getElementById(id); }
  var HEAD={outlook:'Outlook.com (IMAP) · use an app password',icloud:'iCloud Mail (IMAP) · use an app-specific password',custom:'Any IMAP server · enter host, port + app password',gmail:'Gmail · use an app password'};
  fetch('/api/guest?config=1',{credentials:'same-origin'}).then(function(r){return r.json();}).then(function(c){
    if(c&&c.imap){ var w=$('imapWrap'); if(w) w.style.display='block'; }
    if(c&&c.google===false){ var g=$('gBtn'); if(g) g.style.display='none'; var od=$('orDiv'); if(od) od.style.display='none'; }
    if(c&&c.google&&c.clientId) loadGis(c.clientId);
  }).catch(function(){});

  // Google Identity Services: the Google button opens Google's sign-in popup (authorization code model)
  // and the one-time code is exchanged on our server. If the script, the popup or the exchange fails,
  // the button falls back to its href, the server-side redirect flow (/api/auth/start).
  var codeClient=null;
  var SCOPE='openid email profile https://www.googleapis.com/auth/gmail.readonly';
  function loadGis(clientId){
    var s=document.createElement('script'); s.src='https://accounts.google.com/gsi/client'; s.async=true;
    s.onload=function(){
      try{
        codeClient=google.accounts.oauth2.initCodeClient({
          client_id:clientId, scope:SCOPE, ux_mode:'popup', select_account:true,
          callback:onCode,
          error_callback:function(err){ if(err&&err.type==='popup_failed_to_open') fallback(); }
        });
      }catch(e){ codeClient=null; }
    };
    document.head.appendChild(s);
  }
  function fallback(){ location.href='/api/auth/start'; }
  function onCode(resp){
    if(!resp||!resp.code){ return; } // closed or denied: stay on the page
    if(!/gmail\.readonly/.test(resp.scope||'')){ showErr("Crumina needs read access to Gmail to find your statements. Please allow it and try again."); return; }
    fetch('/api/auth/callback',{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/json','X-Requested-With':'XMLHttpRequest'},body:JSON.stringify({client:'web',code:resp.code})})
      .then(function(r){return r.json();})
      .then(function(j){ if(j&&j.ok){ location.href='/'; } else if(j&&(j.consent||j.error==='gmail_scope_missing')){ fallback(); } else { showErr('Sign-in failed. Please try again.'); } })
      .catch(function(){ fallback(); });
  }
  function showErr(m){ var e=$('gErr'); if(!e){ e=document.createElement('p'); e.id='gErr'; e.style.cssText='color:#FFC9D4;margin:12px 0 0;opacity:1'; var g=$('gBtn'); if(g&&g.parentNode) g.parentNode.insertBefore(e,g.nextSibling); } e.textContent=m; }
  function openIm(prov){
    var p=$('imProv'); if(p) p.value=prov;
    var cu=$('imCustom'); if(cu) cu.style.display=(prov==='custom')?'block':'none';
    var h=$('imHead'); if(h) h.textContent=HEAD[prov]||'';
    var f=$('imapForm'); if(f) f.style.display='block';
    var em=document.querySelector('#imapForm input[name=email]'); if(em){ try{ em.focus(); }catch(_){} }
  }
  document.addEventListener('click',function(e){
    var gb=e.target.closest&&e.target.closest('#gBtn');
    if(gb&&codeClient){ e.preventDefault(); try{ codeClient.requestCode(); }catch(_){ fallback(); } return; }
    var t=e.target.closest&&e.target.closest('[data-imp]');
    if(t){ e.preventDefault(); openIm(t.getAttribute('data-imp')); }
  });
  var ec=new URLSearchParams(location.search).get('imap');
  if(ec){ var w=$('imapWrap'); if(w) w.style.display='block'; var f=$('imapForm'); if(f) f.style.display='block';
    var er=$('imapErr'); if(er){ er.textContent = ec==='missing'?'Please fill in all fields.' : ec==='auth'?"Couldn't sign in. Check the email, app password, and server.":ec==='host'?"This mail server isn't allowed here. Use the provider's IMAP host on port 993.":'Connection error. Please try again.'; er.style.display='block'; } }
})();
