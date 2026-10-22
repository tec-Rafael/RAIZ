/* Dashboard com dados do banco. O perfil é demonstrativo, não autenticação. */
(() => {
    'use strict';
    const $ = id => document.getElementById('rd' + id);
    const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const fmt = (v, digits = 0) => v == null ? '—' : new Intl.NumberFormat('pt-BR', {maximumFractionDigits:digits}).format(v);
    const date = v => v ? v.split('-').reverse().join('/') : 'sem registros';
    const state = {users:[], filters:null, data:null, page:0, charts:[], epoch:0, requests:{}};
    let searchTimer;
    function options(el, items, chosen) {
        el.replaceChildren(...items.map(x => new Option(x.nome, x.id)));
        if (chosen != null && items.some(x => String(x.id) === String(chosen))) el.value = chosen;
    }
    function abort(key) { state.requests[key]?.abort(); }
    async function api(route, params, key) {
        abort(key);
        const controller = new AbortController(); state.requests[key] = controller;
        const response = await fetch('/api/dashboard/' + route + '?' + new URLSearchParams(params), {signal:controller.signal});
        const body = await response.json().catch(() => null);
        if (!response.ok) throw new Error(typeof body === 'string' ? body : body?.message || body?.detail || 'Não foi possível consultar os dados (' + response.status + ').');
        return body;
    }
    function failure(target, error, retry) {
        if (error.name === 'AbortError') return;
        target.innerHTML = '<div class="rdAlert"><span>' + esc(error.message) + '</span><button type="button" class="rdButton">Tentar novamente</button></div>';
        target.querySelector('button').addEventListener('click', retry);
    }
    function destroyCharts() { state.charts.forEach(c => c.destroy()); state.charts = []; }
    function invalidate() {
        state.epoch++; abort('data'); abort('table'); clearTimeout(searchTimer);
        $('Content').hidden = true; state.data = null; destroyCharts();
    }
    async function init() {
        $('Status').innerHTML = '<div class="rdLoading">Conectando à base de dados…</div>';
        $('Usuario').disabled = true;
        try {
            state.users = await api('usuarios', {}, 'users');
            if (!state.users.length) throw new Error('Não há funcionários cadastrados na base.');
            const companies = [...new Map(state.users.map(u => [u.companyId,{id:u.companyId,nome:u.empresa}])).values()].sort((a,b) => a.id.localeCompare(b.id));
            options($('Empresa'), companies, $('Empresa').value);
            $('Empresa').disabled = false;
            configureUsers();
            await loadFilters();
        } catch(e) { failure($('Status'), e, init); }
    }
    function configureUsers() {
        const users = state.users.filter(u => u.companyId === $('Empresa').value);
        const current = $('Usuario').value;
        const chosen = users.some(u => u.id === current) ? current : [...users].sort((a,b) => a.id.localeCompare(b.id))[0]?.id;
        options($('Usuario'), users, chosen);
        $('Usuario').disabled = false;
    }
    async function loadFilters() {
        invalidate(); $('Filters').hidden = true;
        const epoch = state.epoch;
        $('Status').innerHTML = '<div class="rdLoading">Preparando a visão selecionada…</div>';
        try {
            const f = await api('filtros', {perfil:$('Perfil').value,usuarioId:$('Usuario').value}, 'filters');
            if (epoch !== state.epoch) return;
            state.filters = f;
            if (!f.periodos.length) throw new Error('A base ainda não possui registros de uso de IA para definir os períodos.');
            const views = [{id:'self',nome:'Minha produtividade'}];
            if (f.perfil === 'gestor') {
                if (f.usuario.departmentId) views.push({id:'equipe',nome:'Minha equipe'});
                views.push({id:'pessoa',nome:'Um colaborador'});
            }
            if (f.perfil === 'administrador') views.push({id:'empresa',nome:'Toda a empresa'},{id:'area',nome:'Uma área'},{id:'pessoa',nome:'Um colaborador'});
            options($('Visao'), views);
            options($('Area'), f.areas);
            options($('Periodo'), f.periodos.map(p => ({id:p.id,nome:'P' + p.id + ' · ' + p.dias + ' dias' + (p.parcial ? ' (atual)' : '')})), 5);
            options($('Ferramenta'), [{id:'',nome:'Todas as ferramentas'},...f.ferramentas]);
            $('Search').value = ''; $('Group').value = 'pessoa'; $('Order').value = 'requisicoes';
            $('GroupField').hidden = f.perfil !== 'administrador';
            configureView(); $('Filters').hidden = false;
            await loadData();
        } catch(e) { if (epoch === state.epoch) failure($('Status'), e, loadFilters); }
    }
    function configureView() {
        const view = $('Visao').value;
        $('AreaField').hidden = view !== 'area';
        $('PessoaField').hidden = view !== 'pessoa';
        options($('Pessoa'), state.filters.pessoas, state.filters.usuario.id);
        state.page = 0;
    }
    function params() {
        const view = $('Visao').value;
        return {perfil:$('Perfil').value,usuarioId:$('Usuario').value,
            escopo: view === 'self' || view === 'pessoa' ? 'pessoal' : view,
            employeeId:view === 'pessoa' ? $('Pessoa').value : state.filters.usuario.id,
            departmentId:$('Area').value,periodo:$('Periodo').value,aiToolId:$('Ferramenta').value};
    }
    async function loadData() {
        invalidate(); state.page = 0;
        const epoch = state.epoch;
        $('Status').innerHTML = '<div class="rdLoading">Calculando indicadores e evolução…</div>';
        try {
            const data = await api('resumo', params(), 'data');
            if (epoch !== state.epoch) return;
            state.data = data; $('Status').replaceChildren(); $('Content').hidden = false;
            render(); await loadTable();
        } catch(e) { if (epoch === state.epoch) failure($('Status'), e, loadData); }
    }
    function delta(metric) {
        const d = state.data;
        if (!d.anterior) return 'Primeiro período da análise';
        const now = d.metricas[metric] / d.periodo.dias, before = d.anterior[metric] / 90;
        if (before === 0) return now === 0 ? 'Sem variação por dia' : 'Sem base anterior para variação percentual';
        const change = (now / before - 1) * 100;
        return (change >= 0 ? '+' : '') + fmt(change,1) + '% por dia vs. P' + (d.periodo.id-1);
    }
    function kpi(title, metric, icon, detail) {
        return '<article class="rdKpi"><div class="rdKpiLabel"><span>' + title + '</span><i class="bi ' + icon + '" aria-hidden="true"></i></div><div class="rdKpiValue">' + fmt(state.data.metricas[metric]) + '</div><div class="rdKpiDetail">' + detail + '</div><span class="rdDelta">' + delta(metric) + '</span></article>';
    }
    function small(title, value, note) {
        return '<div class="rdSmallMetric">' + esc(title) + '<strong>' + esc(value) + '</strong><span>' + esc(note) + '</span></div>';
    }
    function render() {
        const d = state.data, m = d.metricas, f = state.filters, view = $('Visao').value;
        const person = view === 'pessoa' ? f.pessoas.find(p => p.id === $('Pessoa').value) : f.usuario;
        $('Title').textContent = view === 'self' || view === 'pessoa' ? person.nome : view === 'empresa' ? f.usuario.empresa : view === 'equipe' ? 'Equipe · ' + f.usuario.departamento : $('Area').selectedOptions[0]?.textContent;
        $('Subtitle').textContent = f.usuario.empresa + ' · ' + date(d.periodo.inicio) + ' a ' + date(d.periodo.fim);
        $('PeriodBadge').textContent = 'P' + d.periodo.id + ' · ' + d.periodo.dias + ' dias' + (d.periodo.parcial ? ' · parcial' : '');
        $('Coverage').textContent = 'Último registro de uso de IA na empresa: ' + date(d.ultimaData) + '. ' + (d.ultimaData && d.ultimaData < d.periodo.fim ? 'A base não alcança o fim deste período; os totais refletem apenas os registros disponíveis. ' : '') + 'Comparações usam dias corridos. O filtro de ferramenta afeta somente o uso de IA.';
        $('Kpis').innerHTML = kpi('Issues concluídas','issues','bi-check2-circle',fmt(m.issues/d.periodo.dias,2) + ' por dia · fechamento no período') + kpi('PRs integrados','prs','bi-git',fmt(m.prs/d.periodo.dias,2) + ' por dia · integração no período') + kpi('Requisições de IA','requisicoes','bi-stars',fmt(m.registros) + ' registros de uso') + kpi('Tokens consumidos','tokens','bi-layers','Entrada + saída · volume de consumo');
        $('QualityMetrics').innerHTML = small('Ciclo mediano · issues',m.cicloIssuesHoras == null ? '—' : fmt(m.cicloIssuesHoras,1) + ' h',fmt(m.issuesComCiclo) + ' de ' + fmt(m.issues) + ' entregas com datas válidas') + small('Ciclo mediano · PRs',m.cicloPrsHoras == null ? '—' : fmt(m.cicloPrsHoras,1) + ' h',fmt(m.prsComCiclo) + ' de ' + fmt(m.prs) + ' entregas com datas válidas') + small('Sucesso de uso de IA',m.sucesso == null ? '—' : fmt(m.sucesso,1) + '%','Proporção de registros com status success') + small('Latência média',m.latencia == null ? '—' : fmt(m.latencia) + ' ms',fmt(m.registrosComLatencia) + ' de ' + fmt(m.registros) + ' registros com latência');
        if (d.scoresIndividuais) {
            for (const [key,title] of [['literacy','AI Literacy'],['governance','Governança'],['security','Segurança']]) $('QualityMetrics').innerHTML += small(title,d.scores[key] == null ? '—' : fmt(d.scores[key],1) + ' / 100',d.scores[key] == null ? 'Sem dados para cálculo' : 'Score individual · período selecionado');
        } else $('QualityMetrics').innerHTML += small('Scores de maturidade','Visão individual','Selecione um colaborador para consultar os scores disponíveis.');
        renderCharts();
    }
    function table(headers, rows) {
        return '<table><thead><tr>' + headers.map(x => '<th scope="col">' + esc(x) + '</th>').join('') + '</tr></thead><tbody>' + rows.map(row => '<tr>' + row.map(x => '<td>' + esc(x) + '</td>').join('') + '</tr>').join('') + '</tbody></table>';
    }
    function renderCharts() {
        if (!state.data) return;
        destroyCharts();
        const d = state.data, metric = $('Metrica').value, daily = $('Modo').value === 'dia';
        const title = $('Metrica').selectedOptions[0].textContent;
        const values = d.evolucao.map(x => x.metricas[metric] / (daily ? x.periodo.dias : 1));
        $('ChartNote').textContent = daily ? 'Taxa por dia corrido: períodos completos de 90 dias e período atual de 66 dias.' : 'Totais observados. P5 possui 66 dias; os demais possuem 90 dias.';
        $('EvolutionTable').innerHTML = table(['Período','Datas','Dias',title + (daily ? '/dia' : '')],d.evolucao.map((x,i) => ['P' + x.periodo.id,date(x.periodo.inicio) + ' – ' + date(x.periodo.fim),x.periodo.dias,fmt(values[i],daily ? 2 : 0)]));
        $('ToolsTable').innerHTML = table(['Ferramenta','Requisições','Tokens'],d.ferramentas.map(x => [x.nome,fmt(x.requisicoes),fmt(x.tokens)]));
        $('ToolsEmpty').hidden = d.ferramentas.length !== 0;
        $('ToolsWrap').hidden = d.ferramentas.length === 0;
        if (!window.Chart) {
            $('ChartNote').textContent += ' Gráficos indisponíveis; consulte os valores na tabela abaixo.';
            $('EvolutionTable').closest('details').open = true; $('ToolsTable').closest('details').open = true; return;
        }
        const base = {
            responsive:true, maintainAspectRatio:false,
            animation:window.matchMedia('(prefers-reduced-motion: reduce)').matches ? false : {duration:250},
            plugins:{legend:{display:false}},
            scales:{
                x:{grid:{display:false},ticks:{color:'#778471',font:{size:11}}},
                y:{beginAtZero:true,grid:{color:'#edf0e8'},border:{display:false},ticks:{color:'#778471',font:{size:11}}}
            }
        };
        state.charts.push(new Chart($('Evolution'),{type:'line',data:{labels:d.evolucao.map(x => 'P' + x.periodo.id + ' · ' + x.periodo.dias + 'd'),datasets:[{label:title + (daily ? '/dia' : ''),data:values,borderColor:'#28704b',backgroundColor:'rgba(65,121,74,.08)',fill:true,tension:.25,pointRadius:5,pointBackgroundColor:d.evolucao.map(x => x.periodo.id === d.periodo.id ? '#d32e5e' : '#28704b'),borderWidth:2}]},options:base}));
        if (d.ferramentas.length) state.charts.push(new Chart($('Tools'),{type:'bar',data:{labels:d.ferramentas.map(x => x.nome),datasets:[{label:'Requisições',data:d.ferramentas.map(x => x.requisicoes),backgroundColor:d.ferramentas.map((_,i) => i === 0 ? '#275f3e' : '#a8bc91'),borderRadius:4,maxBarThickness:24}]},options:{...base,indexAxis:'y',scales:{x:{beginAtZero:true,grid:{color:'#edf0e8'},ticks:{font:{size:10}}},y:{grid:{display:false},ticks:{font:{size:11},autoSkip:false}}}}}));
    }
    async function loadTable() {
        if (!state.data) return;
        const epoch = state.epoch;
        $('Table').replaceChildren(); $('Prev').disabled = true; $('Next').disabled = true; $('PageInfo').textContent = '';
        $('TableStatus').innerHTML = '<div class="rdLoading">Consultando detalhamento…</div>';
        try {
            const result = await api('integrantes',{...params(),agrupamento:$('Group').value,busca:$('Search').value,ordem:$('Order').value,page:state.page,size:10},'table');
            if (epoch !== state.epoch) return;
            $('TableStatus').replaceChildren();
            if (!result.content.length) $('Table').innerHTML = '<div class="rdEmpty">Nenhum resultado para esta busca.</div>';
            else {
                $('Table').innerHTML = '<table><thead><tr><th scope="col">' + ($('Group').value === 'area' ? 'Área' : 'Colaborador') + '</th><th scope="col">Pessoas</th><th scope="col">Issues</th><th scope="col">PRs</th><th scope="col">Requisições</th><th scope="col">Tokens</th></tr></thead><tbody>' + result.content.map((x,i) => '<tr><td>' + (x.id ? '<button type="button" class="rdPerson" data-row="' + i + '">' + esc(x.nome) + '</button>' : esc(x.nome)) + '</td><td>' + fmt(x.pessoas) + '</td><td>' + fmt(x.issues) + '</td><td>' + fmt(x.prs) + '</td><td>' + fmt(x.requisicoes) + '</td><td>' + fmt(x.tokens) + '</td></tr>').join('') + '</tbody></table>';
                $('Table').querySelectorAll('[data-row]').forEach(button => button.addEventListener('click',() => {
                    const row = result.content[Number(button.dataset.row)];
                    if ($('Group').value === 'area') { $('Visao').value = 'area'; configureView(); $('Area').value = row.id; $('Group').value = 'pessoa'; }
                    else { $('Visao').value = $('Perfil').value === 'colaborador' ? 'self' : 'pessoa'; configureView(); $('Pessoa').value = row.id; }
                    $('Search').value = ''; loadData();
                }));
            }
            $('PageInfo').textContent = fmt(result.totalElements) + ' resultados · Página ' + (result.page + 1) + ' de ' + Math.max(1,Math.ceil(result.totalElements/result.size));
            $('Prev').disabled = state.page === 0; $('Next').disabled = (state.page+1)*10 >= result.totalElements;
        } catch(e) { if (epoch === state.epoch) failure($('TableStatus'), e, loadTable); }
    }
    $('Perfil').addEventListener('change',() => { if (state.users.length) loadFilters(); });
    $('Usuario').addEventListener('change',loadFilters);
    $('Empresa').addEventListener('change',() => { configureUsers(); loadFilters(); });
    $('Visao').addEventListener('change',() => { configureView(); $('Search').value = ''; loadData(); });
    for (const id of ['Area','Pessoa','Periodo','Ferramenta']) $(id).addEventListener('change',loadData);
    for (const id of ['Metrica','Modo']) $(id).addEventListener('change',renderCharts);
    for (const id of ['Group','Order']) $(id).addEventListener('change',() => { state.page = 0; loadTable(); });
    $('Search').addEventListener('input',() => { clearTimeout(searchTimer); abort('table'); state.page=0; searchTimer=setTimeout(loadTable,300); });
    $('Prev').addEventListener('click',() => { state.page--; loadTable(); });
    $('Next').addEventListener('click',() => { state.page++; loadTable(); });
    $('Reset').addEventListener('click',loadFilters);
    init();
})();
