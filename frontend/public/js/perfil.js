(() => {
    'use strict';
    const $ = id => document.getElementById('perfil' + id);
    const periodo = $('Periodo'), status = $('Status'), cards = $('Consumo');
    const fmt = n => new Intl.NumberFormat('pt-BR').format(n);
    const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const share = (n,total) => total > 0 ? Math.min(100,Math.max(0,100 * n / total)) : 0;
    const percent = n => n > 0 && n < 0.1 ? '<0,1%' : new Intl.NumberFormat('pt-BR',{maximumFractionDigits:1}).format(n) + '%';
    const icon = nome => /gpt|openai/i.test(nome) ? 'bi-openai' : 'bi-cpu';
    const tone = nome => /claude/i.test(nome) ? 'clay' : /gemini|copilot/i.test(nome) ? 'blue' : /perplexity|llama/i.test(nome) ? 'plum' : 'green';
    let controller, periodosCarregados = false;

    function card(ia,total,destaque) {
        const participacao = share(ia.total,total);
        return '<article class="perfilAiCard' + (destaque ? ' perfilAiFeatured' : '') + '" data-tone="' + tone(ia.nome) + '">' +
            '<div class="perfilAiCardHead"><span class="perfilAiIcon"><i class="bi ' + icon(ia.nome) + '" aria-hidden="true"></i></span><div><h3>' + esc(ia.nome) + '</h3><p>' + esc(percent(participacao)) + ' do seu consumo</p></div>' + (destaque ? '<span class="perfilAiHighlight">Mais utilizada</span>' : '') + '</div>' +
            '<strong class="perfilAiAmount">' + fmt(ia.total) + '</strong><span class="perfilAiAmountLabel">tokens no período</span>' +
            '<div class="perfilAiShare" aria-hidden="true"><span style="width:' + participacao.toFixed(3) + '%"></span></div>' +
            '<dl class="perfilAiSplit"><div><dt><i class="bi bi-arrow-up-right" aria-hidden="true"></i> Entrada</dt><dd>' + fmt(ia.entrada) + '</dd></div><div><dt><i class="bi bi-arrow-down-left" aria-hidden="true"></i> Saída</dt><dd>' + fmt(ia.saida) + '</dd></div></dl></article>';
    }
    function render(dados) {
        const usadas = dados.ferramentas.filter(ia => ia.total > 0).sort((a,b) => b.total - a.total || a.nome.localeCompare(b.nome,'pt-BR'));
        const semUso = dados.ferramentas.filter(ia => ia.total === 0);
        $('Total').textContent = fmt(dados.total);
        $('Principal').textContent = usadas[0]?.nome || 'Sem consumo';
        $('PrincipalDetail').textContent = usadas.length ? percent(share(usadas[0].total,dados.total)) + ' do consumo total' : 'Nenhuma ferramenta utilizada';
        $('Ativas').textContent = String(usadas.length).padStart(2,'0');
        $('AtivasDetail').textContent = 'de ' + dados.ferramentas.length + ' ferramentas disponíveis';
        status.textContent = usadas.length ? 'Seu consumo, organizado da ferramenta mais utilizada para a menos utilizada.' : 'Não há consumo de IA registrado neste período.';
        cards.innerHTML = usadas.length ? usadas.map((ia,i) => card(ia,dados.total,i === 0)).join('') : '<div class="perfilEmpty"><i class="bi bi-flower1" aria-hidden="true"></i>Um novo espaço para acompanhar seu uso de IA.<br>Selecione outro período para explorar seu histórico.</div>';
        $('SemUso').hidden = !semUso.length;
        $('SemUsoTitle').textContent = 'Ferramentas sem consumo neste período (' + semUso.length + ')';
        $('SemUsoItens').innerHTML = semUso.map(ia => '<div class="perfilUnusedItem"><i class="bi ' + icon(ia.nome) + '" aria-hidden="true"></i><span>' + esc(ia.nome) + '</span><small>0 tokens</small></div>').join('');
    }
    async function carregar() {
        controller?.abort();
        const requisicao = new AbortController(); controller = requisicao;
        status.textContent = 'Consultando seu consumo de IA…';
        $('Retry').hidden = true; $('SemUso').hidden = true; $('SemUso').open = false;
        for (const id of ['Total','Principal','Ativas']) $(id).textContent = '—';
        $('PrincipalDetail').textContent = 'Participação no consumo total';
        $('AtivasDetail').textContent = 'Com consumo no período';
        cards.setAttribute('aria-busy','true');
        cards.innerHTML = '<div class="perfilAiSkeleton" aria-hidden="true"></div><div class="perfilAiSkeleton" aria-hidden="true"></div>';
        try {
            const resposta = await fetch('/api/funcionario/me/consumo-ia?periodo=' + encodeURIComponent(periodo.value), {signal:requisicao.signal});
            if (resposta.status === 401) return window.location.assign('/login');
            if (!resposta.ok) throw new Error('Não foi possível consultar o consumo de IA. Tente novamente.');
            const dados = await resposta.json();
            if (controller !== requisicao) return;
            if (!periodosCarregados) {
                const selecionado = periodo.value;
                periodo.replaceChildren(...dados.periodos.map(p => new Option('P' + p.id + ' · ' + p.dias + ' dias',p.id)),new Option('Todo o histórico','historico'));
                periodo.value = dados.periodos.length ? selecionado : 'historico';
                periodosCarregados = true;
            }
            render(dados);
        } catch (erro) {
            if (erro.name !== 'AbortError' && controller === requisicao) {
                status.textContent = erro.message; cards.replaceChildren(); $('Retry').hidden = false;
            }
        } finally { if (controller === requisicao) cards.setAttribute('aria-busy','false'); }
    }
    periodo.addEventListener('change',carregar);
    $('Retry').addEventListener('click',carregar);
    $('Sair').addEventListener('click', async evento => {
        const botao = evento.currentTarget; botao.disabled = true;
        $('SairStatus').hidden = true;
        try {
            const resposta = await fetch('/api/funcionario/logout',{method:'POST'});
            if (!resposta.ok) throw new Error('Não foi possível encerrar a sessão. Tente novamente.');
            window.location.assign('/login');
        } catch (erro) { $('SairStatus').textContent = erro.message; $('SairStatus').hidden = false; botao.disabled = false; }
    });
    carregar();
})();
