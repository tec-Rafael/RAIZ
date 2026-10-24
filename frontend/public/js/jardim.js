(() => {
 'use strict';
 const root=document.getElementById('gardenRoot');if(!root)return;
 const $=id=>document.getElementById('garden'+id);
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const fmt=(n,d=0)=>n==null?'—':new Intl.NumberFormat('pt-BR',{maximumFractionDigits:d,minimumFractionDigits:d}).format(n);
 const nomes={baixo:'Precisa de cuidado · seca',medio:'Criando vigor · outono',alto:'Saudável · primavera',excelente:'Exuberante · verão'};
 let estado=null,busy=false,playing=false,toastTimer,previewController,rankController,previewEpoch=0,rankEpoch=0,assetsFalharam=false;
 const esperar=ms=>new Promise(resolve=>setTimeout(resolve,ms));
 const motor=window.raizArvoreHome;
 // A imagem nunca é a fonte do saldo, do inventário ou da saúde.
 const pronto=motor?motor.pronto.catch(erro=>{assetsFalharam=true;throw erro;}):Promise.reject(new Error('Motor da árvore indisponível.'));
 pronto.catch(()=>{});
 function imagem(nivel,estagio){const n=['baixo','medio','alto','excelente'].includes(nivel)?nivel:'medio';const e=Math.max(1,Math.min(4,Number(estagio)||1));return '/public/img/FramesArvore/'+n+'/arvore_estagio_'+e+'_vetorial.svg';}
 function data(s){return s?s.split('-').reverse().join('/'):'Dia inicial';}
 async function api(rota,{method='GET',body,signal}={}){
  const r=await fetch('/api/jardim/'+rota,{method,signal,headers:body?{'content-type':'application/json'}:undefined,body:body?JSON.stringify(body):undefined});
  if(r.status===401){window.location.assign('/login');throw new Error('Sua sessão expirou.');}
  const b=await r.json().catch(()=>null);
  if(!r.ok){const erro=new Error(b?.detail||b?.message||'Não foi possível consultar o jardim.');erro.status=r.status;throw erro;}
  return b;
 }
 function toast(texto){clearTimeout(toastTimer);$('Toast').textContent=texto;$('Toast').hidden=false;toastTimer=setTimeout(()=>$('Toast').hidden=true,6500);}
 function controles(){
  if(!estado)return;
  const estoque=estado.carteira.adubos,completa=estado.arvore.estagio===4;
  $('Fertilize').disabled=busy||playing||!estoque||completa;
  $('ApplyQuantity').disabled=busy||playing||!estoque||completa;
  $('ApplyQuantity').max=Math.max(1,Math.min(16-estado.arvore.adubosAplicados,estoque));
  $('ApplyQuantity').value=Math.min(Number($('ApplyQuantity').value)||1,Number($('ApplyQuantity').max));
  $('FertilizeLabel').textContent=completa?'Árvore adulta':'Adubar';
  $('FertilizeHint').textContent=completa?'Sua árvore está adulta. Zere o período para começar de novo.':estoque?'Você tem '+estoque+' adubos no estoque. Escolha quantos aplicar.':'Compre adubo abaixo para cuidar da sua árvore.';
  $('Reset').disabled=busy||playing;
  $('DayNext').disabled=busy||playing||!estado.podeAvancar;
  $('WeekNext').disabled=busy||playing||!estado.podeAvancar;
  $('Period').disabled=busy||playing;$('Day').disabled=busy||playing;
  $('Play').disabled=!playing&&(busy||!estado.podeAvancar);
  $('Play').innerHTML=playing?'<i class="bi bi-pause-fill" aria-hidden="true"></i> Pausar':'<i class="bi bi-play-fill" aria-hidden="true"></i> Reproduzir';
  $('Return').hidden=!estado.previa;$('Return').disabled=busy||playing;
  $('Finish').hidden=estado.previa||estado.encerrado||estado.dia!==estado.periodo.dias;$('Finish').disabled=busy||playing;
  $('Finish').textContent=estado.periodo.id===5?'Guardar última árvore':'Guardar árvore e seguir';
  root.setAttribute('aria-busy',String(busy));
  for(const b of root.querySelectorAll('[data-buy],[data-equip],[data-period]'))b.disabled=busy||playing||b.dataset.indisponivel==='true';
 }
 function saude(ams,nivel){
  $('Health').textContent=nivel?nomes[nivel]:'AMS em formação';$('Ams').textContent=fmt(ams,2);
 }
 async function arvore(nivel,estagio,animar=true){await pronto;return motor.irPara(nivel||'medio',estagio,{animar});}
 function opcoes(el,itens,selecionado){el.replaceChildren(...itens.map(x=>new Option(x.nome,x.id)));if(itens.some(x=>String(x.id)===String(selecionado)))el.value=selecionado;}
 function render(e){
  estado=e;
  opcoes($('Period'),e.periodos.map(p=>({id:p.id,nome:'P'+p.id+' · '+p.dias+' dias'})),e.periodo.id);
  $('Day').max=e.periodo.dias;$('Day').value=e.dia;$('DayLabel').textContent='Dia '+e.dia+' de '+e.periodo.dias;
  $('Date').textContent='P'+e.periodo.id+' · '+data(e.data);
  $('Mode').textContent='Histórico simulado · '+(e.previa?'prévia sem recompensas':e.encerrado?'árvore guardada':'sua jornada');
  $('Coins').textContent=fmt(e.carteira.moedas);$('ShopCoins').textContent=fmt(e.carteira.moedas);$('FertilizerCount').textContent=e.carteira.adubos;
  const a=e.arvore;
  $('Partial').textContent=e.dia===e.periodo.dias?'do período':'parcial';
  saude(a.ams,a.nivel,a.registradas,a.aplicaveis);
  $('Status').textContent=e.previa?'Prévia histórica: rever datas não libera moedas nem altera o crescimento.':e.encerrado?'Árvore guardada. Você pode decorar, adubar ou zerar seu crescimento.':e.dia===0?'Comece avançando um dia ou uma semana do histórico.':e.dia===e.periodo.dias?'Período completo. Você ainda pode adubar antes de guardar a árvore.':'Dia '+e.dia+' salvo. Cada data rende moedas uma vez até você zerar o período.';
  renderLoja();renderColecao();controles();
  if(motor)motor.decorar(e.carteira.decoracao);
 }
 function renderLoja(){
  if(!estado)return;
  $('ShopItems').innerHTML=estado.loja.map(item=>{
   const equipado=estado.carteira.decoracao.split(',').includes(item.id);
   const indisponivel=item.adquirido?false:estado.carteira.moedas<item.preco;
   return '<article class="gardenShopItem"><div class="gardenShopItemHead">'+(item.id==='adubo'?'<i class="bi bi-droplet" aria-hidden="true"></i>':'<img class="gardenItemPreview" src="/public/img/decoracoes/'+item.id+'.png" alt="">')+'<div><h3>'+esc(item.nome)+'</h3><p>'+esc(item.descricao)+'</p></div></div><footer><span>'+item.preco+' moedas'+(item.adquirido?' · adquirido':'')+'</span><div>'+(item.id==='adubo'?'<label class="visually-hidden" for="gardenShopQuantity">Quantidade de adubo</label><input id="gardenShopQuantity" type="number" min="1" max="20" value="1">':'')+'<button type="button" class="gardenButton '+(!item.adquirido?'gardenPrimary':'')+'" '+(item.adquirido?'data-equip':'data-buy')+'="'+item.id+'" data-indisponivel="'+indisponivel+'" '+(busy||playing||indisponivel?'disabled':'')+'>'+(item.adquirido?(equipado?'Retirar':'Decorar'):'Comprar')+'</button></div></footer></article>';
  }).join('')+(estado.carteira.decoracao!=='nenhuma'?'<button class="gardenTextButton" type="button" data-equip="nenhuma">Retirar decoração</button>':'');
 }
 function renderColecao(){
  $('Trees').innerHTML=estado.jardim.map(p=>'<button type="button" class="gardenTreeTile '+(!p.nivel?'gardenNeutral':'')+'" data-period="'+p.periodo+'"><strong>Período '+p.periodo+'</strong><img src="'+imagem(p.nivel,p.estagio)+'" alt="Árvore do período '+p.periodo+'"><span>'+(p.encerrado?'Guardada · AMS '+fmt(p.ams,2):p.dia?'Em crescimento':'Ainda não iniciado')+'</span><small>'+p.dia+'/'+p.dias+' dias · estágio '+p.estagio+'</small></button>').join('');
  const obtidas=estado.conquistas.filter(c=>c.conquistada).length;$('BadgeCount').textContent=obtidas;
  $('Achievements').innerHTML=estado.conquistas.map(c=>'<article class="gardenAchievement '+(c.conquistada?'earned':'')+'"><i class="bi '+(c.conquistada?'bi-award':'bi-lock')+'" aria-hidden="true"></i><div><h4>'+esc(c.nome)+(c.conquistada?' · conquistada':'')+'</h4><p>'+esc(c.descricao)+'</p></div></article>').join('');
 }
 async function carregar(periodo,dia){
  if(busy||playing)return;
  const ep=++previewEpoch;previewController?.abort();previewController=new AbortController();busy=true;controles();$('Retry').hidden=true;
  try{
   const params=new URLSearchParams();if(periodo!=null)params.set('periodo',periodo);if(dia!=null)params.set('dia',dia);
   const e=await api('estado?'+params,{signal:previewController.signal});if(ep!==previewEpoch)return;
   const primeiraCarga=estado===null;render(e);await arvore(e.arvore.nivel,e.arvore.estagio,!primeiraCarga);
   if($('Ranking').classList.contains('show'))carregarRanking();
  }catch(erro){if(erro.name!=='AbortError'){$('Status').textContent=erro.message;$('Retry').hidden=false;}}
  finally{if(ep===previewEpoch){busy=false;controles();}}
 }
 async function mutar(rota,campos={}){
  if(busy||!estado)return false;
  busy=true;controles();const antes=estado;
  try{
   const resposta=await api(rota,{method:'POST',body:{...campos,versao:antes.carteira.versao}});
   if(resposta.passos.length){
    let saldo=antes.carteira.moedas;
    for(const p of resposta.passos){
     saldo+=p.moedas;$('Coins').textContent=fmt(saldo);$('Date').textContent='P'+antes.periodo.id+' · '+data(p.data);$('Day').value=p.dia;$('DayLabel').textContent='Dia '+p.dia+' de '+antes.periodo.dias;
     saude(p.ams,p.nivel,p.registradas,p.aplicaveis);$('Status').textContent='Dia '+p.dia+': '+(p.moedas?'+'+p.moedas+' moedas':'AMS em formação · sem recompensa');
     await arvore(p.nivel,antes.arvore.estagio);if(!window.matchMedia('(prefers-reduced-motion: reduce)').matches)await esperar(180);
    }
   }
   let e=resposta.estado;
   if(rota!=='zerar'&&rota!=='adubar'&&antes.previa)e=await api('estado?'+new URLSearchParams({periodo:antes.periodo.id,dia:antes.dia}));
   const antigas=new Set(antes.conquistas.filter(c=>c.conquistada).map(c=>c.codigo));
   render(e);if(rota==='adubar')motor.adubar();await arvore(e.arvore.nivel,e.arvore.estagio);
   const novas=e.conquistas.filter(c=>c.conquistada&&!antigas.has(c.codigo));
   toast(resposta.mensagem+(novas.length?' Conquista: '+novas.map(c=>c.nome).join(', ')+'.':''));
   if($('Ranking').classList.contains('show'))carregarRanking();return true;
  }catch(erro){
   playing=false;toast(erro.message);$('Status').textContent=erro.message;
   try{const salvo=await api(rota==='zerar'?'estado':'estado?'+new URLSearchParams({periodo:antes.periodo.id,dia:antes.dia}));render(salvo);await arvore(salvo.arvore.nivel,salvo.arvore.estagio);}catch(_){$('Retry').hidden=false;}
   return false;
  }finally{busy=false;controles();}
 }
 $('DayNext').addEventListener('click',()=>mutar('avancar',{periodo:estado.periodo.id,dias:1}));
 $('WeekNext').addEventListener('click',()=>mutar('avancar',{periodo:estado.periodo.id,dias:7}));
 $('Fertilize').addEventListener('click',async()=>{
  if(busy||playing||!estado)return;
  const quantidade=Number($('ApplyQuantity').value);
  if(!Number.isInteger(quantidade)||quantidade<1||quantidade>16){toast('Escolha uma quantidade de adubo válida.');return;}
  await mutar('adubar',{periodo:estado.periodo.id,dia:estado.dia,quantidade});
 });
 $('Reset').addEventListener('click',()=>mutar('zerar',{periodo:estado.periodo.id}));
 $('Finish').addEventListener('click',()=>mutar('encerrar',{periodo:estado.periodo.id}));
 $('Play').addEventListener('click',async()=>{
  if(playing){playing=false;controles();return;}if(busy||!estado?.podeAvancar)return;
  playing=true;controles();
  while(playing&&estado.podeAvancar){if(!await mutar('avancar',{periodo:estado.periodo.id,dias:1}))break;if(playing)await esperar(250);}
  playing=false;controles();
 });
 document.addEventListener('visibilitychange',()=>{if(document.hidden){playing=false;controles();}});
 $('Period').addEventListener('change',()=>carregar($('Period').value));
 $('Day').addEventListener('input',()=>$('DayLabel').textContent='Dia '+$('Day').value+' de '+estado.periodo.dias);
 $('Day').addEventListener('change',()=>carregar(estado.periodo.id,$('Day').value));
 $('Return').addEventListener('click',()=>carregar());
 $('Retry').addEventListener('click',()=>assetsFalharam?window.location.reload():carregar());
 $('ShopItems').addEventListener('click',evento=>{
  const comprar=evento.target.closest('[data-buy]'),equipar=evento.target.closest('[data-equip]');
  if(comprar){const item=comprar.dataset.buy;const quantidade=item==='adubo'?Number($('ShopQuantity').value):1;if(!Number.isInteger(quantidade)||quantidade<1||quantidade>20){toast('Escolha entre 1 e 20 adubos.');return;}mutar('comprar',{item,quantidade});}
  if(equipar)mutar('equipar',{item:equipar.dataset.equip});
 });
 $('Trees').addEventListener('click',evento=>{const b=evento.target.closest('[data-period]');if(!b||busy||playing)return;window.bootstrap?.Offcanvas.getInstance($('Collection'))?.hide();carregar(Number(b.dataset.period));});
 function linhaRank(p){return '<li class="gardenRankRow '+(p.eu?'me':'')+'"><span>'+p.posicao+'</span><img src="'+imagem(p.nivel,p.estagio)+'" alt=""><div><strong>'+esc(p.nome)+(p.eu?' (você)':'')+'</strong><small>AMS '+fmt(p.ams,2)+' · estágio '+p.estagio+'</small></div><b>'+($('RankOrder').value==='evolucao'?(p.evolucao>0?'+':'')+fmt(p.evolucao,2):fmt(p.ams,2))+'</b></li>';}
 async function carregarRanking(){
  if(!estado)return;
  const ep=++rankEpoch;rankController?.abort();rankController=new AbortController();$('RankRetry').hidden=true;
  $('RankStatus').textContent='Consultando o ranking do dia histórico…';$('RankList').replaceChildren();$('MyRank').replaceChildren();$('Forest').replaceChildren();$('ForestSummary').replaceChildren();
  const query={periodo:estado.periodo.id,dia:estado.dia,escopo:$('RankScope').value,ordem:$('RankOrder').value,grupo:$('RankGroup').value,companyId:$('RankCompany')?.value||estado.companyId,departmentId:$('RankArea')?.value||estado.departmentId||''};
  try{
   const r=await api('ranking?'+new URLSearchParams(query),{signal:rankController.signal});if(ep!==rankEpoch)return;
   if($('RankCompany')){opcoes($('RankCompany'),r.empresas,r.companyId);opcoes($('RankArea'),r.areas,r.departmentId||$('RankArea').value);$('RankArea').disabled=query.escopo==='empresa';}
   $('RankStatus').textContent='P'+estado.periodo.id+' · dia '+estado.dia+' · '+(r.escopo==='empresa'?'empresa':'departamento')+(r.ordem==='evolucao'?' · diferença frente ao mesmo dia do período anterior':'');
   $('ForestSummary').innerHTML='<div class="gardenForestSummary">Floresta coletiva<strong>'+fmt(r.floresta.ams,2)+' <small>AMS médio</small></strong><small>'+r.floresta.comDados+' de '+r.floresta.pessoas+' pessoas com cobertura suficiente · '+r.floresta.semDados+' em formação</small></div>';
   $('Forest').innerHTML=r.floresta.arvores.map(p=>'<img class="'+(!p.nivel?'neutral':'')+'" src="'+imagem(p.nivel,p.estagio)+'" alt="'+esc(p.nome)+'" title="'+esc(p.nome)+' · '+(p.nivel?'AMS '+fmt(p.ams,2):'Em formação')+'">').join('');
   $('RankList').innerHTML=r.destaques.length?r.destaques.map(linhaRank).join(''):'<li class="gardenRankEmpty">'+(estado.dia===0?'Percorra o primeiro dia para explorar a floresta.':r.ordem==='evolucao'&&estado.periodo.id===1?'O primeiro período ainda não tem uma base anterior para comparação.':'Não há resultados com cobertura suficiente para este recorte.')+'</li>';
   $('MyRank').innerHTML=r.minhaPosicao?'<p class="gardenEyebrow">SUA POSIÇÃO NESTE RECORTE</p><ol class="gardenRankList">'+linhaRank(r.minhaPosicao)+'</ol>':'<p class="gardenFinePrint">Sua posição aparece quando há dados suficientes e você participa deste recorte.</p>';
   $('RankCriterion').textContent=r.criterio;
  }catch(erro){if(erro.name!=='AbortError'&&ep===rankEpoch){$('RankStatus').textContent=erro.message;$('RankRetry').hidden=false;}}
 }
 $('Shop').addEventListener('show.bs.offcanvas',()=>{playing=false;controles();});
 $('Ranking').addEventListener('show.bs.offcanvas',()=>{if(estado&&!estado.departmentId)$('RankScope').value='empresa';carregarRanking();});
 for(const id of ['RankScope','RankOrder','RankGroup','RankArea'])$(id)?.addEventListener('change',carregarRanking);
 $('RankCompany')?.addEventListener('change',()=>{$('RankScope').value='empresa';$('RankArea').replaceChildren();carregarRanking();});
 $('RankRetry').addEventListener('click',carregarRanking);

 carregar();
})();
