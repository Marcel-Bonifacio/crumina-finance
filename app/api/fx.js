module.exports = async (req,res) => {
  res.setHeader('Content-Type','application/json');
  res.setHeader('Cache-Control','public, max-age=3600');
  try{
    const url=new URL(req.url,'https://'+req.headers.host);
    const base=(url.searchParams.get('base')||'IDR').toUpperCase().replace(/[^A-Z]/g,'').slice(0,3)||'IDR';
    const r=await fetch('https://open.er-api.com/v6/latest/'+base);
    if(!r.ok) throw new Error('fx '+r.status);
    const j=await r.json();
    res.end(JSON.stringify({base, rates:j.rates||{}}));
  }catch(e){ res.setHeader('Cache-Control','no-store'); res.statusCode=200; res.end(JSON.stringify({base:'IDR',rates:{}})); }
};
