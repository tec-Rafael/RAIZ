/* Personagem do usuário no topo do dashboard, com falas montadas a partir dos dados AMS que a página já carregou
   e um campo de pergunta sempre visível, respondido pelo assistente (Java → agentes de IA em Python → Ollama).
   dashboard.js avisa a cada renderização com o evento "raiz:dashboard" (dados, pessoa, perfil, usuário e filtros). */
(() => {
    'use strict';
    const A = window.RaizAvatar, caixa = document.getElementById('rdHello');
    if (!A || !caixa) return;
    const canvas = document.getElementById('rdAvatar'), ctx = canvas.getContext('2d');
    const fala = document.getElementById('rdFala'), pontos = document.getElementById('rdFalaPontos'), proxima = document.getElementById('rdFalaProxima');
    const balao = caixa.querySelector('.rdBubble'), form = document.getElementById('rdPergunta');
    const campo = document.getElementById('rdPerguntaTexto'), enviar = document.getElementById('rdPerguntaEnviar');
    const primeiroNome = (caixa.dataset.nome || '').trim().split(/\s+/)[0] || 'você';
    const reduzir = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const fmt = n => new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(n);
    const nivel = n => n >= 85 ? 'Excelente' : n >= 70 ? 'Alto' : n >= 60 ? 'Médio' : 'Baixo';
    const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    const forte = v => '<strong>' + esc(v) + '</strong>';

    // ------------------------------------------------------------ personagem e poses
    // A pose vem de canvas.dataset.pose (o dashModulos.js muda ao arrastar ou soltar em um card):
    //   parado (padrão), andar (dataset.dir = 1/-1), pendurado (dataset.angulo em graus) e, em cima de um card,
    //   sentado, deitado (de lado, apoiado no braço) ou cruzado (em pé, de braços cruzados).
    // Enquanto o assistente pensa, a pose parada vira "pensando", com um balão de pensamento.
    const POSE = {
        parado: { w: 20, h: 38, x: 4, y: 0, base: 0 }, pensando: { w: 20, h: 38, x: 4, y: 0, base: 0 }, andar: { w: 20, h: 38, x: 4, y: 0, base: 0 },
        pendurado: { w: 40, h: 40, base: 0 },        // pivô (onde o ponteiro segura) em (20, 3)
        sentado: { w: 20, h: 35, x: 4, y: 0, base: 5 },   // as canelas (5 linhas) ficam penduradas na frente do card
        deitado: { w: 35, h: 24, x: 0, y: 0, base: 0 },
        cruzado: { w: 20, h: 38, x: 4, y: 0, base: 1 }
    };
    let cfg = null, cacheQuadros = {}, poseAtual = null, escalaAtual = 0, pensandoAgora = false;
    const escala = () => window.matchMedia('(max-width: 700px)').matches ? 3 : 4;
    function quadroDe(e, i) {
        const k = e + i; if (cacheQuadros[k]) return cacheQuadros[k];
        const q = A.quadro(cfg, e, i), c = document.createElement('canvas');
        c.width = q.largura; c.height = q.altura; c.getContext('2d').putImageData(new ImageData(q.dados, q.largura, q.altura), 0, 0);
        return cacheQuadros[k] = c;
    }
    async function carregarAvatar() {
        try {
            const r = await fetch('/api/avatar/me');
            if (!r.ok) return;   // sem personagem, o balão continua sozinho
            const corpo = await r.json();
            cfg = A.validar(corpo.avatar); cacheQuadros = {};
            canvas.hidden = false;
        } catch (e) { /* o dashboard funciona normalmente sem o personagem */ }
    }
    function aplicarPose(p) {
        const P = POSE[p], s = escala();
        canvas.width = P.w; canvas.height = P.h;
        canvas.style.width = P.w * s + 'px'; canvas.style.height = P.h * s + 'px';
        canvas.style.setProperty('--rd-av-w', P.w * s + 'px'); canvas.style.setProperty('--rd-av-h', P.h * s + 'px');
        canvas.style.setProperty('--rd-av-base', P.base * s + 'px');
        poseAtual = p; escalaAtual = s; ctx.imageSmoothingEnabled = false;
    }
    function sombra(x, y, w) { ctx.fillStyle = 'rgba(20,36,20,.16)'; ctx.fillRect(x, y, w, 2); ctx.fillStyle = 'rgba(20,36,20,.26)'; ctx.fillRect(x + 2, y, w - 4, 1); }
    function desenhar(agora) {
        if (cfg) {
            const t = agora / 1000, piscar = !reduzir && (t % 3.4) > 3.25 ? 1 : 0;
            let p = canvas.dataset.pose || 'parado';
            if (p === 'parado' && pensandoAgora) p = 'pensando';
            if (!POSE[p]) p = 'parado';
            if (p !== poseAtual || escala() !== escalaAtual) aplicarPose(p);
            const P = POSE[p];
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            if (p === 'pendurado') {
                const ang = (parseFloat(canvas.dataset.angulo) || 0) * Math.PI / 180;
                ctx.save(); ctx.translate(20, 3); ctx.rotate(ang); ctx.drawImage(quadroDe('parado', piscar), -A.W / 2, -4); ctx.restore();
                if (Math.abs(ang) > .35 && !reduzir) { ctx.fillStyle = '#9fd8f0'; ctx.fillRect(ang > 0 ? 27 : 12, 6, 1, 2); }   // gota de suor
            } else if (p === 'deitado') {
                ctx.drawImage(quadroDe('deitado', (t % 5) > 4.7 && !reduzir ? 1 : 0), 0, 0);
            } else {
                let c;
                if (p === 'andar') c = quadroDe('andar', reduzir ? 0 : Math.floor(t * 8) % 4);
                else if (p === 'sentado') c = quadroDe('sentado', piscar ? 2 : (!reduzir && (t % 1.4) > .7 ? 1 : 0));
                else c = quadroDe(p, piscar);
                if (p !== 'sentado') sombra(2, 35, 16);
                ctx.save();
                if (p === 'andar' && canvas.dataset.dir === '-1') { ctx.translate(P.w, 0); ctx.scale(-1, 1); }
                ctx.drawImage(c, P.x, P.y, P.w, P.h, 0, 0, P.w, P.h); ctx.restore();
            }
        }
        posicionarBaloes();
        requestAnimationFrame(desenhar);
    }
    requestAnimationFrame(desenhar);

    // ------------------------------------------------------------ falas e pensamento perto do personagem
    const falinha = document.createElement('div'), pensamento = document.createElement('div');
    falinha.className = 'rdFalinha'; falinha.hidden = true; falinha.setAttribute('aria-live', 'polite');
    pensamento.className = 'rdPensamento'; pensamento.hidden = true; pensamento.setAttribute('aria-hidden', 'true');
    pensamento.innerHTML = '<span class="rdPensamentoNuvem"><i></i><i></i><i></i></span><span class="rdPensamentoBolha"></span><span class="rdPensamentoBolha rdPequena"></span>';
    document.body.append(falinha, pensamento);
    let falaAte = 0, falaTimer = null;
    function posicionarBaloes() {
        if (falinha.hidden && pensamento.hidden) return;
        const r = canvas.getBoundingClientRect();
        if (!r.width) { falinha.hidden = pensamento.hidden = true; return; }
        const cabecaX = poseAtual === 'deitado' ? r.left + r.width * .22 : r.left + r.width / 2;
        const topo = poseAtual === 'deitado' ? r.top : r.top + (poseAtual === 'pendurado' ? 0 : 4);
        falinha.style.left = Math.max(8, Math.min(window.innerWidth - 8, cabecaX)) + 'px'; falinha.style.top = topo + 'px';
        pensamento.style.left = (cabecaX + r.width * .18) + 'px'; pensamento.style.top = (topo + 6) + 'px';
    }
    window.raizDashFala = {
        falar(texto, ms) {
            clearTimeout(falaTimer); falinha.textContent = texto; falinha.hidden = false; posicionarBaloes();
            falaTimer = setTimeout(() => { falinha.hidden = true; }, ms || 2800);
        },
        calar() { clearTimeout(falaTimer); falinha.hidden = true; }
    };
    function pensar(sim) { pensandoAgora = sim; pensamento.hidden = !sim; if (sim) window.raizDashFala.calar(); posicionarBaloes(); }

    // ------------------------------------------------------------ balão
    let lista = [], atual = 0, timer = null, modo = 'dicas', filtros = {};
    function digitar(html, depois) {
        clearInterval(timer);
        if (reduzir) { fala.innerHTML = html; depois?.(); return; }
        const texto = html.replace(/<[^>]+>/g, '').replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'");
        // O texto inteiro aparece em no máximo ~1 s (60 passos), para a animação não atrasar a resposta.
        let n = 0; fala.textContent = ''; const passo = Math.max(2, Math.ceil(texto.length / 60));
        timer = setInterval(() => { n += passo; if (n >= texto.length) { clearInterval(timer); fala.innerHTML = html; depois?.(); return; } fala.textContent = texto.slice(0, n); }, 16);
    }
    function mostrar(i) {
        modo = 'dicas'; atual = i; balao.classList.remove('rdResposta');
        pontos.hidden = false; proxima.textContent = 'Próxima';
        pontos.replaceChildren(...lista.map((_, k) => { const s = document.createElement('span'); if (k === i) s.className = 'on'; return s; }));
        proxima.hidden = lista.length < 2;
        digitar(lista[i] || '');
    }
    proxima.addEventListener('click', () => {
        if (modo === 'pensando') return;
        if (modo === 'resposta') { mostrar(0); return; }
        if (lista.length) mostrar((atual + 1) % lista.length);
    });
    setInterval(() => { if (modo === 'dicas' && lista.length > 1 && !caixa.matches(':hover') && !caixa.contains(document.activeElement)) mostrar((atual + 1) % lista.length); }, 10000);

    // ------------------------------------------------------------ perguntas ao assistente
    const placeholders = { COLABORADOR: 'Pergunte sobre seus dados…', GESTOR: 'Pergunte sobre sua equipe ou seus dados…', ADMINISTRADOR: 'Pergunte sobre o recorte selecionado…' };
    campo.placeholder = placeholders[caixa.dataset.perfil] || placeholders.COLABORADOR;
    // Texto do modelo → HTML seguro: escapa tudo e só reconhece **negrito**.
    const formatar = t => esc(t).replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    const nomesFontes = f => f.startsWith('metodologia') ? 'metodologia do AMS' : f.startsWith('politica:') ? 'política de IA da ' + f.slice(9) : f === 'dados' ? 'dados de uso' : f === 'perfil' ? 'perfil de uso (machine learning)' : f;
    function emResposta(conteudo, fontes) {
        modo = 'resposta'; balao.classList.add('rdResposta');
        pontos.hidden = true; proxima.hidden = false; proxima.textContent = '‹ Voltar às dicas';
        const unicas = [...new Set((fontes || []).map(nomesFontes))];
        digitar(conteudo, () => {
            if (unicas.length) { const nota = document.createElement('span'); nota.className = 'rdBubbleFontes'; nota.textContent = 'Com base em: ' + unicas.join(', ') + '.'; fala.append(document.createElement('br'), nota); }
            fala.scrollTop = 0;
        });
    }
    form.addEventListener('submit', async ev => {
        ev.preventDefault();
        const pergunta = campo.value.trim();
        if (pergunta.length < 2 || modo === 'pensando') return;
        modo = 'pensando'; clearInterval(timer); campo.disabled = true; enviar.disabled = true; pensar(true);
        balao.classList.remove('rdResposta'); pontos.hidden = true; proxima.hidden = true;
        fala.innerHTML = 'Analisando seus dados <span class="rdPensando" aria-hidden="true"><span></span><span></span><span></span></span>';
        const f = filtros || {};
        try {
            const r = await fetch('/api/assistente/perguntar', { method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ pergunta, periodo: Number(f.periodo) || 5, companyId: f.companyId || null, gestorId: f.gestorId || null,
                                       departmentId: f.departmentId || null, employeeId: f.employeeId || null }) });
            if (r.status === 401) { window.location.assign('/login'); return; }
            const corpo = await r.json().catch(() => null);
            if (!r.ok) throw new Error(corpo?.detail || corpo?.message || 'Não consegui responder agora. Tente novamente em instantes.');
            campo.value = '';
            emResposta(formatar(corpo.resposta || 'Não encontrei uma resposta para essa pergunta.'), corpo.fontes);
        } catch (erro) {
            emResposta(esc(erro.message || 'Não consegui responder agora.'), []);
        } finally {
            pensar(false);
            campo.disabled = false; enviar.disabled = false; campo.focus();
        }
    });

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
        filtros = e.detail.filtros || {};
        try { lista = falas(e.detail); } catch (erro) { lista = ['Oi, ' + forte(primeiroNome) + '! Bem-vindo(a) ao seu dashboard.']; }
        if (modo === 'dicas') mostrar(0);   // uma resposta aberta continua visível; as dicas novas aparecem ao voltar
    });
    lista = ['Oi, ' + forte(primeiroNome) + '! Estou buscando seus dados…'];
    caixa.hidden = false; mostrar(0);
    carregarAvatar();
})();
