/* Dashboard modular: cada card ([data-modulo]) pode ser arrastado pela alça e trocar de lugar com outro,
   e o personagem (#rdAvatar) pode ser arrastado para ficar em cima de qualquer card.
   A ordem é feita com a propriedade CSS "order" do grid de #rdContent, então os elementos não saem do lugar
   no HTML e o dashboard.js continua encontrando tudo pelos mesmos ids.
   O layout fica salvo só neste navegador (localStorage). Ao enviar uma pergunta, o personagem volta para o lado da caixa. */
(() => {
    'use strict';
    const conteudo = document.getElementById('rdContent');
    if (!conteudo) return;
    const reduzir = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const hash = s => { let h = 0; for (const c of s) h = (h * 31 + c.charCodeAt(0)) | 0; return (h >>> 0).toString(36); };
    const chave = 'raiz.dashboard.layout.v1.' + hash(conteudo.dataset.usuario || '');
    const ler = () => { try { return JSON.parse(localStorage.getItem(chave)) || {}; } catch (e) { return {}; } };
    const gravar = () => { try { localStorage.setItem(chave, JSON.stringify(estado)); } catch (e) { /* sem armazenamento: vale só nesta visita */ } };
    const modulos = [...conteudo.querySelectorAll('[data-modulo]')];
    const padrao = modulos.map(m => m.dataset.modulo);
    const porId = id => modulos.find(m => m.dataset.modulo === id);
    const visivel = m => m.offsetParent !== null;
    const restaurar = document.getElementById('rdLayoutPadrao');
    let estado = ler();

    // ------------------------------------------------------------ ordem dos cards
    function aplicar(ordem) {
        const lista = Array.isArray(ordem) ? ordem.filter(id => padrao.includes(id)) : [];
        padrao.forEach(id => { if (!lista.includes(id)) lista.push(id); });
        lista.forEach((id, i) => { porId(id).style.order = i; });
    }
    const ordemAtual = () => modulos.slice().sort((a, b) => a.style.order - b.style.order).map(m => m.dataset.modulo);
    function salvar() {
        estado.ordem = ordemAtual();
        if (estado.ordem.join() === padrao.join()) delete estado.ordem;
        gravar();
        if (restaurar) restaurar.hidden = !estado.ordem && !estado.avatar;
    }
    function trocar(a, b) { const o = a.style.order; a.style.order = b.style.order; b.style.order = o; }

    const titulo = m => (m.querySelector('h2, summary')?.textContent || 'Resumo AMS').trim();
    const GRIP = '<svg viewBox="0 0 10 16" width="10" height="16" aria-hidden="true"><g fill="currentColor"><circle cx="2" cy="2" r="1.6"/><circle cx="8" cy="2" r="1.6"/><circle cx="2" cy="8" r="1.6"/><circle cx="8" cy="8" r="1.6"/><circle cx="2" cy="14" r="1.6"/><circle cx="8" cy="14" r="1.6"/></g></svg>';
    let arrasto = null;

    function retanguloSemTransform(m) { const t = m.style.transform; m.style.transform = ''; const r = m.getBoundingClientRect(); m.style.transform = t; return r; }
    function posicionar() {
        const a = arrasto; if (!a) return;
        a.base = retanguloSemTransform(a.m);
        a.m.style.transform = 'translate(' + (a.x - a.gx - a.base.left) + 'px,' + (a.y - a.gy - a.base.top) + 'px)';
    }
    function procurarAlvo() {
        const a = arrasto, alvo = document.elementsFromPoint(a.x, a.y).map(el => el.closest('#rdContent [data-modulo]')).find(m => m && m !== a.m);
        if (!alvo) { a.ultimo = null; return; }
        if (alvo === a.ultimo) return;          // evita trocar duas vezes com o mesmo card enquanto o ponteiro está nele
        trocar(a.m, alvo); a.ultimo = alvo; posicionar();
    }
    function rolar() {                          // perto da borda da janela, a página rola sozinha durante o arrasto
        if (!arrasto) return;
        const borda = 70, h = window.innerHeight;
        const v = arrasto.y < borda ? -(borda - arrasto.y) / 4 : arrasto.y > h - borda ? (arrasto.y - (h - borda)) / 4 : 0;
        if (v) { window.scrollBy(0, v); posicionar(); procurarAlvo(); }
        arrasto.raf = requestAnimationFrame(rolar);
    }

    modulos.forEach(m => {
        const alca = document.createElement('button');
        alca.type = 'button'; alca.className = 'rdMover'; alca.innerHTML = GRIP;
        alca.title = 'Arraste para mudar o card de lugar (ou use as setas do teclado)';
        alca.setAttribute('aria-label', 'Mover o card ' + titulo(m));
        m.appendChild(alca);
        alca.addEventListener('pointerdown', e => {
            if (e.button !== 0) return;
            e.preventDefault();
            const r = m.getBoundingClientRect();
            arrasto = { m, alca, gx: e.clientX - r.left, gy: e.clientY - r.top, x: e.clientX, y: e.clientY, ultimo: null };
            alca.setPointerCapture(e.pointerId);
            m.classList.add('rdArrastando'); document.body.classList.add('rdOrganizando');
            posicionar(); arrasto.raf = requestAnimationFrame(rolar);
        });
        alca.addEventListener('pointermove', e => {
            if (!arrasto || arrasto.alca !== alca) return;
            arrasto.x = e.clientX; arrasto.y = e.clientY; posicionar(); procurarAlvo();
        });
        const soltar = () => {
            if (!arrasto || arrasto.alca !== alca) return;
            cancelAnimationFrame(arrasto.raf);
            m.classList.remove('rdArrastando'); document.body.classList.remove('rdOrganizando');
            m.style.transform = ''; arrasto = null; salvar();
        };
        alca.addEventListener('pointerup', soltar);
        alca.addEventListener('pointercancel', soltar);
        alca.addEventListener('click', e => e.preventDefault());
        alca.addEventListener('keydown', e => {
            const passo = { ArrowUp: -1, ArrowLeft: -1, ArrowDown: 1, ArrowRight: 1 }[e.key];
            if (!passo) return;
            e.preventDefault();
            const lista = modulos.filter(visivel).sort((a, b) => a.style.order - b.style.order);
            const vizinho = lista[lista.indexOf(m) + passo];
            if (vizinho) { trocar(m, vizinho); salvar(); alca.focus(); m.scrollIntoView({ block: 'nearest', behavior: reduzir ? 'auto' : 'smooth' }); }
        });
    });

    // ------------------------------------------------------------ personagem
    // Segurado: fica pendurado pelo ponto onde o ponteiro pega (topo da cabeça) e balança como um pêndulo.
    // Solto em um card: sorteia uma pose (sentado na borda, deitado de lado ou em pé de braços cruzados). Fora de um card: reclama e volta andando.
    const av = document.getElementById('rdAvatar'), casa = document.getElementById('rdHello');
    const RECLAMA = ['Ai! Isso doeu!', 'Ei, mais cuidado!', 'Ui, minhas costas…', 'Precisava disso?', 'Que tombo!', 'Me põe no chão devagar!'];
    const PASSEIO = ['Voltando para o meu cantinho.', 'Lá-lá-lá…', 'Um passo de cada vez!', 'Que dashboard bonito!', 'Bora evoluir hoje?'];
    const sorteio = l => l[Math.floor(Math.random() * l.length)];
    const falar = (texto, ms) => window.raizDashFala?.falar(texto, ms);
    const escala = () => window.matchMedia('(max-width: 700px)').matches ? 3 : 4;
    let solto = null, caminhada = null;
    const POSES_CARD = ['sentado', 'deitado', 'cruzado'];
    // Ao ser colocado em um card, ele comenta o que aquele card mostra, com os dados que o dashboard acabou de carregar
    // (evento "raiz:dashboard": os próprios dados do colaborador, a equipe do gestor ou o recorte do administrador).
    let painel = null;
    document.addEventListener('raiz:dashboard', e => { painel = e.detail || null; });
    const num = n => new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(n);
    const nivelDe = n => n >= 85 ? 'Excelente' : n >= 70 ? 'Alto' : n >= 60 ? 'Médio' : 'Baixo';
    const primeiro = nome => String(nome || '').trim().split(/\s+/)[0];
    function fraseDoCard(id) {
        const d = painel?.dados; if (!d || !d.resumo || !d.periodo) return null;
        const r = d.resumo, p = d.periodo.id, perfil = painel.perfil, pessoa = painel.pessoa, usuario = painel.usuario;
        const propria = perfil === 'colaborador' || (pessoa && usuario && pessoa.id === usuario.id);
        const quem = propria ? 'Seu AMS' : pessoa ? 'O AMS de ' + primeiro(pessoa.nome) : perfil === 'gestor' ? 'O AMS médio da sua equipe' : 'O AMS médio do recorte';
        const notas = (d.evolucao || []).map(x => x?.resumo?.nota ?? null);
        try {
            switch (id) {
                case 'kpis':
                    return r.nota == null ? 'Ainda não há AMS em P' + p + '. Assim que houver dados, eu conto!' : quem + ' em P' + p + ' é ' + num(r.nota) + ': nível ' + nivelDe(r.nota) + '.';
                case 'dimensoes': {
                    const indiv = propria || pessoa, chave = indiv ? 'utilizada' : 'nota';
                    const ds = ((indiv ? d.pessoal?.dimensoes : r.dimensoes) || []).filter(x => x[chave] != null && (chave !== 'utilizada' || x.registrada != null));
                    if (!ds.length) return 'Ainda não há notas por dimensão neste período.';
                    const ord = ds.slice().sort((a, b) => b[chave] - a[chave]), alta = ord[0], baixa = ord[ord.length - 1];
                    return ord.length === 1 ? alta.nome + ' está em ' + num(alta[chave]) + '.'
                        : 'O ponto forte é ' + alta.nome + ' (' + num(alta[chave]) + '). Dá para crescer em ' + baixa.nome + ' (' + num(baixa[chave]) + ').';
                }
                case 'trajetoria': {
                    const atual = notas[p - 1], ant = notas[p - 2], ini = notas.find(x => x != null);
                    if (atual == null) return 'Sem nota em P' + p + ', então a linha tem uma lacuna aqui.';
                    if (ant == null) return quem + ' começou a série com ' + num(atual) + '.';
                    const dif = atual - ant, tot = ini != null ? atual - ini : null;
                    return (Math.abs(dif) < .005 ? 'De P' + (p - 1) + ' para P' + p + ' a nota ficou igual.' : 'De P' + (p - 1) + ' para P' + p + ' foram ' + (dif > 0 ? '+' : '') + num(dif) + ' pontos.')
                        + (tot != null && p > 2 ? ' Desde o início: ' + (tot > 0 ? '+' : '') + num(tot) + '.' : '');
                }
                case 'distribuicao': {
                    if (propria || pessoa) return r.nota == null ? 'Sem nota neste período, então fico na faixa "indisponível".' : 'Essa nota cai na faixa ' + nivelDe(r.nota) + '.';
                    const fx = (d.distribuicao || []).filter(x => x.quantidade > 0).sort((a, b) => b.quantidade - a.quantidade)[0];
                    return fx ? 'A faixa com mais gente é "' + fx.nome + '": ' + fx.quantidade + (fx.quantidade === 1 ? ' pessoa.' : ' pessoas.') : null;
                }
                case 'lideres':
                    return r.melhor?.nome ? 'Destaque de P' + p + ': ' + primeiro(r.melhor.nome) + ', com ' + num(r.melhor.nota) + '.' : 'Ninguém se destacou em P' + p + ' ainda.';
                case 'destaque': {
                    const ev = (d.evolucaoDestaque || []).map(x => x?.resumo?.nota ?? null), ini = ev.find(x => x != null), fim = ev[p - 1];
                    if (!r.melhor?.nome || fim == null) return null;
                    return ini != null && ini !== fim ? primeiro(r.melhor.nome) + ' saiu de ' + num(ini) + ' para ' + num(fim) + '.' : primeiro(r.melhor.nome) + ' está com ' + num(fim) + ' em P' + p + '.';
                }
                case 'comparacao': {
                    const gs = (d.areas || d.empresas || []).map(g => ({ nome: g.nome, nota: g.evolucao?.[p - 1]?.nota ?? g.evolucao?.[p - 1]?.resumo?.nota ?? null })).filter(g => g.nota != null).sort((a, b) => b.nota - a.nota);
                    return gs.length ? gs[0].nome + ' lidera com ' + num(gs[0].nota) + (gs.length > 1 ? '; ' + gs[gs.length - 1].nome + ' tem a menor nota (' + num(gs[gs.length - 1].nota) + ').' : '.') : null;
                }
                case 'pessoas':
                    return propria ? 'Aqui ficam suas notas, uma por uma.' : r.validos + ' de ' + r.pessoas + ' pessoas têm AMS em P' + p + '.';
                case 'uso':
                    return 'Abra aqui para ver o uso de IA e as entregas do período.';
                case 'metodologia':
                    return 'Sabia? Cada curso concluído na árvore soma +5 pontos à AI Literacy.';
            }
        } catch (erro) { return null; }
        return null;
    }
    let ultimaPose = null;
    function voltar(animar, motivo) {
        if (!av || !casa) return;
        caminhada?.cancel(); caminhada = null;
        const r0 = av.getBoundingClientRect(), estava = av.parentElement !== casa;
        av.classList.remove('rdAvatarSolto', 'rdAvatarArrastando'); av.style.left = av.style.top = ''; av.style.removeProperty('--rd-av-frac');
        delete av.dataset.angulo; delete av.dataset.pose;
        casa.appendChild(av);
        if (motivo === 'jogado') falar(sorteio(RECLAMA), 2600);
        if (animar && estava && !reduzir && r0.width) {
            const r1 = av.getBoundingClientRect(), dx = r0.left + r0.width / 2 - (r1.left + r1.width / 2), dy = r0.bottom - r1.bottom;
            const ms = Math.max(500, Math.min(1600, Math.hypot(dx, dy) / .35));
            av.dataset.pose = 'andar'; av.dataset.dir = dx > 0 ? '-1' : '1';
            if (motivo === 'restaurar') falar(sorteio(PASSEIO), 2400);
            caminhada = av.animate([{ transform: 'translate(' + dx + 'px,' + dy + 'px)' }, { transform: 'none' }], { duration: ms, easing: 'linear' });
            caminhada.onfinish = () => { caminhada = null; if (av.dataset.pose === 'andar') delete av.dataset.pose; };
        }
        if (estado.avatar) { delete estado.avatar; salvar(); }
    }
    function colocar(m, frac, pose) {
        caminhada?.cancel(); caminhada = null;
        av.classList.remove('rdAvatarArrastando'); av.classList.add('rdAvatarSolto'); av.style.top = av.style.left = '';
        if (!POSES_CARD.includes(pose)) { pose = sorteio(POSES_CARD.filter(p => p !== ultimaPose)); }   // sorteada, sem repetir a anterior
        ultimaPose = pose;
        delete av.dataset.angulo; av.dataset.pose = pose;
        av.style.setProperty('--rd-av-frac', frac.toFixed(3));   // o CSS calcula o left e mantém o personagem dentro do card
        m.appendChild(av);
        estado.avatar = { modulo: m.dataset.modulo, frac: Math.round(frac * 1000) / 1000, pose }; salvar();
    }
    function comentar(m) { const f = fraseDoCard(m.dataset.modulo); if (f) falar(f, 5000); }
    // pêndulo: o personagem balança conforme a aceleração do ponteiro
    function balancar(agora) {
        const s = solto; if (!s || !s.moveu) return;
        const dt = Math.min(.033, (agora - (s.t || agora)) / 1000) || .016; s.t = agora;
        const vx = (s.x - s.xAnt) / dt; const ax = (vx - s.vx) / dt; s.xAnt = s.x; s.vx = vx;
        const L = 18 * escala(), g = 2400;
        s.w += (-(g / L) * Math.sin(s.ang) - 4 * s.w - (Math.max(-6000, Math.min(6000, ax)) / L) * Math.cos(s.ang)) * dt;
        s.ang = Math.max(-1.1, Math.min(1.1, s.ang + s.w * dt));
        av.dataset.angulo = (s.ang * 180 / Math.PI).toFixed(1);
        s.raf = requestAnimationFrame(balancar);
    }
    function seguir() {
        const s = escala();
        av.style.left = (solto.x - 20 * s) + 'px'; av.style.top = (solto.y - 3 * s) + 'px';   // o pivô (20, 3) fica sob o ponteiro
    }
    if (av && casa) {
        av.title = 'Arraste o personagem para cima de um card';
        av.addEventListener('pointerdown', e => {
            if (e.button !== 0) return;
            e.preventDefault();
            solto = { x0: e.clientX, y0: e.clientY, x: e.clientX, y: e.clientY, xAnt: e.clientX, vx: 0, ang: 0, w: 0, moveu: false };
            av.setPointerCapture(e.pointerId);
        });
        av.addEventListener('pointermove', e => {
            if (!solto) return;
            solto.x = e.clientX; solto.y = e.clientY;
            if (!solto.moveu && Math.hypot(e.clientX - solto.x0, e.clientY - solto.y0) < 6) return;   // um clique não tira o personagem do lugar
            if (!solto.moveu) {
                solto.moveu = true; caminhada?.cancel(); caminhada = null;
                av.dataset.pose = 'pendurado'; av.classList.remove('rdAvatarSolto'); av.classList.add('rdAvatarArrastando');
                solto.origem = av.parentElement; document.body.appendChild(av);   // fora dos cards, para ficar por cima de tudo
                try { av.setPointerCapture(e.pointerId); } catch (erro) { /* mover o elemento solta a captura; recaptura */ }
                document.body.classList.add('rdOrganizando'); window.raizDashFala?.calar();
                solto.raf = requestAnimationFrame(balancar);
            }
            seguir();
        });
        const largar = e => {
            if (!solto) return;
            const s = solto; solto = null; cancelAnimationFrame(s.raf); document.body.classList.remove('rdOrganizando');
            if (!s.moveu) return;
            const sc = escala(), ponto = { x: e.clientX, y: e.clientY + 22 * sc };   // o corpo fica pendurado abaixo do ponteiro
            const alvo = document.elementsFromPoint(ponto.x, ponto.y).filter(el => el !== av).map(el => el.closest('#rdContent [data-modulo]')).find(Boolean);
            if (alvo && e.type === 'pointerup') { const r = alvo.getBoundingClientRect(); colocar(alvo, Math.min(1, Math.max(0, (ponto.x - r.left) / r.width))); comentar(alvo); }
            else voltar(true, 'jogado');
        };
        av.addEventListener('pointerup', largar);
        av.addEventListener('pointercancel', largar);
        // Ao perguntar algo, o personagem volta para perto da caixa de mensagem (e pensa lá).
        document.getElementById('rdPergunta')?.addEventListener('submit', () => voltar(true), true);
        // Se o card onde ele estava some (outro filtro ou papel), ele volta.
        document.addEventListener('raiz:dashboard', () => requestAnimationFrame(() => {
            if (av.parentElement !== casa && !visivel(av.parentElement)) voltar(false);
        }));
    }

    restaurar?.addEventListener('click', () => {
        delete estado.ordem; aplicar(padrao); voltar(true, 'restaurar'); estado = {}; gravar(); restaurar.hidden = true;
    });

    aplicar(estado.ordem);
    if (estado.avatar && av && porId(estado.avatar.modulo)) {
        const m = porId(estado.avatar.modulo), a = estado.avatar;
        requestAnimationFrame(() => colocar(m, a.frac, a.pose));
    }
    if (restaurar) restaurar.hidden = !estado.ordem && !estado.avatar;
})();
