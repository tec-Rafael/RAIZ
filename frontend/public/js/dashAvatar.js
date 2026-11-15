/* Personagem do usuário no topo do dashboard, com falas montadas a partir dos dados AMS que a página já carregou.
   dashboard.js avisa a cada renderização com o evento "raiz:dashboard" (dados, pessoa selecionada, perfil e usuário). */
(() => {
    'use strict';
    const A = window.RaizAvatar, caixa = document.getElementById('rdHello');
    if (!A || !caixa) return;
    const canvas = document.getElementById('rdAvatar'), ctx = canvas.getContext('2d');
    const fala = document.getElementById('rdFala'), pontos = document.getElementById('rdFalaPontos'), proxima = document.getElementById('rdFalaProxima');
    const primeiroNome = (caixa.dataset.nome || '').trim().split(/\s+/)[0] || 'você';
    const reduzir = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const fmt = n => new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(n);
    const nivel = n => n >= 85 ? 'Excelente' : n >= 70 ? 'Alto' : n >= 60 ? 'Médio' : 'Baixo';
    const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    const forte = v => '<strong>' + esc(v) + '</strong>';

    // ------------------------------------------------------------ personagem (parado, de frente, piscando)
    let quadros = [];
    async function carregarAvatar() {
        try {
            const r = await fetch('/api/avatar/me');
            if (!r.ok) return;   // sem personagem, o balão continua sozinho
            const corpo = await r.json();
            const cfg = A.validar(corpo.avatar);
            quadros = [0, 1].map(i => {
                const q = A.quadro(cfg, 'parado', i), c = document.createElement('canvas');
                c.width = A.W; c.height = A.H; c.getContext('2d').putImageData(new ImageData(q.dados, A.W, A.H), 0, 0);
                return c;
            });
            canvas.hidden = false;
        } catch (e) { /* o dashboard funciona normalmente sem o personagem */ }
    }
    function desenhar(agora) {
        if (quadros.length) {
            const t = agora / 1000, x0 = (A.W - 20) / 2;
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            ctx.fillStyle = 'rgba(20,36,20,.16)'; ctx.fillRect(2, 35, 16, 2);
            ctx.fillStyle = 'rgba(20,36,20,.26)'; ctx.fillRect(4, 35, 12, 1);
            ctx.drawImage(quadros[!reduzir && (t % 3.4) > 3.25 ? 1 : 0], x0, 0, 20, A.H, 0, 0, 20, A.H);
        }
        requestAnimationFrame(desenhar);
    }
    requestAnimationFrame(desenhar);

    // ------------------------------------------------------------ balão
    let lista = [], atual = 0, timer = null;
    function mostrar(i) {
        atual = i; clearInterval(timer);
        pontos.replaceChildren(...lista.map((_, k) => { const s = document.createElement('span'); if (k === i) s.className = 'on'; return s; }));
        proxima.hidden = lista.length < 2;
        const html = lista[i] || '';
        if (reduzir) { fala.innerHTML = html; return; }
        const texto = html.replace(/<[^>]+>/g, '').replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'");
        let n = 0; fala.textContent = '';
        timer = setInterval(() => { n += 2; if (n >= texto.length) { clearInterval(timer); fala.innerHTML = html; return; } fala.textContent = texto.slice(0, n); }, 22);
    }
    proxima.addEventListener('click', () => { if (lista.length) mostrar((atual + 1) % lista.length); });
    setInterval(() => { if (lista.length > 1 && !caixa.matches(':hover') && !caixa.contains(document.activeElement)) mostrar((atual + 1) % lista.length); }, 10000);

    function variacao(nota, anterior, periodo, sujeito) {
        if (nota == null || anterior == null) return null;
        const diff = nota - anterior, ref = 'P' + (periodo - 1);
        if (Math.abs(diff) < 0.005) return sujeito.manteve + ' a mesma nota de ' + ref + '.';
        return diff > 0 ? sujeito.subiu + ' ' + forte(fmt(diff) + ' pontos') + ' em relação a ' + ref + '. Continue assim!'
            : sujeito.caiu + ' ' + forte(fmt(-diff) + ' pontos') + ' em relação a ' + ref + '. Vamos recuperar?';
    }
    function menorDimensao(dimensoes, chave) {
        return (dimensoes || []).filter(x => x[chave] != null && (chave !== 'utilizada' || x.registrada != null))
            .sort((a, b) => a[chave] - b[chave])[0] || null;
    }

    function falas({ dados: d, pessoa, perfil, usuario }) {
        const r = d.resumo, p = d.periodo, anterior = p.id > 1 ? d.evolucao[p.id - 2]?.resumo?.nota : null;
        const propria = perfil === 'colaborador' || (pessoa && usuario && pessoa.id === usuario.id);
        const linhas = [];
        if (propria) {
            linhas.push(r.nota == null ? 'Oi, ' + forte(primeiroNome) + '! Seu AMS ainda não está disponível em P' + p.id + '.'
                : 'Oi, ' + forte(primeiroNome) + '! Seu AMS em P' + p.id + ' é ' + forte(fmt(r.nota)) + ': nível ' + nivel(r.nota) + '.' + (r.nota >= 85 ? ' Sua árvore agradece!' : ''));
            linhas.push(variacao(r.nota, anterior, p.id, { subiu: 'Você subiu', caiu: 'Seu AMS caiu', manteve: 'Você manteve' }));
            const fraca = menorDimensao(d.pessoal?.dimensoes, 'utilizada');
            if (fraca) linhas.push('Sua dimensão com mais espaço para evoluir é ' + forte(fraca.nome) + ' (' + fmt(fraca.utilizada) + ').');
            linhas.push('Abra ' + forte('Como esta nota foi calculada') + ' para ver o peso de cada dimensão.');
        } else if (pessoa) {
            linhas.push('Você está vendo ' + forte(pessoa.nome) + (r.nota == null ? ': AMS indisponível em P' + p.id + '.' : ': AMS ' + forte(fmt(r.nota)) + ' em P' + p.id + ', nível ' + nivel(r.nota) + '.'));
            linhas.push(variacao(r.nota, anterior, p.id, { subiu: 'Subiu', caiu: 'Caiu', manteve: 'Manteve' }));
            const fraca = menorDimensao(d.pessoal?.dimensoes, 'utilizada');
            if (fraca) linhas.push('A dimensão com mais espaço para evoluir é ' + forte(fraca.nome) + ' (' + fmt(fraca.utilizada) + ').');
        } else {
            const quem = perfil === 'gestor' ? 'da sua equipe' : 'do recorte selecionado';
            const saudacao = perfil === 'gestor' ? 'Oi, ' : 'Olá, ';
            linhas.push(r.nota == null ? saudacao + forte(primeiroNome) + '! O AMS ' + quem + ' ainda não está disponível em P' + p.id + '.'
                : saudacao + forte(primeiroNome) + '! O AMS médio ' + quem + ' em P' + p.id + ' é ' + forte(fmt(r.nota)) + ' (' + r.validos + ' de ' + r.pessoas + ' pessoas com nota): nível ' + nivel(r.nota) + '.');
            linhas.push(variacao(r.nota, anterior, p.id, { subiu: 'A média subiu', caiu: 'A média caiu', manteve: 'A média manteve' }));
            const fraca = menorDimensao(r.dimensoes, 'nota');
            if (fraca) linhas.push('A dimensão com menor média agora é ' + forte(fraca.nome) + ' (' + fmt(fraca.nota) + '). Vale começar por ela.');
            linhas.push(perfil === 'gestor' ? 'Em ' + forte('Equipe em foco') + ' você vê quem se destacou em cada período.'
                : 'Use ' + forte('Compare empresas e áreas') + ' para ver onde a maturidade em IA mais precisa de apoio.');
        }
        return linhas.filter(Boolean);
    }

    document.addEventListener('raiz:dashboard', e => {
        try { lista = falas(e.detail); } catch (erro) { lista = ['Oi, ' + forte(primeiroNome) + '! Bem-vindo(a) ao seu dashboard.']; }
        mostrar(0);
    });
    lista = ['Oi, ' + forte(primeiroNome) + '! Estou buscando seus dados…'];
    caixa.hidden = false; mostrar(0);
    carregarAvatar();
})();
