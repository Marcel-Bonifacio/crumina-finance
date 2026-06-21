(function(){
  function $(id){ return document.getElementById(id); }
  var HEAD={outlook:'Outlook / Office 365 · use an app password',icloud:'Apple iCloud Mail · use an app-specific password',custom:'Any IMAP server · enter host, port + app password',gmail:'Gmail · use an app password'};
  fetch('/api/guest?config=1',{credentials:'same-origin'}).then(function(r){return r.json();}).then(function(c){
    if(c&&c.imap){ var w=$('imapWrap'); if(w) w.style.display='block'; }
    if(c&&c.google===false){ var g=$('gBtn'); if(g) g.style.display='none'; var od=$('orDiv'); if(od) od.style.display='none'; }
  }).catch(function(){});
  function openIm(prov){
    var p=$('imProv'); if(p) p.value=prov;
    var cu=$('imCustom'); if(cu) cu.style.display=(prov==='custom')?'block':'none';
    var h=$('imHead'); if(h) h.textContent=HEAD[prov]||'';
    var f=$('imapForm'); if(f) f.style.display='block';
    var em=document.querySelector('#imapForm input[name=email]'); if(em){ try{ em.focus(); }catch(_){} }
  }
  document.addEventListener('click',function(e){
    var t=e.target.closest&&e.target.closest('[data-imp]');
    if(t){ e.preventDefault(); openIm(t.getAttribute('data-imp')); }
  });
  var ec=new URLSearchParams(location.search).get('imap');
  if(ec){ var w=$('imapWrap'); if(w) w.style.display='block'; var f=$('imapForm'); if(f) f.style.display='block';
    var er=$('imapErr'); if(er){ er.textContent = ec==='missing'?'Please fill in all fields.' : ec==='auth'?"Couldn't sign in. Check the email, app password, and server.":'Connection error. Please try again.'; er.style.display='block'; } }
})();
