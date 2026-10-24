const assert=require('node:assert/strict');
const {origin,login,get,logout}=require('./sessao-helper.cjs');
const contas=require('./contas-demo.json');
(async()=>{
 const conta=contas.naoDesenvolvedor||contas.colaborador||{email:'valentina.souza@empresa.com',senha:'123456'};
 const s=await login(conta.email,conta.senha);
 try{
  const estado=()=>get('/api/jardim/estado',{},200,s.cookie);
  async function post(rota,corpo,status=200){const r=await fetch(origin+'/api/jardim/'+rota,{method:'POST',headers:{cookie:s.cookie,'content-type':'application/json'},body:JSON.stringify(corpo)});const b=await r.json();assert.equal(r.status,status,JSON.stringify(b));return b.estado;}
  let e=await estado();
  while(e.carteira.moedas<140&& !e.encerrado){
   if(e.dia===e.periodo.dias){e=await post('encerrar',{periodo:e.periodo.id,versao:e.carteira.versao});}
   else e=await post('avancar',{periodo:e.periodo.id,dias:7,versao:e.carteira.versao});
  }
  assert.ok(e.carteira.moedas>=140,'Conta de demonstração precisa de moedas históricas');
  const periodo=e.periodo.id;
  // O teste de reset completo está em jardim-reset-integration.cjs.
  if(e.arvore.adubosAplicados>10)throw new Error('Use uma jornada com capacidade de adubo para o teste da loja.');
  const ams=e.arvore.ams,dia=e.dia,saldo=e.carteira.moedas,estoque=e.carteira.adubos;
  e=await post('comprar',{item:'adubo',quantidade:6,versao:e.carteira.versao});
  assert.equal(e.carteira.moedas,saldo-60);
  assert.equal(e.carteira.adubos,estoque+6);
  e=await post('adubar',{periodo,quantidade:3,versao:e.carteira.versao});
  assert.ok(e.arvore.estagio>=2);assert.equal(e.arvore.ams,ams);assert.equal(e.dia,dia);
  for(const item of ['flores','pedras']){
   if(!e.loja.find(x=>x.id===item).adquirido)e=await post('comprar',{item,quantidade:1,versao:e.carteira.versao});
  }
  e=await post('equipar',{item:'nenhuma',versao:e.carteira.versao});
  for(const item of ['flores','pedras'])e=await post('equipar',{item,versao:e.carteira.versao});
  assert.deepEqual(e.carteira.decoracao.split(',').sort(),['flores','pedras']);
  await post('adubar',{periodo,quantidade:17,versao:e.carteira.versao},400);
  console.log(JSON.stringify({passed:true,periodo:e.periodo.id,dia:e.dia,ams:e.arvore.ams,estagio:e.arvore.estagio,decoracoes:e.carteira.decoracao,moedas:e.carteira.moedas},null,2));
 }finally{await logout(s.cookie);}
})().catch(e=>{console.error(e);process.exitCode=1;});
