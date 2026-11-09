// Strip the token-bearing #t=...&rt=... fragment from the address bar and history entry.
// External file because the CSP (script-src 'self') blocks inline scripts.
try{if(location.hash||location.search)history.replaceState(null,'',location.pathname);}catch(e){}
