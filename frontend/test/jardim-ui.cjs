const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
const script=fs.readFileSync(require('node:path').join(__dirname,'../public/js/jardim.js'),'utf8');
async function verificar({dia=0,previa=false,adubos=11,falhar=false,acao='adubar'}={}){
 const elementos=new Map(),chamadas=[];let loja=false;
 function elemento(id){if(!elementos.has(id))elementos.set(id,{value:'',hidden:false,disabled:false,dataset:{},classList:{toggle(){},remove(){},contains(){return false;}},firstElementChild:{style:{}},setAttribute(){},replaceChildren(){},querySelectorAll(){return [];},querySelector(){return {open:false};},addEventListener(tipo,fn){this[tipo]=fn;}});return elementos.get(id);}
 let estado={periodo:{id:2,dias:90},periodos:[{id:2,dias:90}],dia,previa,encerrado:false,podeAvancar:!previa,podeAdubar:!previa&&dia>0&&adubos>0,carteira:{moedas:112,adubos,versao:7,decoracao:'nenhuma'},arvore:{estagio:1,adubosAplicados:0,baseAdubo:0,metaAdubo:3,ams:null,nivel:null,registradas:0,aplicaveis:5},loja:[],jardim:[],conquistas:[]};
 const motor={pronto:Promise.resolve(),irPara:async()=>{},decorar(){},adubar(){}};
 const contexto={document:{getElementById:elemento,addEventListener(){}},window:{raizArvoreHome:motor,matchMedia:()=>({matches:true}),bootstrap:{Offcanvas:{getOrCreateInstance:()=>({show(){loja=true;}})}}},Option:function(){},Intl,URLSearchParams,AbortController,setTimeout:()=>0,clearTimeout(){},fetch:async(url,opcoes)=>{
  if(!opcoes.body)return {ok:true,status:200,json:async()=>structuredClone(estado)};
  const rota=url.split('/').pop(),body=JSON.parse(opcoes.body);chamadas.push({rota,body});
  if(falhar&&rota==='adubar')return {ok:false,status:409,json:async()=>({detail:'Conflito'})};
  assert.equal(body.versao,estado.carteira.versao);
  if(rota==='zerar'){estado.dia=0;estado.previa=false;estado.arvore.adubosAplicados=0;}
  if(rota==='avancar'){assert.equal(body.dias,1);estado.dia=1;}
  if(rota==='adubar'){assert.equal(body.dia,dia);assert.equal(estado.dia,dia);estado.carteira.adubos--;estado.arvore.adubosAplicados++;}
  estado.carteira.versao++;estado.podeAdubar=estado.carteira.adubos>0;
  return {ok:true,status:200,json:async()=>({estado:structuredClone(estado),passos:[],mensagem:'OK'})};
 }};
 vm.runInNewContext(script,contexto);
 for(let i=0;i<5;i++)await new Promise(setImmediate);
 assert.equal(elemento('gardenFertilize').disabled,adubos===0);
 if(acao==='zerar')await elemento('gardenReset').click();else if(adubos)await elemento('gardenFertilize').click();
 return {chamadas,loja,estado,elementos};
}
(async()=>{
 const reset=await verificar({dia:35,previa:true,acao:'zerar'});assert.equal(reset.estado.dia,0);assert.equal(reset.estado.carteira.moedas,112);assert.deepEqual(reset.chamadas.map(c=>c.rota),['zerar']);
 const inicio=await verificar();assert.deepEqual(inicio.chamadas.map(c=>c.rota),['adubar']);assert.equal(inicio.estado.carteira.adubos,10);assert.equal(inicio.estado.arvore.adubosAplicados,1);
 const conflito=await verificar({falhar:true});assert.deepEqual(conflito.chamadas.map(c=>c.rota),['adubar']);assert.equal(conflito.estado.carteira.adubos,11);
 const previa=await verificar({previa:true,dia:12});assert.equal(previa.chamadas.length,1);assert.equal(previa.estado.dia,12);assert.equal(previa.elementos.get('gardenDay').value,12);
 const vazio=await verificar({adubos:0,dia:1});assert.equal(vazio.elementos.get('gardenFertilize').disabled,true);assert.equal(vazio.chamadas.length,0);
 console.log('Loja: aplicação no dia zero e na prévia, conflito e estoque vazio verificados.');
})().catch(e=>{console.error(e);process.exitCode=1;});
