const assert = require('node:assert/strict');
// Somente leitura. Requer API, frontend e a base de exemplo do projeto em execução.
const base = (process.env.DASHBOARD_URL || 'http://127.0.0.1:3000') + '/api/dashboard/';
async function get(route, params={}, status=200) {
 const r=await fetch(base+route+'?'+new URLSearchParams(params));
 assert.equal(r.status,status,route+' '+await (r.status===status ? Promise.resolve('') : r.text()));
 return status===200 ? r.json() : null;
}
(async()=>{
 const users=await get('usuarios');
 const u=users.find(x=>x.id===(process.env.DASHBOARD_EMPLOYEE || 'C01-E0001')); assert.ok(u,'Pessoa de referência deve existir na base');
 const q={perfil:'administrador',usuarioId:u.id,escopo:'empresa',periodo:5};
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
 await get('resumo',{...q,perfil:'colaborador'},403);
 const outside=users.find(x=>x.companyId!==u.companyId);
 await get('resumo',{...q,escopo:'pessoal',employeeId:outside.id},403);
 const personal=await get('resumo',{...q,escopo:'pessoal',employeeId:u.id});
 assert.ok(personal.scoresIndividuais);
 const manager=await get('resumo',{...q,perfil:'gestor',escopo:'equipe'});
 assert.ok(manager.metricas.requisicoes<=r.metricas.requisicoes);
 const none=await get('integrantes',{...q,busca:'___nao_existe___'}); assert.equal(none.totalElements,0);
 console.log(JSON.stringify({passed:true,users:users.length,periods:f.periodos,company:r.metricas,areas:areas.totalElements,people:rows.totalElements,personal:personal.metricas,manager:manager.metricas},null,2));
})().catch(e=>{console.error(e);process.exitCode=1});
