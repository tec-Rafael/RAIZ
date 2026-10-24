const assert=require('node:assert/strict');
const {origin,login,get,logout}=require('./sessao-helper.cjs');
const conta=require('./contas-demo.json').colaborador;
(async()=>{const s=await login(conta.email,conta.senha);try{
 const estado=(q={})=>get('/api/jardim/estado',q,200,s.cookie);
 async function post(rota,body){const r=await fetch(origin+'/api/jardim/'+rota,{method:'POST',headers:{cookie:s.cookie,'content-type':'application/json'},body:JSON.stringify(body)});const b=await r.json();assert.equal(r.status,200,JSON.stringify(b));return b;}
 let e=await estado();const periodo=e.periodo.id,dia=e.dia;
 if(e.arvore.adubosAplicados>14)throw new Error('Conta sem capacidade para o teste de adubo.');
 const comprar=Math.max(0,2-e.carteira.adubos);
 if(comprar)e=(await post('comprar',{item:'adubo',quantidade:comprar,versao:e.carteira.versao})).estado;
 const ams=e.arvore.ams,nivel=e.arvore.nivel,moedas=e.carteira.moedas,estoque=e.carteira.adubos;
 e=(await post('adubar',{periodo,dia,quantidade:1,versao:e.carteira.versao})).estado;
 assert.equal(e.dia,dia);assert.equal(e.arvore.ams,ams);assert.equal(e.arvore.nivel,nivel);assert.equal(e.carteira.moedas,moedas);assert.equal(e.carteira.adubos,estoque-1);
 assert.equal((await estado()).dia,dia);
 const d=dia===0?3:dia-1,previa=await estado({periodo,dia:d});
 e=(await post('adubar',{periodo,dia:d,quantidade:1,versao:e.carteira.versao})).estado;
 assert.equal(e.dia,d);assert.equal(e.arvore.ams,previa.arvore.ams);assert.equal(e.data,previa.data);assert.equal((await estado()).dia,dia);
 if(dia===0){const zero=await estado();assert.equal(zero.arvore.ams,null);assert.equal(zero.arvore.nivel,null);}
 const r=await get('/api/jardim/ranking',{periodo,dia:0,escopo:'departamento'},200,s.cookie);
 assert.ok(r.floresta.arvores.length>0);assert.ok(r.floresta.arvores.some(x=>x.estagio>1));assert.equal(r.floresta.ams,null);assert.equal(r.destaques.length,0);
 console.log(JSON.stringify({passed:true,diaSalvo:dia,diaDaPrevia:d,arvoresNaFloresta:r.floresta.arvores.length,arvoresDesenvolvidas:r.floresta.arvores.filter(x=>x.estagio>1).length},null,2));
}finally{await logout(s.cookie);}})().catch(e=>{console.error(e);process.exitCode=1;});
