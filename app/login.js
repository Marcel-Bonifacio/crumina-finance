(function(){
  function $(id){ return document.getElementById(id); }
  var PROVIDERS=[], CUSTOM=false;
  fetch('/api/guest?config=1',{credentials:'same-origin'}).then(function(r){return r.json();}).then(function(c){
    if(c&&c.imap){ var w=$('imapWrap'); if(w) w.style.display='block'; PROVIDERS=c.providers||[]; CUSTOM=!!c.imapCustom; fillProviders(); }
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
      // Anything but success goes through the proven redirect flow, so sign-in never gets stuck here.
      .then(function(j){ if(j&&j.ok){ location.href='/'; } else { fallback(); } })
      .catch(function(){ fallback(); });
  }
  function showErr(m){ var e=$('gErr'); if(!e){ e=document.createElement('p'); e.id='gErr'; e.style.cssText='color:#FFC9D4;margin:12px 0 0;opacity:1'; var g=$('gBtn'); if(g&&g.parentNode) g.parentNode.insertBefore(e,g.nextSibling); } e.textContent=m; }
  // Provider list comes from the server (/api/guest?config=1); the hosted service only allows these.
  function fillProviders(){
    var sel=$('imProv'); if(!sel) return; sel.textContent='';
    // No silent default: an address we don't recognise must pick its provider explicitly.
    var ph=document.createElement('option'); ph.value=''; ph.textContent='Choose your email provider'; ph.disabled=true; ph.selected=true; sel.appendChild(ph); sel.required=true;
    PROVIDERS.forEach(function(p){ var o=document.createElement('option'); o.value=p.id; o.textContent=p.name; sel.appendChild(o); });
    if(CUSTOM){ var o=document.createElement('option'); o.value='custom'; o.textContent='Other IMAP server'; sel.appendChild(o); }
    sel.onchange=syncProv;
    var em=$('imEmail'); if(em) em.oninput=function(){ var d=(em.value.split('@')[1]||'').toLowerCase(); var hit=PROVIDERS.find(function(p){return p.domains.indexOf(d)>=0;}); if(hit){ sel.value=hit.id; } else if(sel.value&&PROVIDERS.some(function(p){return p.id===sel.value&&p.domains.indexOf(d)<0;})&&d.indexOf('.')>0){ sel.value=''; } syncProv(); };
    syncProv();
  }
  function syncProv(){
    var sel=$('imProv'), v=sel?sel.value:'', p=PROVIDERS.find(function(x){return x.id===v;});
    var cu=$('imCustom'); if(cu) cu.style.display=(v==='custom')?'block':'none';
    var h=$('imHelp'); if(!h) return; h.textContent='';
    if(p&&p.help){ var a=document.createElement('a'); a.href=p.help; a.target='_blank'; a.rel='noopener'; a.style.cssText='color:#7fe3cf;text-decoration:underline'; a.textContent='How to create a '+p.name+' app password'; h.appendChild(a); }
  }
  function openIm(){
    var f=$('imapForm'); if(f) f.style.display='block';
    var em=$('imEmail'); if(em){ try{ em.focus(); }catch(_){} }
  }
  document.addEventListener('click',function(e){
    var gb=e.target.closest&&e.target.closest('#gBtn');
    if(gb&&codeClient){ e.preventDefault(); try{ codeClient.requestCode(); }catch(_){ fallback(); } return; }
    var t=e.target.closest&&e.target.closest('[data-imp]');
    if(t){ e.preventDefault(); openIm(t.getAttribute('data-imp')); }
  });
  var ec=new URLSearchParams(location.search).get('imap');
  if(ec){ var w=$('imapWrap'); if(w) w.style.display='block'; var f=$('imapForm'); if(f) f.style.display='block';
    var er=$('imapErr'); if(er){ er.textContent = ec==='missing'?'Please fill in all fields.' : ec==='auth'?"Couldn't sign in. Check the email and app password (your normal password won't work; Gmail needs 2-Step Verification on to create one).":ec==='host'?"This mail server isn't allowed here. Use the provider's IMAP host on port 993.":ec==='provider'?"That email provider isn't supported here yet. Pick one from the list.":'Connection error. Please try again.'; er.style.display='block'; } }
})();
