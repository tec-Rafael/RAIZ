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
    const av = document.getElementById('rdAvatar'), casa = document.getElementById('rdHello');
    let solto = null;
    function voltar(animar) {
        if (!av || !casa) return;
        const r0 = av.getBoundingClientRect(), estava = av.parentElement !== casa;
        av.classList.remove('rdAvatarSolto', 'rdAvatarArrastando'); av.style.left = av.style.top = ''; av.style.removeProperty('--rd-av-frac');
        casa.appendChild(av);
        if (animar && estava && !reduzir && r0.width) {
            const r1 = av.getBoundingClientRect();
            av.animate([{ transform: 'translate(' + (r0.left - r1.left) + 'px,' + (r0.top - r1.top) + 'px)' }, { transform: 'none' }], { duration: 500, easing: 'ease-in-out' });
        }
        if (estado.avatar) { delete estado.avatar; salvar(); }
    }
    function colocar(m, frac) {
        av.classList.remove('rdAvatarArrastando'); av.classList.add('rdAvatarSolto'); av.style.top = av.style.left = '';
        av.style.setProperty('--rd-av-frac', frac.toFixed(3));   // o CSS calcula o left e mantém o personagem dentro do card
        m.appendChild(av);
        estado.avatar = { modulo: m.dataset.modulo, frac: Math.round(frac * 1000) / 1000 }; salvar();
    }
    if (av && casa) {
        av.title = 'Arraste o personagem para cima de um card';
        av.addEventListener('pointerdown', e => {
            if (e.button !== 0) return;
            e.preventDefault();
            const r = av.getBoundingClientRect();
            solto = { gx: e.clientX - r.left, gy: e.clientY - r.top, x0: e.clientX, y0: e.clientY, w: r.width, h: r.height, origem: av.parentElement, left: av.style.left, moveu: false };
            av.setPointerCapture(e.pointerId);
        });
        av.addEventListener('pointermove', e => {
            if (!solto) return;
            if (!solto.moveu && Math.hypot(e.clientX - solto.x0, e.clientY - solto.y0) < 6) return;   // um clique não tira o personagem do lugar
            if (!solto.moveu) { solto.moveu = true; av.classList.add('rdAvatarArrastando'); document.body.classList.add('rdOrganizando'); }
            av.style.left = (e.clientX - solto.gx) + 'px'; av.style.top = (e.clientY - solto.gy) + 'px';
        });
        const largar = e => {
            if (!solto) return;
            const s = solto; solto = null; document.body.classList.remove('rdOrganizando');
            if (!s.moveu) return;
            const pes = { x: e.clientX - s.gx + s.w / 2, y: e.clientY - s.gy + s.h - 6 };
            const alvo = document.elementsFromPoint(pes.x, pes.y).filter(el => el !== av).map(el => el.closest('#rdContent [data-modulo]')).find(Boolean);
            if (alvo && e.type === 'pointerup') { const r = alvo.getBoundingClientRect(); colocar(alvo, Math.min(1, Math.max(0, (pes.x - r.left) / r.width))); }
            else voltar(true);
        };
        av.addEventListener('pointerup', largar);
        av.addEventListener('pointercancel', largar);
        // Ao perguntar algo, o personagem volta para perto da caixa de mensagem.
        document.getElementById('rdPergunta')?.addEventListener('submit', () => voltar(true), true);
        // Se o card onde ele estava some (outro filtro ou papel), ele volta.
        document.addEventListener('raiz:dashboard', () => requestAnimationFrame(() => {
            if (av.parentElement !== casa && !visivel(av.parentElement)) voltar(false);
        }));
    }

    restaurar?.addEventListener('click', () => {
        delete estado.ordem; aplicar(padrao); voltar(true); estado = {}; gravar(); restaurar.hidden = true;
    });

    aplicar(estado.ordem);
    if (estado.avatar && av && porId(estado.avatar.modulo)) {
        const m = porId(estado.avatar.modulo), a = estado.avatar;
        requestAnimationFrame(() => colocar(m, a.frac));
    }
    if (restaurar) restaurar.hidden = !estado.ordem && !estado.avatar;
})();
