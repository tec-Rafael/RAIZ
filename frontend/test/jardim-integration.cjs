const assert=require('node:assert/strict');
const {origin,spring,login,get,logout}=require('./sessao-helper.cjs');
const contas=require('./contas-demo.json');
// Exercita apenas o jogo da conta de demonstração; não altera métricas AMS.
const conta={email:process.env.JARDIM_EMAIL||contas.desenvolvedor.email,senha:process.env.JARDIM_SENHA||contas.desenvolvedor.senha};
(async()=>{
 await get('/api/jardim/estado',{},401);
 const sessao=await login(conta.email,conta.senha);
 try{
  async function estado(params={}){return get('/api/jardim/estado',params,200,sessao.cookie);}
  async function post(rota,body,status=200){
   const r=await fetch(origin+'/api/jardim/'+rota,{method:'POST',headers:{cookie:sessao.cookie,'content-type':'application/json'},body:JSON.stringify(body)});
   const b=await r.json();assert.equal(r.status,status,JSON.stringify({rota,b}));return b;
  }
  let e=await estado();assert.equal(e.employeeId,sessao.usuario.employeeId);
  const antesPreview=e.carteira;
  const previa=await estado({periodo:5,dia:66,employeeId:'ROOT-E0001'});
  const score=await get('/score/ams/'+sessao.usuario.employeeId,{periodo:5},200,sessao.cookie,spring);
  assert.ok(Math.abs(previa.arvore.ams-score.calculo.nota)<1e-8,'AMS completo do jardim deve corresponder ao dashboard');
  assert.deepEqual((await estado()).carteira,antesPreview,'Explorar datas não concede moedas');
  await get('/api/jardim/estado',{periodo:5,dia:67},400,sessao.cookie);
  await post('comprar',{item:'adubo',quantidade:0,versao:e.carteira.versao},400);
  await post('comprar',{item:'item-forjado',quantidade:1,versao:e.carteira.versao},400);
  await post('comprar',{item:'adubo',quantidade:20,versao:e.carteira.versao},409);
  if(!e.encerrado&&e.dia<e.periodo.dias){
   const corpo={periodo:e.periodo.id,dias:1,versao:e.carteira.versao};
   const resultados=await Promise.all([fetch(origin+'/api/jardim/avancar',{method:'POST',headers:{cookie:sessao.cookie,'content-type':'application/json'},body:JSON.stringify(corpo)}),fetch(origin+'/api/jardim/avancar',{method:'POST',headers:{cookie:sessao.cookie,'content-type':'application/json'},body:JSON.stringify(corpo)})]);
   assert.deepEqual(resultados.map(r=>r.status).sort(),[200,409],'Apenas uma ação simultânea deve ser aceita');
   const ok=await resultados.find(r=>r.status===200).json();
   assert.equal(ok.estado.dia,e.dia+1);assert.equal(ok.estado.carteira.moedas,e.carteira.moedas+ok.moedasRecebidas);e=ok.estado;
   const saldo=e.carteira.moedas;
   await post('avancar',corpo,409);assert.equal((await estado()).carteira.moedas,saldo,'Reenviar uma ação antiga não duplica moedas');
   for(let i=0;i<3&&e.carteira.moedas<30&&e.dia<e.periodo.dias;i++)e=(await post('avancar',{periodo:e.periodo.id,dias:7,versao:e.carteira.versao})).estado;
  }
  if(!e.encerrado&&e.dia>0&&e.carteira.moedas>=10&&e.arvore.adubosAplicados<16){
   const quantidade=Math.min(3,16-e.arvore.adubosAplicados,Math.floor(e.carteira.moedas/10));
   const saldo=e.carteira.moedas,nota=e.arvore.ams,usados=e.arvore.adubosAplicados,estoque=e.carteira.adubos;
   e=(await post('comprar',{item:'adubo',quantidade,versao:e.carteira.versao,preco:0,employeeId:'ROOT-E0001'})).estado;
   assert.equal(e.carteira.moedas,saldo-quantidade*10);assert.equal(e.carteira.adubos,estoque+quantidade);assert.equal(e.arvore.ams,nota);
   for(let i=0;i<quantidade;i++)e=(await post('adubar',{periodo:e.periodo.id,versao:e.carteira.versao})).estado;
   assert.equal(e.arvore.adubosAplicados,usados+quantidade);assert.equal(e.arvore.ams,nota,'Adubo não modifica o AMS');
  }
  const antes=e.carteira;await estado({periodo:e.periodo.id,dia:Math.max(0,e.dia-1)});assert.deepEqual((await estado()).carteira,antes);
  const company=sessao.usuario.companyId==='C01'?'C02':'C01';
  await get('/api/jardim/ranking',{periodo:e.periodo.id,dia:e.dia,companyId:company},403,sessao.cookie);
  await get('/api/jardim/ranking',{periodo:e.periodo.id,dia:e.dia,escopo:'departamento',departmentId:'OUTRA-AREA'},403,sessao.cookie);
  const r=await get('/api/jardim/ranking',{periodo:e.periodo.id,dia:e.dia,escopo:'empresa'},200,sessao.cookie);
  assert.ok(r.destaques.every(p=>!('email' in p)&&!('employeeId' in p)&&!('dimensoes' in p)));
  assert.ok(r.destaques.every(p=>p.ams!=null));
  const estranho=await fetch(origin+'/api/jardim/comprar',{method:'POST',headers:{cookie:sessao.cookie,origin:'https://origem-invalida.example','content-type':'application/json'},body:JSON.stringify({item:'adubo',quantidade:1,versao:e.carteira.versao})});assert.equal(estranho.status,403);
  if(process.env.JARDIM_CICLO_COMPLETO==='1'&&!e.encerrado){
   while(e.dia<e.periodo.dias)e=(await post('avancar',{periodo:e.periodo.id,dias:7,versao:e.carteira.versao})).estado;
   const completo=e;
   const nota=await get('/score/ams/'+sessao.usuario.employeeId,{periodo:e.periodo.id},200,sessao.cookie,spring);
   assert.ok(Math.abs(e.arvore.ams-nota.calculo.nota)<1e-8);
   e=(await post('encerrar',{periodo:e.periodo.id,versao:e.carteira.versao})).estado;
   assert.ok(e.jardim.find(p=>p.periodo===completo.periodo.id).encerrado);
   assert.equal(e.carteira.moedas,completo.carteira.moedas);
   if(completo.periodo.id<5){assert.equal(e.periodo.id,completo.periodo.id+1);assert.equal(e.dia,0);assert.equal(e.arvore.estagio,1);}
   await post('avancar',{periodo:completo.periodo.id,dias:1,versao:e.carteira.versao},409);
   const guardada=await estado({periodo:completo.periodo.id,dia:completo.dia});
   assert.equal(guardada.arvore.ams,completo.arvore.ams);assert.equal(guardada.arvore.estagio,completo.arvore.estagio);
   assert.deepEqual((await estado()).carteira,e.carteira);
  }
  console.log(JSON.stringify({passed:true,periodo:e.periodo.id,dia:e.dia,ams:e.arvore.ams,nivel:e.arvore.nivel,estagio:e.arvore.estagio,moedas:e.carteira.moedas,ranking:r.destaques.length},null,2));
 }finally{await logout(sessao.cookie);}
})().catch(e=>{console.error(e);process.exitCode=1;});
