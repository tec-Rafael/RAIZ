const assert=require('node:assert/strict');
const {origin,login,get,logout}=require('./sessao-helper.cjs');
const conta=require('./contas-demo.json').colaborador;
(async()=>{
 const s=await login(conta.email,conta.senha);
 try{
  const estado=()=>get('/api/jardim/estado',{},200,s.cookie);
  async function post(rota,body,status=200){const r=await fetch(origin+'/api/jardim/'+rota,{method:'POST',headers:{cookie:s.cookie,'content-type':'application/json'},body:JSON.stringify(body)});const b=await r.json();assert.equal(r.status,status,JSON.stringify(b));return b;}
  let e=await estado();const antes=e.carteira;
  const completo=await get('/api/jardim/estado',{periodo:5,dia:66},200,s.cookie);
  const request={periodo:5,versao:antes.versao};
  const reset=await post('zerar',request);e=reset.estado;
  assert.equal(e.periodo.id,5);assert.equal(e.periodoAtual,5);assert.equal(e.dia,0);assert.equal(e.previa,false);assert.equal(e.arvore.estagio,1);assert.equal(e.carteira.moedas,antes.moedas);assert.equal(e.encerrado,false);
  assert.equal(e.carteira.adubos,antes.adubos);assert.equal(e.carteira.decoracao,antes.decoracao);assert.equal(e.arvore.ams,null);assert.equal(e.arvore.nivel,null);
  await post('zerar',request,409);
  let ganho=0;
  while(ganho===0&&e.dia<66){const passo=await post('avancar',{periodo:5,dias:7,versao:e.carteira.versao});ganho+=passo.moedasRecebidas;e=passo.estado;}
  assert.ok(ganho>0,'É preciso verificar pelo menos um dia com recompensa real');
  assert.equal(e.carteira.moedas,antes.moedas+ganho);
  const fim=e.dia;
  e=(await post('zerar',{periodo:5,versao:e.carteira.versao})).estado;
  assert.equal(e.carteira.moedas,antes.moedas+ganho);assert.equal(e.dia,0);
  let repetido=0;
  while(e.dia<fim){const passo=await post('avancar',{periodo:5,dias:Math.min(7,fim-e.dia),versao:e.carteira.versao});repetido+=passo.moedasRecebidas;e=passo.estado;}
  assert.equal(repetido,ganho,'Reset permite receber os mesmos dias novamente');
  assert.equal(e.carteira.moedas,antes.moedas+ganho+repetido);assert.equal(e.dia,fim);
  const saldo=e.carteira.moedas;
  await get('/api/jardim/estado',{periodo:5,dia:66},200,s.cookie);assert.equal((await estado()).carteira.moedas,saldo,'Prévia não concede moedas');
  console.log(JSON.stringify({passed:true,saldo,moedasRepetidas:repetido,dia:e.dia,ams:e.arvore.ams},null,2));
 }finally{await logout(s.cookie);}
})().catch(e=>{console.error(e);process.exitCode=1;});
