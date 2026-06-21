async function extractText(buf, password){
  const pdfjs = await import('pdfjs-dist/legacy/build/pdf.mjs');
  const doc = await pdfjs.getDocument({ data:new Uint8Array(buf), password, isEvalSupported:false, useWorkerFetch:false, disableFontFace:true, useSystemFonts:false }).promise;
  let t=''; const NP=Math.min(doc.numPages,12); for(let i=1;i<=NP;i++){ const pg=await doc.getPage(i); const c=await pg.getTextContent(); t+=' '+c.items.map(x=>x.str).join(' '); }
  return t.replace(/\s+/g,' ');
}
const N=s=>parseFloat(s.replace(/,/g,''));
const { STATEMENT_ADAPTERS } = require('./banks');
async function readStatements(mbox, pwMap){
  const accounts=[];
  for(const ad of STATEMENT_ADAPTERS){
    const pw = ad.pwKey ? pwMap[ad.pwKey] : '';
    if(ad.pwKey && !pw) continue;
    try{
      const refs=await mbox.search(Object.assign({max:3}, ad.match)); if(!refs.length) continue;
      let pdf=null;
      for(const r of refs){ const msg=await mbox.fetch(r.uid,{attachments:true}); const f=(msg.attachments||[]).find(a=>((a.filename||'').toLowerCase().endsWith('.pdf'))||a.mimeType==='application/pdf'); if(f){ pdf=f; break; } }
      if(!pdf) continue;
      const text=await extractText(pdf.data, pw||undefined);
      const txns=parseStatementTxns(text);
      const an=text.match(/(?:Account No\.?|No\.?\s*Rekening|Card No\.?|No\.?\s*Kartu)\s*[:\-]?\s*([\dXx*]{6,})/i);
      const pd=text.match(/(\d{1,2}\s+\w{3,}\s+\d{4})\s*[-\u2013]\s*(\d{1,2}\s+\w{3,}\s+\d{4})/);
      for(const def of (ad.accounts||[])){ const m=text.match(def.re); if(!m) continue; const a={institution:ad.institution,name:def.name,type:def.type,balance:(def.sign||1)*parseAmt(m[1]),source:'statement',bank:ad.id,uid:'s_'+ad.id}; if(txns.length) a.txns=txns; if(an) a.acctNo=an[1]; if(pd) a.period=pd[1]+' - '+pd[2]; accounts.push(a); break; }
    }catch(e){ console.error('stmt '+ad.id, (e&&e.stack)||e); accounts.push({bank:ad.id,error:'parse_failed'}); }
  }
  return accounts;
}
async function discoverBanks(mbox){
  const seen={}; const out=[];
  for(const ad of STATEMENT_ADAPTERS){
    if(!ad.pwKey || seen[ad.pwKey]) continue;
    try{ const refs=await mbox.search(Object.assign({max:1}, ad.match)); if(refs && refs.length){ seen[ad.pwKey]=1; out.push({key:ad.pwKey, institution:ad.institution||ad.pwKey}); } }catch(e){}
  }
  return out;
}
function parseAmt(s){ s=String(s).trim().replace(/^[.,]+|[.,]+$/g,'');
  if(/^\d{1,3}(\.\d{3})+(,\d{1,2})?$/.test(s)) return parseFloat(s.replace(/\./g,'').replace(',','.'));
  if(/^\d{1,3}(,\d{3})+(\.\d{1,2})?$/.test(s)) return parseFloat(s.replace(/,/g,''));
  if(/^\d+,\d{1,2}$/.test(s)) return parseFloat(s.replace(',','.'));
  return parseFloat(s.replace(/[, ]/g,''))||0;
}
function detectAccounts(text){
  const out=[]; let m;
  for(const ad of STATEMENT_ADAPTERS){ if(ad.guard && !ad.guard.test(text)) continue; for(const def of (ad.accounts||[])){ if(m=text.match(def.re)){ out.push({institution:ad.institution,name:def.name,type:def.type,balance:(def.sign||1)*parseAmt(m[1])}); break; } } }
  if(!out.length && /Rekening Koran|Ringkasan Rekening|Account Summary/i.test(text)){const rows=[...text.matchAll(/([A-Za-z][A-Za-z ]{2,28}?)\s+(IDR|USD|SGD|EUR)\s+\d+\s+([\d.,]+)\s+([\d.,]+)/g)];for(const r of rows){const v=parseAmt(r[4]);if(v>0)out.push({institution:'Statement',name:r[1].trim(),type:'bank',balance:v,ccy:r[2]});}if(!out.length){const tm=text.match(/Rekening Simpanan Total ([\d.,]+)/i);if(tm){const v=parseAmt(tm[1]);if(v>0)out.push({institution:'Statement',name:'Savings account',type:'bank',balance:v,ccy:'IDR'});}}}
  if(!out.length){
    var CCY=(/\bIDR\b|Rp\b|rupiah/i.test(text)?'IDR':/\bSGD\b|S\$/.test(text)?'SGD':/\bEUR\b|€/.test(text)?'EUR':/\bGBP\b|£/.test(text)?'GBP':/\bAUD\b|A\$/.test(text)?'AUD':(/\bUSD\b|US\$|\$/.test(text)?'USD':'IDR'));
    var lastPos=function(re){var c=[...text.matchAll(re)],v=0;for(var k=0;k<c.length;k++){var p=parseAmt(c[k][1]);if(p>0)v=p;}return v;};
    var isCard=/(credit card|kartu kredit|minimum payment|pembayaran minimum|total tagihan|amount due|payment due date|tanggal jatuh tempo|credit limit|limit kredit)/i.test(text);
    if(isCard){
      var cv=lastPos(/(?:new balance|statement balance|total balance due|total amount due|total tagihan(?: saat ini)?(?: \(rp\))?|amount due|outstanding(?: balance)?|current balance)[^\d\-]{0,16}(?:rp|idr|usd|sgd|\$)?\s*([\d.,]+)/ig);
      if(cv>0) out.push({institution:'Statement',name:'Credit card',type:'credit_card',balance:-cv,ccy:CCY});
    }
    if(!out.length){
      var v=lastPos(/(?:closing balance|ending balance|ending daily balance|saldo akhir(?: tabungan)?|baki akhir)[^\d\-]{0,16}(?:rp|idr|usd|sgd|\$)?\s*([\d.,]+)/ig);
      if(!v) v=lastPos(/(?:available balance|ledger balance|current balance|account balance|total balance|cash balance|portfolio value|market value|net asset value|nilai portofolio)[^\d\-]{0,16}(?:rp|idr|usd|sgd|\$)?\s*([\d.,]+)/ig);
      if(v>0){
        var inv=/(portfolio|securities|brokerage|rekening efek|rekening dana|\brdn\b|market value|net asset value|nilai portofolio|efek)/i.test(text);
        var sav=/(saving|tabungan|payroll|deposito|time deposit|fixed deposit|giro)/i.test(text);
        var tp=inv?'investment':(sav?'savings':'bank');
        out.push({institution:'Statement',name:(inv?'Investment account':(sav?'Savings account':'Bank account')),type:tp,balance:v,ccy:CCY});
      }
    }
  }
  return out;
}
function cleanDesc(s){
  s=String(s||'').replace(/\b\d{1,2}:\d{2}(?::\d{2})?\b/g,' ').replace(/\b\d{6,}\b/g,' ').replace(/\s{2,}/g,' ').trim();
  var low=s.toLowerCase();
  if(/payroll|gaji|salary/.test(low))return 'Salary';
  if(/pendapatan bunga|\bbunga\b|interest/.test(low))return 'Interest';
  if(/biaya sms|sms notif/.test(low))return 'SMS notification fee';
  if(/biaya adm|admin fee|\badm\b|monthly fee|biaya bulanan/.test(low))return 'Bank admin fee';
  if((/bill\s*payment|billpayment|pembayaran/.test(low))&&/ccard|credit\s*card|kartu\s*kredit|\bc card\b|\bcc\b/.test(low))return 'Credit card payment';
  if(/\bgoogle\b/.test(low)){var g=s.replace(/[*#]/g,' ').replace(/g\.?co\/?\w*|help.?pay\w*/ig,' ').replace(/mountain\s*view|\bmountain\b/ig,' ').replace(/\b(us|usa|ca|sg)\b/ig,' ').replace(/\bgoogle\b/ig,' ').replace(/\s{2,}/g,' ').trim();g=g.toLowerCase().replace(/\b[a-z]/g,function(c){return c.toUpperCase();});g=('Google '+g).replace(/\s{2,}/g,' ').trim();return g.slice(0,38);}
  if(/shopee\s*pay|shopeepay/.test(low))return 'ShopeePay';
  if(/shopee/.test(low))return 'Shopee';
  if(/tokopedia/.test(low))return 'Tokopedia';
  if(/\bgojek\b|gopay/.test(low))return 'Gojek';
  if(/\bgrab\b/.test(low))return 'Grab';
  if(/netflix/.test(low))return 'Netflix';
  if(/spotify/.test(low))return 'Spotify';
  if(/\bapple\b|itunes/.test(low))return 'Apple';
  if(/\bpln\b|token listrik|prepaid.*listrik|listrik/.test(low))return 'PLN electricity';
  if(/\bpdam\b|tagihan air/.test(low))return 'Water bill';
  if(/pulsa|telkomsel|indosat|\bxl\b|smartfren|by\.u|byu/.test(low))return 'Mobile top-up';
  var isQR=/\bqr(is)?\b|\bqrs\b|\bqrd\b|qr purchase/.test(low);
  var isTransfer=/overbo[oe]king|\btransfer\b|\btrf\b|bifast|\brtgs\b|\bskn\b|kliring/.test(low);
  var isTopup=/top\s*up|topup|isi ulang|reload/.test(low);
  var isWd=/tarik tunai|penarikan|withdrawal|cash withdrawal/.test(low);
  var ch=/octo\s*mobile|octomobile/.test(low)?'OctoMobile':(/atm/.test(low)?'ATM':(/m-?bank|mobile bank|\bib\b|internet bank/.test(low)?'Mobile':''));
  var rest=s.replace(/[*#]/g,' ');
  if(/atm/.test(low))rest=rest.replace(/\b(atm|prima|bersama|alto|link)\b/ig,' ');
  if(isQR)rest=rest.replace(/\b(qris|qrs|qrd|qr|sa|off us|on us|off|on)\b/ig,' ');
  rest=rest.replace(/\b(overbo[oe]king|purchase|pembelian|payment|pembayaran|billpayment|bill payment|transfer|trf|octo\s*mobile|octomobile|bifast|rtgs|skn|kliring|via|ref no?|trx id?|edc|pos|debit|kredit|topup|top up|isi ulang|reload|tarik tunai|penarikan|withdrawal|to|ke|kepada|dari|from|cr|dr|idr|rp|usd|dom|intl)\b/ig,' ')
    .replace(/g\.?co\/?\w*|help.?pay\w*|mountain\s*view/ig,' ')
    .replace(/[^A-Za-z& ]/g,' ').replace(/\b[a-z]\b/ig,' ').replace(/\s{2,}/g,' ').trim();
  rest=rest.replace(/\s+(idn|id|idr|sg|sgp|us|usa|my|mys|au|jp)\s*$/i,'').replace(/\s+(kota|kab|ko)\s*$/i,'').trim();
  rest=rest.replace(/\b([A-Za-z]{3,})\s+\1\b/gi,'$1').replace(/\s{2,}/g,' ').trim();
  if(rest&&rest.replace(/[^a-z]/ig,'').length>=3)return rest.toLowerCase().replace(/\b[a-z]/g,function(c){return c.toUpperCase();}).slice(0,38);
  if(isWd)return 'Cash withdrawal';
  if(isTopup)return 'Top-up';
  if(isQR)return 'QRIS payment';
  if(isTransfer)return ch?('Transfer · '+ch):'Bank transfer';
  return 'Transaction';
}
function isPromoLine(d){return /\b(poin|point)\b|kadaluars|kedaluwars|will expire|expired?|redeem|undian|hadiah|voucher|kupon|cashback|nikmati|promo|diskon|berlaku hingga|gratis ongkir|s&k|syarat (dan|&) ketentuan/i.test(d||'');}
function parseTxnsRB(text){
  var out=[];
  var mo=text.match(/(?:SALDO\s+AWAL|opening\s+balance|beginning\s+balance|balance\s+brought\s+forward|previous\s+balance)\s*(?:periode\s*)?(?:rp|idr|usd|sgd|\$)?\s*([\d.,]+)/i);
  if(!mo) return out;
  var prev=parseAmt(mo[1]); var region=text.slice(mo.index);
  var parts=region.split(/(?=(?<![\d\/-])(?:0?[1-9]|[12]\d|3[01])[\/-](?:0?[1-9]|1[0-2])(?:[\/-]\d{2,4})?(?:\s+(?:0?[1-9]|[12]\d|3[01])[\/-](?:0?[1-9]|1[0-2])(?:[\/-]\d{2,4})?)?\s)/);
  var matches=0,total=0;
  for(var i=0;i<parts.length;i++){
    var m=parts[i].trim().match(/^(\d{1,2}[\/-]\d{1,2}(?:[\/-]\d{2,4})?)(?:\s+\d{1,2}[\/-]\d{1,2}(?:[\/-]\d{2,4})?)?\s+([\s\S]+)$/);
    if(!m) continue;
    var rest=m[2];
    var cut=rest.search(/\s(Total|SALDO\s+AKHIR|closing\s+balance|ending\s+balance|PermataBank\.com|Halaman\/Page|Bersambung|Ringkasan)/i);
    if(cut>0) rest=rest.slice(0,cut);
    var nums=rest.match(/\d{1,3}(?:[.,]\d{3})*[.,]\d{2}\b/g)||[];
    if(nums.length<2) continue;
    var bal=parseAmt(nums[nums.length-1]); var amtCol=parseAmt(nums[nums.length-2]);
    var di=rest.lastIndexOf(nums[nums.length-2]);
    var desc=cleanDesc((di>=0?rest.slice(0,di):rest));
    if(isPromoLine(rest)||isPromoLine(desc)) continue;
    var delta=bal-prev; total++;
    if(Math.abs(Math.abs(delta)-amtCol)<Math.max(1,amtCol*0.03)) matches++;
    out.push({date:m[1],desc:desc||'Transaction',amount:Math.round(delta*100)/100,balance:bal});
    prev=bal; if(out.length>500) break;
  }
  if(total>=3 && matches/total<0.6) return [];
  return out;
}

function parseTxnsFlat(text){
  var out=[]; var NUM=/\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{2})?|\d+[.,]\d{2}/g;
  var parts=text.split(/(?=(?<!\d)\d{1,2}[\/\-\s](?:\d{1,2}|jan|feb|mar|apr|mei|may|jun|jul|agu|aug|sep|okt|oct|nov|des|dec)(?:[\/\-\s]\d{2,4})?\s)/i);
  for(var i=0;i<parts.length;i++){
    var p=parts[i].trim();
    var m=p.match(/^(\d{1,2}[\/\-\s](?:\d{1,2}|jan|feb|mar|apr|mei|may|jun|jul|agu|aug|sep|okt|oct|nov|des|dec)(?:[\/\-\s]\d{2,4})?)\s+([\s\S]+)$/i);
    if(!m) continue;
    var rest=m[2];
    var cut=rest.search(/\s(Total|Sub-?total|SALDO|Minimum|Credit Limit|Limit Kredit|Previous Balance|Halaman|Page|Lembar|PermataBank\.com|Bersambung)/i);
    if(cut>0) rest=rest.slice(0,cut);
    if(/credit limit|limit kredit|minimum (payment|pembayaran)|total (tagihan|amount|payment|due)|previous balance|saldo (awal|akhir|tersedia)|available bal/i.test(rest)) continue;
    var nums=rest.match(NUM)||[];
    if(!nums.length) continue;
    var amtStr=nums[nums.length-1]; var amt=parseAmt(amtStr);
    if(!amt||amt<0.01) continue;
    var di=rest.lastIndexOf(amtStr);
    var desc=cleanDesc(rest.slice(0,di));
    if(isPromoLine(rest)||isPromoLine(desc)) continue;
    if(desc.replace(/[^a-zA-Z]/g,'').length<2) continue;
    var credit=/\bCR\b|\bcredit\b|\brefund\b|reversal|koreksi|cashback|diterima|\breceived\b/i.test(rest);
    out.push({date:m[1].replace(/\s+/g,' ').trim(),desc:desc,amount:credit?amt:-amt,balance:0});
    if(out.length>500) break;
  }
  return out.length>=3?out:[];
}
function parseStatementTxns(text){var a=parseTxnsRB(text);if(a.length)return a;return parseTxnsFlat(text);}

module.exports={ readStatements, discoverBanks, extractText, detectAccounts, parseStatementTxns };
