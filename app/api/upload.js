const { fromReq } = require('../lib/session');
const { extractText, detectAccounts, parseStatementTxns } = require('../lib/statements');
const { rateLimit } = require('../lib/ratelimit');
module.exports = async (req,res) => {
  const email=fromReq(req); res.setHeader('Content-Type','application/json');
  if(!email){res.statusCode=401;return res.end('{"error":"unauthorized"}');}
  if(req.method!=='POST'){res.statusCode=405;return res.end('{"error":"method"}');}
  if(!rateLimit(req,res,{max:10}))return;
  let body=''; for await(const c of req){ body+=c; if(body.length>6*1024*1024){res.statusCode=413;return res.end('{"error":"too_large"}');} }
  let pdfB64,password; try{const j=JSON.parse(body);pdfB64=j.pdf;password=j.password;}catch(e){res.statusCode=400;return res.end('{"error":"bad_body"}');}
  if(!pdfB64){res.statusCode=400;return res.end('{"error":"no_file"}');}
  try{
    const buf=Buffer.from(String(pdfB64).replace(/^data:[^,]*,/,''),'base64');
    if(buf.length>4*1024*1024){res.statusCode=413;return res.end('{"error":"too_large"}');}
    const text=await extractText(buf, password||undefined);
    if(!text || text.replace(/\s/g,'').length<40) return res.end(JSON.stringify({ok:true, accounts:[], note:'no_text'}));
    const accounts=detectAccounts(text).map(a=>({...a, source:'upload', ccy:a.ccy||'IDR'}));
    if(accounts.length){ const txns=parseStatementTxns(text); if(txns.length) accounts[0].txns=txns; const an=text.match(/(?:Currency\s+(\d{8,16})|Account No\.?\s*[:\-]?\s*(\d{8,16})|No\.\s*Rekening\s*[:\-]?\s*(\d{8,16}))/i); if(an) accounts[0].acctNo=(an[1]||an[2]||an[3]); const pd=text.match(/(\d{1,2}\s+\w+\s+\d{4})\s*[-–]\s*(\d{1,2}\s+\w+\s+\d{4})/); if(pd) accounts[0].period=pd[1]+' - '+pd[2]; }
    if(!accounts.length) return res.end(JSON.stringify({ok:true, accounts:[], note:'no_balance_found'}));
    res.end(JSON.stringify({ok:true, accounts}));
  }catch(e){ const msg=String((e&&e.message)||e); const pw=/password|encrypt/i.test(msg);
    res.statusCode=200; res.end(JSON.stringify({ok:false, error: pw?'wrong_or_missing_password':'could_not_read'})); }
};
