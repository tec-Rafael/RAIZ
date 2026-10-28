const assert=require('node:assert/strict');
const contas=require('./contas-demo.json');
const {origin,spring,login,get,logout}=require('./sessao-helper.cjs');
(async()=>{
 for(const path of ['/','/dashboard','/perfil','/funcionario/listar']){
  const r=await fetch(origin+path,{redirect:'manual'});assert.equal(r.status,302);assert.equal(r.headers.get('location'),'/login');
 }
 for(const base of [origin,spring])await get('/api/dashboard/ams',{},401,undefined,base);
 await get('/score/ams/'+contas.desenvolvedor.employeeId,{periodo:5},401,undefined,spring);
 const admin=await login();
 try{
  assert.equal(admin.usuario.posicao,'ADMINISTRADOR');assert.equal(admin.usuario.employeeId,'ROOT-E0001');
  const pessoas=await get('/api/funcionario/listar',{},200,admin.cookie);
  assert.equal(pessoas.filter(p=>p.posicao==='ADMINISTRADOR').length,1);
  const all=await get('/api/dashboard/ams',{periodo:5,size:1},200,admin.cookie);
  assert.ok(all.empresas.length>1);
  const corpo={gestorId:contas.gestorMaior.employeeId,nome:'Teste de permissão',email:'novo.teste@raiz.local',senha:'123456',posicao:'ADMINISTRADOR'};
  const criar=await fetch(origin+'/api/funcionario/criar',{method:'POST',headers:{cookie:admin.cookie,'content-type':'application/json'},body:JSON.stringify(corpo)});
  assert.equal(criar.status,403,'Nenhum segundo administrador pode ser criado');
  const remover=await fetch(origin+'/api/funcionario/deletar/ROOT-E0001',{method:'DELETE',headers:{cookie:admin.cookie}});assert.equal(remover.status,403);
 }finally{await logout(admin.cookie);}
 for(const tipo of ['desenvolvedor','colaborador','gestorMaior','gestorMenor']){
  const conta=contas[tipo],sessao=await login(conta.email,conta.senha);
  try{
   const gestor=tipo.startsWith('gestor');
   const f=await get('/api/dashboard/ams/filtros',{perfil:'administrador',usuarioId:'ROOT-E0001'},200,sessao.cookie);
   assert.equal(f.usuario.id,conta.employeeId,'Identidade enviada no query não substitui a sessão');
   if(!gestor){assert.equal(f.pessoas.length,1);assert.equal(f.pessoas[0].id,conta.employeeId);}
   else assert.ok(f.pessoas.every(p=>p.companyId===sessao.usuario.companyId&&p.departmentId===sessao.usuario.departmentId));
   const fora=conta.employeeId===contas.colaborador.employeeId?contas.desenvolvedor.employeeId:contas.colaborador.employeeId;
   for(const base of [origin,spring]){
    await get('/api/dashboard/ams',{employeeId:fora,perfil:'administrador',usuarioId:'ROOT-E0001'},403,sessao.cookie,base);
    await get('/api/funcionario/buscar/'+fora,{},403,sessao.cookie,base);
   }
   for(const route of ['ams','ai-literacy','governance','security-incidents','finops','delivery'])
    await get('/score/'+route+'/'+fora,{periodo:5},403,sessao.cookie,spring);
   await get('/api/dashboard/resumo',{escopo:'empresa',perfil:'administrador',usuarioId:'ROOT-E0001'},403,sessao.cookie);
   const pessoal=await get('/api/dashboard/ams',{employeeId:conta.employeeId,periodo:5},200,sessao.cookie);
   assert.equal(pessoal.integrantes.content.length,1);
   if(!gestor){assert.equal(pessoal.pessoal.dimensoes.length,tipo==='desenvolvedor'?5:4);assert.ok(pessoal.pessoal.dimensoes.every(d=>d.registrada!=null));}
   const lista=await get('/api/funcionario/listar',{},200,sessao.cookie);
   assert.ok(lista.every(p=>f.pessoas.some(x=>x.id===p.employeeId)));
   const uso=await get('/api/utilizacao-ia/listar',{size:100},200,sessao.cookie);
   assert.ok(uso.content.every(l=>f.pessoas.some(p=>p.id===l.employeeId)));
   const consumo=await get('/api/funcionario/me/consumo-ia',{periodo:5,employeeId:fora},200,sessao.cookie);
   assert.equal(consumo.employeeId,conta.employeeId);assert.equal(consumo.total,consumo.ferramentas.reduce((s,t)=>s+t.total,0));
   assert.ok(consumo.ferramentas.every(t=>t.total===t.entrada+t.saida));
   const tecnico=await get('/api/dashboard/resumo',{escopo:'pessoal',employeeId:conta.employeeId,periodo:5},200,sessao.cookie);
   assert.equal(consumo.total,tecnico.metricas.tokens);
   await get('/api/funcionario/me/consumo-ia',{periodo:0},400,sessao.cookie);
   const html=await fetch(origin+'/perfil',{headers:{cookie:sessao.cookie}});assert.equal(html.status,200);assert.ok((await html.text()).includes('perfilSair'));
   const dashboard=await fetch(origin+'/dashboard',{headers:{cookie:sessao.cookie}});assert.equal(dashboard.status,200);assert.ok(!(await dashboard.text()).includes('rdActorCompany'));
   const cadastro=await fetch(origin+'/funcionario/criar',{headers:{cookie:sessao.cookie}});assert.equal(cadastro.status,gestor?200:403);
  }finally{await logout(sessao.cookie);}
  await get('/api/funcionario/sessao',{},401,sessao.cookie);
 }
 const conta=contas.desenvolvedor;
 const antiga=await login(conta.email,conta.senha),nova=await login(conta.email,conta.senha);
 try{
  assert.notEqual(antiga.cookie,nova.cookie);
  await get('/api/funcionario/sessao',{},401,antiga.cookie);
  await logout(antiga.cookie);await get('/api/funcionario/sessao',{},200,nova.cookie);
  for(const base of [origin,spring]){
   const r=await fetch(base+'/api/funcionario/logout',{method:'POST',headers:{cookie:nova.cookie,origin:'https://origem-invalida.example'}});assert.equal(r.status,403);
   const l=await fetch(base+'/api/funcionario/login',{method:'POST',headers:{origin:'https://origem-invalida.example','content-type':'application/json'},body:JSON.stringify(conta)});assert.equal(l.status,403);
  }
  await get('/api/funcionario/sessao',{},200,nova.cookie);
 }finally{await logout(nova.cookie);}
 await get('/api/funcionario/sessao',{},401,nova.cookie);
 await get('/api/funcionario/sessao',{},401,'usuario='+'a'.repeat(128)+'ROOT-E0001');
 console.log('Autenticação: cookies, 4 perfis de demonstração, escopos Java/Node, tokens por IA, troca de sessão, logout e origem verificados.');
})().catch(e=>{console.error(e);process.exitCode=1;});
