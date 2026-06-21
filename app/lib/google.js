const SCOPES = ['openid','email','profile','https://www.googleapis.com/auth/gmail.readonly'];
const tokenURL = 'https://oauth2.googleapis.com/token';
function authUrl(state){
  const p = new URLSearchParams({client_id:process.env.GOOGLE_CLIENT_ID,redirect_uri:process.env.GOOGLE_REDIRECT_URI,response_type:'code',scope:SCOPES.join(' '),access_type:'offline',prompt:'consent',state});
  return 'https://accounts.google.com/o/oauth2/v2/auth?'+p.toString();
}
async function exchangeCode(code){
  const r=await fetch(tokenURL,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({code,client_id:process.env.GOOGLE_CLIENT_ID,client_secret:process.env.GOOGLE_CLIENT_SECRET,redirect_uri:process.env.GOOGLE_REDIRECT_URI,grant_type:'authorization_code'})});
  if(!r.ok) throw new Error('token '+r.status); return r.json();
}
async function refresh(rt){
  const r=await fetch(tokenURL,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({refresh_token:rt,client_id:process.env.GOOGLE_CLIENT_ID,client_secret:process.env.GOOGLE_CLIENT_SECRET,grant_type:'refresh_token'})});
  if(!r.ok) throw new Error('refresh '+r.status); return (await r.json()).access_token;
}
async function userInfo(at){const r=await fetch('https://www.googleapis.com/oauth2/v2/userinfo',{headers:{Authorization:'Bearer '+at}});if(!r.ok)throw new Error('userinfo '+r.status);return r.json();}
async function gmailList(at,q,max=20){const r=await fetch('https://gmail.googleapis.com/gmail/v1/users/me/messages?q='+encodeURIComponent(q)+'&maxResults='+max,{headers:{Authorization:'Bearer '+at}});if(!r.ok)throw new Error('gmail list '+r.status);return r.json();}
async function gmailGet(at,id){const r=await fetch('https://gmail.googleapis.com/gmail/v1/users/me/messages/'+id+'?format=full',{headers:{Authorization:'Bearer '+at}});if(!r.ok)throw new Error('gmail get '+r.status);return r.json();}
async function gmailAttachment(at,msgId,attId){const r=await fetch('https://gmail.googleapis.com/gmail/v1/users/me/messages/'+msgId+'/attachments/'+attId,{headers:{Authorization:'Bearer '+at}});if(!r.ok)throw new Error('att '+r.status);return (await r.json()).data;}
module.exports={SCOPES,authUrl,exchangeCode,refresh,userInfo,gmailList,gmailGet,gmailAttachment};
