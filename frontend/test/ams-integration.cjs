const assert=require('node:assert/strict');
// Somente leitura. Requer a API e o frontend em execução.
const origin=process.env.DASHBOARD_URL||'http://127.0.0.1:3000';
const spring=process.env.SPRINGBOOT_URL||'http://127.0.0.1:8080';
async function get(path,params={},status=200,base=origin){
 const r=await fetch(base+path+'?'+new URLSearchParams(params));const body=await r.json();
 assert.equal(r.status,status,JSON.stringify({path,body}));return body;
}
function checkCalculation(c){
 const used=c.dimensoes.filter(d=>d.pesoEfetivo>0);
 if(c.nota!=null){
  assert.ok(c.nota>=0&&c.nota<=100);
  assert.ok(Math.abs(used.reduce((s,d)=>s+d.pesoEfetivo,0)-1)<1e-9);
  assert.ok(Math.abs(used.reduce((s,d)=>s+d.utilizada*d.pesoEfetivo,0)-c.nota)<1e-8);
 }
 assert.equal(c.dimensoes.length,c.desenvolvedor?5:4);
 if(!c.desenvolvedor){assert.ok(c.dimensoes.every(d=>d.id!=='delivery'));assert.equal(c.delivery.length,0);}
 for(const d of c.dimensoes){
  if(d.registrada==null&&d.id==='literacy'){assert.equal(d.utilizada,0);assert.equal(d.estado,'zero_por_ausencia');}
  if(d.registrada==null&&d.id!=='literacy'){assert.equal(d.utilizada,null);assert.equal(d.pesoEfetivo,0);}
 }
}
(async()=>{
 const users=await get('/api/dashboard/ams/usuarios');
 const dev=users.find(p=>/Desenvolvedor|Engenheiro\(a\) de Software/i.test(p.cargo));
 const nondev=users.find(p=>p.id==='C01-E0001')||users.find(p=>/Marketing|DevOps/i.test(p.cargo));
 assert.ok(dev&&nondev);
 const q={perfil:'administrador',usuarioId:nondev.id,periodo:5};
 const all=await get('/api/dashboard/ams',q);
 assert.equal(all.evolucao.length,5);assert.equal(all.empresas.length,new Set(users.map(p=>p.companyId)).size);
 assert.equal(all.distribuicao.reduce((s,x)=>s+x.quantidade,0),all.resumo.pessoas);
 const mean=all.empresas.reduce((s,g)=>{const r=g.evolucao[4].resumo;return s+(r.nota??0)*r.validos;},0)/all.resumo.validos;
 assert.ok(Math.abs(mean-all.resumo.nota)<1e-8);
 for(const person of [dev,nondev]){
  const personal=await get('/api/dashboard/ams',{perfil:'colaborador',usuarioId:person.id,periodo:5});
  personal.evolucao.forEach(p=>checkCalculation(p.pessoal));
  const individual=await get('/score/ams/'+person.id,{periodo:5},200,spring);
  assert.equal(individual.periodo.id,5);
  assert.equal(individual.pessoa.id,person.id);
  assert.ok(Math.abs(individual.calculo.nota-personal.pessoal.nota)<1e-8);
  for(const [id,route,field]of[['security','security-incidents','securityScore'],['governance','governance','governanceScore'],['literacy','ai-literacy','aiLiteracyScore']]){
   const existing=await get('/score/'+route+'/'+person.id,{periodo:5},200,spring);
   const registered=personal.pessoal.dimensoes.find(d=>d.id===id).registrada;
   if(existing[field]==null)assert.equal(registered,null);else assert.ok(Math.abs(existing[field]-registered)<1e-7,id+' deve manter a regra existente');
  }
 }
 const team=await get('/api/dashboard/ams',{...q,perfil:'gestor'});
 assert.equal(team.empresas.length,0);
 assert.ok(team.integrantes.content.every(l=>l.pessoa.companyId===nondev.companyId&&l.pessoa.departmentId===nondev.departmentId));
 const outside=users.find(p=>p.companyId!==nondev.companyId);
 await get('/api/dashboard/ams',{...q,perfil:'gestor',companyId:outside.companyId},403);
 await get('/api/dashboard/ams',{...q,perfil:'colaborador',employeeId:dev.id===nondev.id?outside.id:dev.id},403);
 await get('/api/dashboard/ams',{...q,periodo:0},400);
 await get('/api/dashboard/ams',{...q,page:-1},400);
 const empty=await get('/api/dashboard/ams',{...q,busca:'__nome_inexistente__'});assert.equal(empty.integrantes.totalElements,0);
 assert.equal(empty.resumo.pessoas,all.resumo.pessoas,'Busca da tabela não muda o agregado do recorte');
 console.log(JSON.stringify({passed:true,people:all.resumo.pessoas,companies:all.empresas.length,ams:all.resumo.nota,missing:all.resumo.notasAusentes,dev:dev.id,nondev:nondev.id},null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
