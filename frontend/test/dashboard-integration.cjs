const assert = require('node:assert/strict');
const {login,logout}=require('./sessao-helper.cjs');
const contas=require('./contas-demo.json');
let cookie;
// Somente leitura. Requer API, frontend e a base de exemplo do projeto em execução.
const base = (process.env.DASHBOARD_URL || 'http://127.0.0.1:3000') + '/api/dashboard/';
async function get(route, params={}, status=200) {
 const r=await fetch(base+route+'?'+new URLSearchParams(params),{headers:{cookie}});
 assert.equal(r.status,status,route+' '+await (r.status===status ? Promise.resolve('') : r.text()));
 return status===200 ? r.json() : null;
}
(async()=>{
 const admin=await login();cookie=admin.cookie;
 try{
 const users=await get('usuarios');
 const u=users.find(x=>x.id===(process.env.DASHBOARD_EMPLOYEE || 'C01-E0001')); assert.ok(u,'Pessoa de referência deve existir na base');
 const q={companyId:u.companyId,escopo:'empresa',periodo:5};
 const f=await get('filtros',q); assert.deepEqual(f.periodos.map(p=>p.dias),[90,90,90,90,66]);
 const r=await get('resumo',q); assert.equal(r.evolucao.length,5);
 const rows=await get('integrantes',{...q,size:100});
 const areas=await get('integrantes',{...q,agrupamento:'area',size:100});
 for(const key of ['requisicoes','tokens','issues','prs']) assert.equal(areas.content.reduce((s,x)=>s+x[key],0),r.metricas[key],key);
 const tool=f.ferramentas[0].id;
 const filtered=await get('resumo',{...q,aiToolId:tool});
 assert.equal(filtered.metricas.issues,r.metricas.issues);
 assert.equal(filtered.metricas.prs,r.metricas.prs);
 assert.ok(filtered.metricas.requisicoes<=r.metricas.requisicoes);
 await get('resumo',{...q,periodo:6},400);
 const outside={id:contas.colaborador.employeeId};
 await get('resumo',{...q,escopo:'pessoal',employeeId:outside.id},403);
 const personal=await get('resumo',{...q,escopo:'pessoal',employeeId:u.id});
 assert.ok(personal.scoresIndividuais);
 const gestor=await login(contas.gestorMaior.email,contas.gestorMaior.senha);cookie=gestor.cookie;
 let manager;
 try{
  manager=await get('resumo',{escopo:'equipe',periodo:5});
  await get('resumo',{escopo:'empresa',perfil:'administrador',usuarioId:'ROOT-E0001'},403);
 }finally{await logout(gestor.cookie);cookie=admin.cookie;}
 const none=await get('integrantes',{...q,busca:'___nao_existe___'}); assert.equal(none.totalElements,0);
 console.log(JSON.stringify({passed:true,users:users.length,periods:f.periodos,company:r.metricas,areas:areas.totalElements,people:rows.totalElements,personal:personal.metricas,manager:manager.metricas},null,2));
 }finally{await logout(admin.cookie);}
})().catch(e=>{console.error(e);process.exitCode=1});
