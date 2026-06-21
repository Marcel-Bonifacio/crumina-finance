const C='crumina-v7';
self.addEventListener('install',function(e){self.skipWaiting();e.waitUntil(caches.open(C).then(function(c){return c.addAll(['/','/app.js','/login','/manifest.webmanifest','/icon-192.png','/icon-32.png','/brand/crumina-icon.svg']).catch(function(){});}));});
self.addEventListener('activate',function(e){e.waitUntil(caches.keys().then(function(ks){return Promise.all(ks.filter(function(k){return k!==C;}).map(function(k){return caches.delete(k);}));}).then(function(){return self.clients.claim();}));});
self.addEventListener('fetch',function(e){
  if(e.request.method!=='GET')return;
  var u;try{u=new URL(e.request.url);}catch(_){return;}
  if(u.origin!==location.origin)return;
  if(u.pathname.indexOf('/api/')===0)return;
  if(u.pathname.indexOf('/vendor/')===0){e.respondWith(caches.match(e.request).then(function(r){return r||fetch(e.request).then(function(rr){if(rr&&rr.ok){var cc=rr.clone();caches.open(C).then(function(ca){ca.put(e.request,cc);});}return rr;});}));return;}
  e.respondWith(fetch(e.request).then(function(r){if(r&&r.status===200&&r.type==='basic'){var cc=r.clone();caches.open(C).then(function(ca){ca.put(e.request,cc);});}return r;}).catch(function(){return caches.match(e.request).then(function(r){return r||caches.match('/');});}));
});
self.addEventListener('notificationclick',function(e){e.notification.close();e.waitUntil(clients.matchAll({type:'window'}).then(function(cs){for(var i=0;i<cs.length;i++){if('focus'in cs[i])return cs[i].focus();}if(clients.openWindow)return clients.openWindow('/');}));});
