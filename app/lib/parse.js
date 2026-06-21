// Real-time alert parsing. Bank-specific senders and domain names live in
// banks.js; this module only knows how to pull fields out of an alert body.
const { DOMAIN_NAMES, ALERT_ADAPTERS } = require('./banks');
function strip(html){return html.replace(/<[^>]+>/g,' ').replace(/&amp;/g,'&').replace(/&nbsp;/g,' ').replace(/&#39;/g,"'").replace(/\s+/g,' ').trim();}
function clean(m){return m.replace(/\s+(IDN|SGP|USA)\b.*$/,'').replace(/\s{2,}/g,' ').trim();}
function parseCimbAlert(text){
  const amt=text.match(/Jumlah Transaksi\s*:\s*([\d,]+\.\d{2})/);
  const dt=text.match(/Transaksi\s*:\s*(\d{4}-\d{2}-\d{2})/);
  const mc=text.match(/Nama Merchant\s*:\s*(.+?)\s+Apabila/);
  if(!amt||!dt) return null;
  return {date:dt[1],amount:-parseFloat(amt[1].replace(/,/g,'')),merchant:mc?clean(mc[1]):'Card transaction',account:'Card',source:'alert',status:'pending'};
}
function htmlPart(p){
  if((p.mimeType==='text/html'||p.mimeType==='text/plain')&&p.body&&p.body.data) return p.body.data;
  for(const c of (p.parts||[])){const r=htmlPart(c); if(r) return r;}
  return null;
}
const MON={jan:'01',feb:'02',mar:'03',apr:'04',may:'05',mei:'05',jun:'06',jul:'07',aug:'08',agu:'08',agt:'08',sep:'09',oct:'10',okt:'10',nov:'11',dec:'12',des:'12'};
function pdate(s){s=(s||'').trim();let m;if(m=s.match(/^(\d{4})-(\d{2})-(\d{2})/))return s.slice(0,10);if(m=s.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})/))return m[3]+'-'+m[2].padStart(2,'0')+'-'+m[1].padStart(2,'0');if(m=s.match(/^(\d{1,2})\s+([A-Za-z]{3,})\s+(\d{4})/)){const mo=MON[m[2].slice(0,3).toLowerCase()];if(mo)return m[3]+'-'+mo+'-'+m[1].padStart(2,'0');}return new Date().toISOString().slice(0,10);}
function amtP(s){s=String(s).trim();if(/^\d{1,3}(\.\d{3})+(,\d{1,2})?$/.test(s))return parseFloat(s.replace(/\./g,'').replace(',','.'));if(/^\d{1,3}(,\d{3})+(\.\d{1,2})?$/.test(s))return parseFloat(s.replace(/,/g,''));return parseFloat(s.replace(/[, ]/g,''))||0;}
function fld(t,re){const m=t.match(re);return m?m[1].trim():'';}
function ntok(s){return (s||'').toUpperCase().replace(/[^A-Z ]/g,' ').split(/\s+/).filter(w=>w.length>=3);}
function parsePermataAlert(subject,txt){
  const nominal=fld(txt,/Nominal\s*:\s*(?:IDR|Rp)\.?\s*([\d.,]+)/i)||fld(txt,/(?:Jumlah|Nilai|Total)(?:\s*Transaksi)?\s*:\s*(?:IDR|Rp)\.?\s*([\d.,]+)/i);
  if(!nominal)return null; const amt=amtP(nominal); if(!amt)return null;
  const incoming=/incoming transfer|transfer masuk/i.test((subject||'')+' '+txt);
  const date=pdate(fld(txt,/Tanggal\s*:\s*(\d{1,2}\/\d{1,2}\/\d{4}|\d{1,2}\s+[A-Za-z]{3,}\s+\d{4})/));
  const kat=fld(txt,/Kategori\s*:\s*(.+?)\s+(?:Bank Tujuan|Tipe|Nama|Customer|Rekening|Kategori Isi|Nominal|Nomor)/);
  const recv=fld(txt,/Nama Penerima\s*:\s*(.+?)\s+(?:Nominal|Bank|Rekening|Tanggal|Tipe|Biaya)/);
  const send=fld(txt,/Nama Pengirim\s*:\s*(.+?)\s+(?:Rekening|Nominal|Bank|Tanggal)/);
  const merch=fld(txt,/Nama Merchant\s*:\s*(.+?)\s+(?:Nominal|Lokasi|Kota|Tanggal|Customer|Biaya)/);
  const owner=fld(txt,/Dibuat oleh\s*:\s*(.+?)\s+(?:Ketentuan|Status|Semoga|$)/);
  let cls,merchant,sign;
  if(incoming){cls='transfer_in';sign=1;merchant=send||'Incoming transfer';}
  else{sign=-1;
    if(/isi ulang|top ?up/i.test(kat)){cls='topup';merchant=(kat||'Top up');}
    else if(/qr/i.test(kat)){cls='spend';merchant=merch||'QR payment';}
    else if(/virtual account|pembayaran|payment|tagihan|bill/i.test(kat)){cls='spend';merchant=recv||merch||'Bill payment';}
    else if(/transfer/i.test(kat)){const ot=ntok(owner),rt=ntok(recv);const internal=ot.length&&rt.length&&rt.some(w=>ot.includes(w));cls=internal?'transfer':'spend';merchant=recv||'Transfer';}
    else{cls='spend';merchant=recv||merch||kat||'Transaction';}
  }
  return {date,amount:sign*amt,merchant:clean(merchant),account:'Bank',source:'alert',class:cls,status:'pending'};
}
function classifyPair(txns){
  txns.forEach(function(t){if(!t.class)t.class=(t.amount>0?'income':'spend');});
  for(let i=0;i<txns.length;i++){const a=txns[i];if(a._p)continue;
    for(let j=0;j<txns.length;j++){if(i===j)continue;const b=txns[j];if(b._p)continue;
      if(a.account!==b.account&&Math.abs(a.amount+b.amount)<1&&Math.abs(a.amount)>0){
        const dd=Math.abs((new Date(a.date)-new Date(b.date))/86400000);
        if(dd<=3){a.class='transfer';b.class='transfer';a._p=b._p=true;break;}
      }
    }
  }
  txns.forEach(function(t){delete t._p;});
  return txns;
}
function bankFromDomain(from){var f=(from||'').toLowerCase();for(var i=0;i<DOMAIN_NAMES.length;i++){if(f.indexOf(DOMAIN_NAMES[i][0])>=0)return DOMAIN_NAMES[i][1];}var m=f.match(/@([^.>\s]+)/);return m?m[1].charAt(0).toUpperCase()+m[1].slice(1):'Bank';}
function parseGenericAlert(subject,text,from){
  var f=(from||'').toLowerCase(),all=((subject||'')+' '+(text||'')).toLowerCase();
  if(/promo|highlight|newsletter|news@|marketing|promosi|blast|ebanking-?news/.test(f))return null;
  if(/one[- ]?time password|\botp\b|kode verifikasi|verification code|kode otp/.test(all))return null;
  if(/nikmati|promo|diskon|voucher|kupon|undian|hadiah|penawaran|dapatkan|berlaku hingga|berlaku s\/d|syarat (dan|&) ketentuan|\bs&k\b|unsubscribe|berhenti berlangganan|daftar sekarang|klik di sini|hingga rp|up to rp|poin hingga|%\s*(off|cashback|disc)/.test(all))return null;
  var am=text.match(/(?:Nominal|Jumlah(?:\s*Transaksi)?|Amount|Nilai\s*Transaksi|Total\s*(?:Transaksi|Pembayaran|Bayar))\s*:?\s*(?:IDR|Rp)?\.?\s*([\d.,]+)/i);
  if(!am&&/transaksi berhasil|telah melakukan transaksi|berhasil melakukan|\bdebit\b|\bkredit\b|saldo|e-?receipt|struk|pembayaran berhasil/.test(all))am=text.match(/(?:Rp|IDR)\.?\s*([\d][\d.,]{2,})/i);
  if(!am)return null;
  var amt=amtP(am[1]);if(!amt||amt<100)return null;
  var dm=text.match(/(\d{4}-\d{2}-\d{2})/)||text.match(/(\d{1,2}\/\d{1,2}\/\d{4})/)||text.match(/(\d{1,2}\s+[A-Za-z]{3,}\s+\d{4})/);
  var date=dm?pdate(dm[1]):new Date().toISOString().slice(0,10);
  var cls,sign;
  if(/incoming|transfer masuk|dana masuk|uang masuk|credited|\brefund\b|received|penerimaan|setoran|masuk ke rekening/.test(all)){cls='transfer_in';sign=1;}
  else if(/top ?up|isi ulang|reload/.test(all)){cls='topup';sign=-1;}
  else if(/transfer|\btrf\b|kirim uang|pengiriman dana|sent to|ke rekening tujuan/.test(all)){cls='transfer';sign=-1;}
  else{cls='spend';sign=-1;}
  var mer=fld(text,/(?:Nama Merchant|Merchant|Nama Penerima|Penerima|Beneficiary|Tujuan|Kepada|Nama Toko|Toko)\s*:?\s*(.+?)\s+(?:Nominal|Jumlah|Tanggal|Rp|IDR|Amount|Bank|Rekening|Status|Nomor|Ref|Tipe|$)/i);
  if(!mer){mer=(subject||'').replace(/^(re|fwd):\s*/i,'').replace(/[\[\(][^\])]*[\]\)]/g,'').replace(/notifikasi|notification|transaksi|transaction|alert|informasi|berhasil/ig,'').trim();}
  if(cls==='transfer_in')mer=/bi.?fast|remit/i.test(all)?'Incoming BI-FAST':'Incoming transfer';if(mer&&mer.length>40)mer=mer.slice(0,38).trim();return {date:date,amount:sign*amt,merchant:clean(mer||'Transaction'),account:bankFromDomain(from),source:'alert',class:cls,status:'pending'};
}
function parseAlert(from,subject,text){
  var f=(from||'').toLowerCase();
  for(var i=0;i<ALERT_ADAPTERS.length;i++){var ad=ALERT_ADAPTERS[i];if(f.indexOf(ad.sender)<0)continue;
    if(ad.parser==='cimb'){var t=parseCimbAlert(text);if(t){t.class='spend';t.account=bankFromDomain(from);}return t;}
    if(ad.parser==='permata'){var p=parsePermataAlert(subject,text);if(p)p.account=bankFromDomain(from);return p;}
  }
  return parseGenericAlert(subject,text,from);
}
module.exports={strip,clean,parseCimbAlert,htmlPart,parsePermataAlert,classifyPair,parseGenericAlert,parseAlert,bankFromDomain};
