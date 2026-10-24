const assert=require('node:assert/strict');
const origin=process.env.DASHBOARD_URL||'http://127.0.0.1:3000';
const spring=process.env.SPRINGBOOT_URL||'http://127.0.0.1:8080';
async function login(email=process.env.RAIZ_ADMIN_EMAIL||'raiz@gmail.com',senha=process.env.RAIZ_ADMIN_SENHA||'12345',base=origin){
 const r=await fetch(base+'/api/funcionario/login',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({email,senha})});
 assert.equal(r.status,200,'Login de '+email);
 const header=r.headers.get('set-cookie');
 assert.match(header,/HttpOnly/i);assert.match(header,/SameSite=Lax/i);assert.match(header,/Max-Age=28800/i);
 const usuario=await r.json();assert.ok(!('senha' in usuario)&&!('token' in usuario));
 return {cookie:header.split(';')[0],usuario,header};
}
async function get(path,params={},status=200,cookie,base=origin){
 const r=await fetch(base+path+'?'+new URLSearchParams(params),{headers:cookie?{cookie}:{},redirect:'manual'});
 const body=await r.json().catch(()=>null);
 assert.equal(r.status,status,JSON.stringify({path,status:r.status,body}));
 return body;
}
async function logout(cookie,base=origin){
 const r=await fetch(base+'/api/funcionario/logout',{method:'POST',headers:{cookie}});assert.equal(r.status,204);
}
module.exports={origin,spring,login,get,logout};
