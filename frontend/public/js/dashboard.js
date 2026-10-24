/* AMS calculado no backend; identidade e permissões vêm da sessão Java. */
(() => {
 'use strict';
 const $=id=>document.getElementById('rd'+id);
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const fmt=(n,d=1)=>n==null?'Sem nota':new Intl.NumberFormat('pt-BR',{maximumFractionDigits:d}).format(n);
 const date=s=>s.split('-').reverse().join('/');
 const names={ams:'AMS',security:'Segurança',governance:'Governança',finops:'FinOps',literacy:'AI Literacy',delivery:'Delivery'};
 const colors={ams:'#1e7349',security:'#6588a8',governance:'#a88848',finops:'#8d6ab3',literacy:'#d32e5e',delivery:'#64a095'};
 const state={filters:null,data:null,charts:{},epoch:0,controller:null,page:0};
 let searchTimer,technicalController;
 function options(el,items,selected){el.replaceChildren(...items.map(x=>new Option(x.nome,x.id)));if(items.some(x=>String(x.id)===String(selected)))el.value=selected;}
 function unique(people,key,label){return [...new Map(people.map(p=>[p[key],{id:p[key],nome:label(p)}])).values()].filter(x=>x.id!=null).sort((a,b)=>a.nome.localeCompare(b.nome,'pt-BR'));}
 function choices(el,items,all,selected){options(el,[{id:'',nome:all},...items],selected);}
 function invalidate(){state.epoch++;state.controller?.abort();technicalController?.abort();clearTimeout(searchTimer);return state.epoch;}
 async function api(route,params={}){
  state.controller?.abort();state.controller=new AbortController();
  const r=await fetch('/api/dashboard/ams'+route+'?'+new URLSearchParams(params),{signal:state.controller.signal});
  const body=await r.json().catch(()=>null);
  if(r.status===401){window.location.assign('/login');throw new Error('Sua sessão expirou.');}
  if(!r.ok)throw new Error(body?.detail||body?.message||(typeof body==='string'?body:'Falha ao consultar a API ('+r.status+').'));
  return body;
 }
 function error(e,retry){if(e.name==='AbortError')return;$('Status').innerHTML='<div class="rdAlert"><span>'+esc(e.message)+'</span><button class="rdButton" type="button">Tentar novamente</button></div>';$('Status').querySelector('button').addEventListener('click',retry);}
 function loading(text){$('Status').innerHTML='<div class="rdLoading">'+esc(text)+'</div>';}
 async function init(){
  loading('Conectando à base de dados…');
  try{
   await loadFilters();
  }catch(e){error(e,init);}
 }
 async function loadFilters(){
  const epoch=invalidate();$('Content').hidden=true;$('Filters').hidden=true;loading('Preparando o escopo da visão…');
  try{
   const f=await api('/filtros');if(epoch!==state.epoch)return;
   state.filters=f;const role=$('Perfil').value;
   $('ActiveRole').textContent='Visão '+(role==='colaborador'?'do colaborador':role==='gestor'?'do gestor':'do administrador');
   options($('Empresa'),role==='administrador'?[{id:'',nome:'Todas as empresas'},...unique(f.pessoas,'companyId',p=>p.empresa)]:[{id:f.usuario.companyId,nome:f.usuario.empresa}]);
   $('Empresa').disabled=role!=='administrador';$('GestorField').hidden=role!=='administrador';$('AreaField').hidden=role==='colaborador';
   options($('Periodo'),f.periodos.map(p=>({id:p.id,nome:'P'+p.id+' · '+p.dias+' dias'+(p.parcial?' (parcial)':'')})),5);
   if(!f.periodos.length)throw new Error('Ainda não há registros de uso para definir os períodos.');
   $('Gestor').replaceChildren();$('Area').replaceChildren();$('Pessoa').replaceChildren();
   cascade('company');
   if(role==='colaborador')$('Pessoa').value=f.usuario.id;
   $('Search').value='';$('Order').value='ams';state.page=0;$('Filters').hidden=false;await load();
  }catch(e){if(epoch===state.epoch)error(e,loadFilters);}
 }
 function cascade(from){
  const f=state.filters,role=$('Perfil').value;let people=f.pessoas;
  if(role==='gestor'){
   const u=f.usuario,old=$('Pessoa').value;
   options($('Empresa'),[{id:u.companyId,nome:u.empresa}],u.companyId);$('Empresa').disabled=true;
   options($('Gestor'),[{id:u.departmentId?u.id:'',nome:u.nome}]);$('Gestor').disabled=true;
   options($('Area'),[{id:u.departmentId||'',nome:u.departamento||'Sem departamento'}]);$('Area').disabled=true;
   people=people.filter(p=>p.id===u.id||(p.companyId===u.companyId&&u.departmentId&&p.departmentId===u.departmentId));
   choices($('Pessoa'),people.map(p=>({id:p.id,nome:p.nome+(p.id===u.id?' (eu)':'')})),'Toda a minha equipe',old);$('Pessoa').disabled=false;
   return;
  }
  if($('Empresa').value)people=people.filter(p=>p.companyId===$('Empresa').value);
  if(from==='company'){
   const managers=people.filter(p=>p.posicao?.toLowerCase()==='gestor'||(role==='gestor'&&p.id===f.usuario.id));
   choices($('Gestor'),managers.filter(p=>p.departmentId),'Todos os gestores','');
   $('Gestor').disabled=role==='administrador'&&!$('Empresa').value;
  }
  if($('Gestor').value){const manager=people.find(p=>p.id===$('Gestor').value);people=people.filter(p=>p.companyId===manager.companyId&&p.departmentId===manager.departmentId);}
  if(from==='company'||from==='manager'){
   choices($('Area'),unique(people,'departmentId',p=>p.departamento||'Sem departamento'),'Todas as áreas','');
   $('Area').disabled=role==='administrador'&&!$('Empresa').value;
  }
  if($('Area').value)people=people.filter(p=>p.departmentId===$('Area').value);
  const old=from==='area'?'':$('Pessoa').value;
  if(role==='colaborador'){options($('Pessoa'),people,f.usuario.id);$('Pessoa').disabled=true;}
  else{choices($('Pessoa'),people.map(p=>({id:p.id,nome:p.nome+(p.id===f.usuario.id?' (eu)':'')})),'Todos os colaboradores',old);$('Pessoa').disabled=role==='administrador'&&!$('Empresa').value;}
 }
 function params(){return{perfil:$('Perfil').value,companyId:$('Empresa').value,gestorId:$('Gestor').value,departmentId:$('Area').value,employeeId:$('Pessoa').value,periodo:$('Periodo').value,page:state.page,size:10,busca:$('Search').value,ordem:$('Order').value};}
 async function load(tableOnly=false){
  const epoch=invalidate();if(!tableOnly)$('Content').hidden=true;loading(tableOnly?'Atualizando colaboradores…':'Calculando AMS e evolução dos cinco períodos…');
  $('Prev').disabled=true;$('Next').disabled=true;
  try{
   const data=await api('',params());if(epoch!==state.epoch)return;state.data=data;$('Status').replaceChildren();$('Content').hidden=false;
   if(tableOnly){renderPeople();if($('Technical').open)loadTechnical();}else render();
  }catch(e){if(epoch===state.epoch)error(e,()=>load(tableOnly));}
 }
 function metricCard(title,value,detail,extra='',name=false){return'<article class="rdKpi"><div class="rdKpiLabel">'+esc(title)+'</div><div class="rdKpiValue'+(name?' amsName':'')+'">'+esc(value)+'</div><div class="rdKpiDetail">'+esc(detail)+'</div><span class="rdDelta">'+esc(extra)+'</span></article>';}
 function variation(now,before){if(now==null||before==null)return'Comparação indisponível';const diff=now-before;return(diff>0?'+':'')+fmt(diff,2)+' pontos';}
 function render(){
  const d=state.data,r=d.resumo,p=d.periodo,role=$('Perfil').value;
  const person=state.filters.pessoas.find(x=>x.id===$('Pessoa').value);
  $('Scope').textContent=role==='colaborador'?'MINHA EVOLUÇÃO':person?'ANÁLISE INDIVIDUAL':role==='gestor'?'MINHA EQUIPE':'VISÃO ORGANIZACIONAL';
  $('Title').textContent=person?.nome||($('Area').value?$('Area').selectedOptions[0].textContent:$('Empresa').value?$('Empresa').selectedOptions[0].textContent:'Todas as empresas');
  $('Subtitle').textContent=(person?person.cargo+' · ':'')+date(p.inicio)+' a '+date(p.fim);
  $('PeriodBadge').textContent='P'+p.id+' · '+p.dias+' dias'+(p.parcial?' · parcial':'');
  $('Criterion').textContent=d.criterio;
  const previous=p.id>1?d.evolucao[p.id-2].resumo.nota:null;
  $('Kpis').innerHTML=metricCard(person?(person.id===state.filters.usuario.id?'Minha nota AMS':'Nota AMS individual'):'AMS médio',r.nota==null?'Indisponível':fmt(r.nota,2)+' / 100',person?(d.pessoal.desenvolvedor?'Desenvolvedor · 5 dimensões':'Demais profissionais · 4 dimensões'):'Peso igual por colaborador com AMS disponível')+metricCard('Variação entre períodos',p.id===1?'Primeiro período':variation(r.nota,previous),'Em relação a P'+Math.max(1,p.id-1),'Mesma regra por perfil profissional')+metricCard('Cobertura do AMS',fmt(r.validos,0)+' / '+fmt(r.pessoas,0),'Colaboradores com resultado disponível',fmt(r.semAms,0)+' sem AMS')+metricCard('Notas de dimensão ausentes',fmt(r.notasAusentes,0)+' / '+fmt(r.dimensoesAplicaveis,0),'Ausência original, incluindo AI Literacy',r.dimensoesAplicaveis?fmt(100*r.notasAusentes/r.dimensoesAplicaveis,1)+'% das dimensões aplicáveis':'Sem pessoas neste recorte');
  renderDimensions();$('Team').hidden=role==='colaborador'||!!person;
  $('ComparisonSection').hidden=role==='colaborador'||!!person;
  options($('Group'),role==='administrador'?[{id:'empresas',nome:'Empresas'},{id:'areas',nome:'Áreas'}]:[{id:'areas',nome:'Áreas'}],$('Group').value);
  const devApplicable=!d.pessoal||d.pessoal.desenvolvedor;
  const series=$('Series').value;options($('Series'),[{id:'all',nome:'AMS + todas as dimensões'},{id:'ams',nome:'Somente AMS'},...Object.keys(names).filter(k=>k!=='ams'&&(k!=='delivery'||devApplicable)).map(k=>({id:k,nome:'AMS + '+names[k]}))],series);
  const winner=r.melhor;$('WinnerTitle').textContent=winner?'Evolução de '+winner.nome:'Nenhum destaque disponível';
  charts();renderPeople();$('TechnicalContent').replaceChildren();if($('Technical').open)loadTechnical();
 }
 function dimensionCell(d){if(!d)return'<span class="amsNull">Não se aplica</span>';if(d.registrada==null)return'<span class="amsNull">'+(d.id==='literacy'?'0 · sem nota registrada':'Sem nota')+'</span>';return fmt(d.utilizada,2);}
 function renderDimensions(){
  const d=state.data,c=d.pessoal;
  $('DimensionsTitle').textContent=c?'Dimensões e pesos desta pessoa':'Médias das dimensões no recorte';
  const dimensions=c?c.dimensoes:d.resumo.dimensoes;
  $('Dimensions').innerHTML=dimensions.filter(x=>c||x.validos+x.ausentes>0).map(x=>{
   const value=c?x.utilizada:x.nota,missing=c?x.registrada==null:x.ausentes>0;
   const note=c?(x.registrada==null?(x.id==='literacy'?'Sem nota registrada · zero aplicado':'Sem informação · dimensão excluída'):'Nota registrada: '+fmt(x.registrada,2)):'Usadas na média: '+x.validos+' · sem registro: '+x.ausentes+(x.zerosLiteracy?' · '+x.zerosLiteracy+' zeros por ausência':'');
   return'<article class="amsDimension'+(missing?' missing':'')+'"><h3>'+esc(x.nome)+'</h3><strong>'+esc(value==null?'Sem nota':fmt(value,2))+'</strong><p>'+esc(note)+'</p>'+(c?'<p>Peso: '+fmt(x.pesoEfetivo*100,2)+'% (inicial '+fmt(x.pesoInicial*100,0)+'%)</p>':'')+'<div class="amsProgress"><span style="width:'+(value??0)+'%"></span></div></article>';
  }).join('');
  $('DimensionsNote').textContent=c?(c.desenvolvedor?'Delivery aplicável ao cargo de desenvolvedor.':'Delivery não se aplica a este cargo e não participa do AMS.'):'Médias por dimensão usam as notas disponíveis; AI Literacy inclui os zeros por ausência. Delivery inclui somente desenvolvedores. A média das dimensões não substitui a média dos AMS individuais.';
  $('Formula').hidden=!c;
  if(c)$('FormulaContent').innerHTML=table(['Dimensão','Registrada','Usada','Peso inicial','Peso efetivo','Contribuição'],c.dimensoes.map(x=>[x.nome,x.registrada==null?'Sem nota':fmt(x.registrada,2),fmt(x.utilizada,2),fmt(x.pesoInicial*100,2)+'%',fmt(x.pesoEfetivo*100,2)+'%',fmt(x.contribuicao,2)]))+'<p>'+esc(c.nota==null?'AMS indisponível: nenhuma dimensão tem nota registrada.':'AMS = soma das contribuições = '+fmt(c.nota,2)+'. O cálculo usa a precisão completa; os valores exibidos são arredondados.')+'</p>';
  $('Delivery').hidden=!c?.desenvolvedor;
  if(c?.desenvolvedor)$('Delivery').innerHTML='<h3>Indicadores de Delivery</h3><p class="rdMuted">Fórmula existente: 70% individual + 30% relativa, se ambas disponíveis. Sem uma das parcelas, usa a parcela disponível. A nota relativa exige quatro pessoas do mesmo cargo.</p><div class="rdTableWrap" role="region" aria-label="Tabela de indicadores de Delivery" tabindex="0">'+table(['Indicador','Nota individual','Peso individual','Nota relativa','Peso relativo'],c.delivery.map(x=>[x.nome,fmt(x.notaIndividual,2),fmt(x.pesoIndividual*100,2)+'%',fmt(x.notaRelativa,2),fmt(x.pesoRelativo*100,2)+'%']))+'</div>';
 }
 function table(headers,rows){return'<table><thead><tr>'+headers.map(h=>'<th scope="col">'+esc(h)+'</th>').join('')+'</tr></thead><tbody>'+rows.map(row=>'<tr>'+row.map(x=>'<td>'+esc(x)+'</td>').join('')+'</tr>').join('')+'</tbody></table>';}
 function pointValue(point,key){return key==='ams'?point.resumo.nota:point.resumo.dimensoes.find(x=>x.id===key)?.nota??null;}
 function dataset(label,values,color,width=2){return{label,data:values,borderColor:color,backgroundColor:color,borderWidth:width,pointRadius:3,tension:.15,spanGaps:false};}
 function draw(id,type,data,extra={}){
  state.charts[id]?.destroy();
  if(!window.Chart){$(id).hidden=true;document.querySelectorAll('details').forEach(x=>x.open=true);return;}
  $(id).hidden=false;
  state.charts[id]=new Chart($(id),{type,data,options:{responsive:true,maintainAspectRatio:false,resizeDelay:80,animation:matchMedia('(prefers-reduced-motion: reduce)').matches?false:{duration:200},plugins:{legend:{display:type==='line',position:'bottom',maxHeight:95,labels:{boxWidth:10,font:{size:10},generateLabels:chart=>Chart.defaults.plugins.legend.labels.generateLabels(chart).map(x=>({...x,text:x.text.length>24?x.text.slice(0,23)+'…':x.text}))}}},scales:{x:{grid:{display:false},ticks:{font:{size:10}}},y:{min:0,max:type==='line'?100:undefined,grid:{color:'#edf0e8'},ticks:{font:{size:10}}}},...extra}});
 }
 function charts(){
  const d=state.data,series=$('Series').value,keys=series==='all'?['ams',...d.resumo.dimensoes.filter(x=>x.validos+x.ausentes>0).map(x=>x.id)]:series==='ams'?['ams']:['ams',series];
  const labels=d.evolucao.map(x=>'P'+x.periodo.id+' · '+x.periodo.dias+'d');
  draw('Evolution','line',{labels,datasets:keys.map(k=>dataset(names[k],d.evolucao.map(x=>pointValue(x,k)),colors[k],k==='ams'?3:1.5))});
  $('EvolutionTable').innerHTML=table(['Período','AMS','Segurança','Governança','FinOps','AI Literacy','Delivery','Notas ausentes'],d.evolucao.map(x=>['P'+x.periodo.id+' · '+date(x.periodo.inicio)+'–'+date(x.periodo.fim),...['ams','security','governance','finops','literacy','delivery'].map(k=>{const note=pointValue(x,k);const dim=x.pessoal?.dimensoes.find(z=>z.id===k);return dim?.estado==='zero_por_ausencia'?'0 (ausente)':k==='delivery'&&x.pessoal&&!x.pessoal.desenvolvedor?'Não se aplica':fmt(note,2);}),x.resumo.notasAusentes+'/'+x.resumo.dimensoesAplicaveis]));
  draw('Distribution','bar',{labels:d.distribuicao.map(x=>x.nome),datasets:[{label:'Pessoas',data:d.distribuicao.map(x=>x.quantidade),backgroundColor:d.distribuicao.map((_,i)=>i===5?'#b9aa8d':'#7e9e69'),borderRadius:4}]});
  $('DistributionTable').innerHTML=table(['Faixa AMS','Pessoas'],d.distribuicao.map(x=>[x.nome,x.quantidade]));
  draw('Leaders','line',{labels,datasets:[dataset('Melhor AMS',d.evolucao.map(x=>x.resumo.melhor?.nota??null),'#a88848')]});
  $('LeadersTable').innerHTML=table(['Período','Colaborador','AMS','Pessoas empatadas'],d.evolucao.map(x=>['P'+x.periodo.id,x.resumo.melhor?.nome||'Sem destaque',fmt(x.resumo.melhor?.nota,2),x.resumo.melhor?.empatados??0]));
  draw('Winner','line',{labels,datasets:[dataset(d.resumo.melhor?.nome||'Destaque',d.evolucaoDestaque.map(x=>x.resumo.nota),'#1e7349')]});
  $('WinnerTable').innerHTML=table(['Período','AMS'],d.evolucaoDestaque.map(x=>['P'+x.periodo.id,fmt(x.resumo.nota,2)]));
  renderComparison();
 }
 function renderComparison(){
  if(!state.data)return;
  const d=state.data,metric=$('CompareMetric').value,groups=d[$('Group').value]||[];
  const sorted=[...groups].sort((a,b)=>(pointValue(b.evolucao[d.periodo.id-1],metric)??-1)-(pointValue(a.evolucao[d.periodo.id-1],metric)??-1));
  const bars=sorted.slice(0,12),lines=sorted.slice(0,6);
  $('ComparisonWrap').style.height=Math.max(275,bars.length*32)+'px';
  $('ComparisonNote').textContent='Barras: até 12 maiores notas do período. Evolução: até 6 maiores notas do período. A tabela abaixo contém todas as organizações do recorte, incluindo notas ausentes.';
  draw('Comparison','bar',{labels:bars.map(x=>x.nome),datasets:[{label:names[metric],data:bars.map(x=>pointValue(x.evolucao[d.periodo.id-1],metric)),backgroundColor:'#77986a',borderRadius:4}]},{indexAxis:'y',plugins:{legend:{display:false}},scales:{x:{min:0,max:100},y:{ticks:{autoSkip:false,font:{size:10},callback:function(v){const s=this.getLabelForValue(v);return s.length>20?s.slice(0,19)+'…':s;}},grid:{display:false}}}});
  const palette=['#1e7349','#6588a8','#d32e5e','#a88848','#8d6ab3','#64a095','#38668e','#a14e36'];
  draw('OrganizationEvolution','line',{labels:d.evolucao.map(x=>'P'+x.periodo.id),datasets:lines.map((g,i)=>dataset(g.nome,g.evolucao.map(x=>pointValue(x,metric)),palette[i%palette.length]))});
  $('ComparisonTable').innerHTML=table(['Organização',...d.evolucao.map(x=>'P'+x.periodo.id+' · '+names[metric]),'AMS válidos / pessoas','Notas ausentes / aplicáveis'],sorted.map(g=>{const a=g.evolucao[d.periodo.id-1].resumo;return[g.nome,...g.evolucao.map(x=>fmt(pointValue(x,metric),2)),a.validos+'/'+a.pessoas,a.notasAusentes+'/'+a.dimensoesAplicaveis];}));
 }
 function renderPeople(){
  const d=state.data,page=d.integrantes;
  $('Table').innerHTML=page.content.length?'<table><thead><tr><th scope="col">Colaborador / cargo</th><th scope="col">AMS</th>'+['security','governance','finops','literacy','delivery'].map(k=>'<th scope="col">'+names[k]+'</th>').join('')+'<th scope="col">Ausentes</th></tr></thead><tbody>'+page.content.map((l,i)=>'<tr><td><button class="rdPerson" type="button" data-person="'+i+'">'+esc(l.pessoa.nome)+'</button><br><small>'+esc(l.pessoa.cargo)+'</small></td><td class="amsScoreCell">'+(l.calculo.nota==null?'Indisponível':fmt(l.calculo.nota,2))+'</td>'+['security','governance','finops','literacy','delivery'].map(k=>'<td>'+dimensionCell(l.calculo.dimensoes.find(x=>x.id===k))+'</td>').join('')+'<td>'+l.calculo.notasAusentes+'/'+l.calculo.dimensoesAplicaveis+'</td></tr>').join('')+'</tbody></table>':'<div class="rdEmpty">Nenhum colaborador para esta busca.</div>';
  $('Table').querySelectorAll('[data-person]').forEach(button=>button.addEventListener('click',()=>{const person=page.content[Number(button.dataset.person)].pessoa;$('Empresa').value=person.companyId;cascade('company');$('Pessoa').value=person.id;$('Search').value='';state.page=0;load();}));
  $('PageInfo').textContent=page.totalElements+' resultados · página '+(page.page+1)+' de '+Math.max(1,Math.ceil(page.totalElements/page.size));
  $('Prev').disabled=page.page===0;$('Next').disabled=(page.page+1)*page.size>=page.totalElements;
  $('PeopleChartWrap').hidden=$('Perfil').value==='colaborador';
  $('PeopleChartWrap').style.height=Math.max(280,page.content.length*34+45)+'px';
  draw('PeopleChart','bar',{labels:page.content.map(l=>l.pessoa.nome+(l.calculo.nota==null?' · AMS indisponível':'')),datasets:[{label:'AMS',data:page.content.map(l=>l.calculo.nota),backgroundColor:'#7f9f6c',borderRadius:4}]},{indexAxis:'y',plugins:{legend:{display:false}},scales:{x:{min:0,max:100},y:{ticks:{autoSkip:false,font:{size:10},callback:function(v){const s=this.getLabelForValue(v);return s.length>19?s.slice(0,18)+'…':s;}}}}});
 }
 async function loadTechnical(){
  const epoch=state.epoch,query=params(),f=state.filters;
  technicalController?.abort();technicalController=new AbortController();
  if(!query.companyId){$('TechnicalContent').innerHTML='<p class="rdNotice">Selecione uma empresa para consultar o uso de IA e as entregas.</p>';return;}
  const manager=f.pessoas.find(p=>p.id===query.gestorId);
  const p={companyId:query.companyId,periodo:query.periodo,
   escopo:query.employeeId?'pessoal':query.perfil==='gestor'?'equipe':query.departmentId||manager?'area':'empresa',
   employeeId:query.employeeId,departmentId:query.departmentId||manager?.departmentId||''};
  $('TechnicalContent').innerHTML='<div class="rdLoading">Consultando uso e entregas…</div>';
  try{
   const r=await fetch('/api/dashboard/resumo?'+new URLSearchParams(p),{signal:technicalController.signal});const d=await r.json();
   if(!r.ok)throw new Error(d.message||'Não foi possível consultar o detalhamento técnico.');if(epoch!==state.epoch)return;
   const m=d.metricas;
   $('TechnicalContent').innerHTML='<div class="rdQualityMetrics">'+[['Requisições',m.requisicoes],['Tokens',m.tokens],['Issues concluídas',m.issues],['PRs integrados',m.prs]].map(([label,value])=>'<div class="rdSmallMetric">'+esc(label)+'<strong>'+fmt(value,0)+'</strong></div>').join('')+'</div><div class="rdCharts"><div><h3 class="amsSubheading">Requisições de IA por dia corrido</h3><div class="rdCanvas"><canvas id="rdTechnicalEvolution" role="img" aria-label="Requisições por dia nos cinco períodos"></canvas></div></div><div><h3 class="amsSubheading">Requisições por ferramenta</h3><div class="rdCanvas"><canvas id="rdTechnicalTools" role="img" aria-label="Uso de IA por ferramenta"></canvas></div></div></div><details><summary>Ver uso e entregas em tabela</summary><div class="rdTableWrap" tabindex="0" role="region" aria-label="Uso e entregas por período">'+table(['Período','Requisições','Tokens','Issues','PRs'],d.evolucao.map(x=>['P'+x.periodo.id,fmt(x.metricas.requisicoes,0),fmt(x.metricas.tokens,0),fmt(x.metricas.issues,0),fmt(x.metricas.prs,0)]))+'</div><div class="rdTableWrap" tabindex="0" role="region" aria-label="Uso por ferramenta">'+table(['Ferramenta','Requisições'],d.ferramentas.map(x=>[x.nome,fmt(x.requisicoes,0)]))+'</div></details>';
   draw('TechnicalEvolution','line',{labels:d.evolucao.map(x=>'P'+x.periodo.id),datasets:[dataset('Requisições/dia',d.evolucao.map(x=>x.metricas.requisicoes/x.periodo.dias),'#1e7349')]},{scales:{y:{beginAtZero:true}}});
   draw('TechnicalTools','bar',{labels:d.ferramentas.map(x=>x.nome),datasets:[{label:'Requisições',data:d.ferramentas.map(x=>x.requisicoes),backgroundColor:'#7f9f6c'}]});
  }catch(e){if(e.name!=='AbortError'&&epoch===state.epoch)$('TechnicalContent').innerHTML='<p class="rdNotice">'+esc(e.message)+'</p>';}
 }
 $('Technical').addEventListener('toggle',()=>{if($('Technical').open&&state.data)loadTechnical();});
 for(const [id,from]of[['Empresa','company'],['Gestor','manager'],['Area','area']])$(id).addEventListener('change',()=>{cascade(from);state.page=0;load();});
 for(const id of['Pessoa','Periodo'])$(id).addEventListener('change',()=>{state.page=0;load();});
 $('Self').addEventListener('click',()=>{$('Empresa').value=state.filters.usuario.companyId;cascade('company');$('Pessoa').value=state.filters.usuario.id;state.page=0;$('Search').value='';load();});
 $('Reset').addEventListener('click',loadFilters);
 $('Series').addEventListener('change',charts);$('Group').addEventListener('change',renderComparison);$('CompareMetric').addEventListener('change',renderComparison);
 $('Search').addEventListener('input',()=>{clearTimeout(searchTimer);state.controller?.abort();state.page=0;searchTimer=setTimeout(()=>load(true),300);});
 $('Order').addEventListener('change',()=>{state.page=0;load(true);});
 $('Prev').addEventListener('click',()=>{state.page--;load(true);});$('Next').addEventListener('click',()=>{state.page++;load(true);});
 document.addEventListener('toggle',event=>{if(event.target.tagName==='DETAILS'&&event.target.open)requestAnimationFrame(()=>Object.values(state.charts).filter(c=>event.target.contains(c.canvas)).forEach(c=>c.resize()));},true);
 init();
})();
