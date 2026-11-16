/* Personagem do usuário no perfil: visualização (parado, andando, regando) e edição salva em /api/avatar/me. */
(() => {
    'use strict';
    const A = window.RaizAvatar;
    const $ = id => document.getElementById('perfilAv' + id);
    const secao = $('Secao');
    if (!A || !secao) return;
    const OX = (A.W - 20) / 2;   // o desenho ocupa 20 colunas no meio da grade (a margem é do regador)
    const nomes = {
        genero: A.GENEROS, cabelo: A.CABELOS, facial: A.FACIAIS,
        sardas: [{ id: false, nome: 'Sem sardas' }, { id: true, nome: 'Com sardas' }]
    };
    const paleta = { pele: A.PALETAS.pele, olhos: A.PALETAS.olhos, corCabelo: A.PALETAS.cabelo, camisa: A.PALETAS.camisa, calca: A.PALETAS.calca };
    const ROTULOS = [['genero', 'Corpo'], ['pele', 'Pele'], ['olhos', 'Olhos'], ['cabelo', 'Cabelo'], ['corCabelo', 'Cor do cabelo'],
        ['facial', 'Barba e bigode'], ['sardas', 'Sardas'], ['camisa', 'Camisa'], ['calca', 'Calça']];
    const ORIGEM = { sorteado: 'Este personagem foi sorteado para você. Edite à vontade: ele é só seu.',
        escolhido: 'Você personalizou este personagem.', fixo: 'Personagem do administrador, o mesmo da tela de login.' };
    let salvo = null, rascunho = null, origem = null, editando = false, pose = 'parado', dir = 1, cache = {};

    // ------------------------------------------------------------ sprites
    function sprite(cfg, estado, i) {
        const k = JSON.stringify(cfg) + estado + i;
        if (cache[k]) return cache[k];
        const q = A.quadro(cfg, estado, i), c = document.createElement('canvas');
        c.width = A.W; c.height = A.H; c.getContext('2d').putImageData(new ImageData(q.dados, A.W, A.H), 0, 0);
        return (cache[k] = { c, bico: q.bico });
    }
    function cabeca(canvas, cfg) {   // recorte da cabeça (cabelo alto incluído)
        const x = canvas.getContext('2d'), s = Math.floor(canvas.width / 18);
        x.imageSmoothingEnabled = false; x.clearRect(0, 0, canvas.width, canvas.height);
        x.drawImage(sprite(cfg, 'parado', 0).c, OX + 1, 2, 18, 18, (canvas.width - 18 * s) / 2, (canvas.height - 18 * s) / 2, 18 * s, 18 * s);
    }
    function corpo(canvas, cfg) {
        const x = canvas.getContext('2d'), s = Math.floor(canvas.height / A.CHAO);
        x.imageSmoothingEnabled = false; x.clearRect(0, 0, canvas.width, canvas.height);
        x.drawImage(sprite(cfg, 'parado', 0).c, OX, 0, 20, A.CHAO, (canvas.width - 20 * s) / 2, canvas.height - A.CHAO * s, 20 * s, A.CHAO * s);
    }

    // ------------------------------------------------------------ cabeçalho do perfil (troca as iniciais pela cabeça do personagem)
    function atualizarIdentidade() {
        const box = document.querySelector('.perfilIdentity .perfilAvatar');
        if (!box || !salvo) return;
        let c = box.querySelector('canvas');
        if (!c) { c = document.createElement('canvas'); c.width = 72; c.height = 72; box.appendChild(c); }
        cabeca(c, salvo);
    }

    // ------------------------------------------------------------ palco
    const palco = $('Canvas'), px = palco.getContext('2d'), fundo = new Image();
    fundo.src = '/public/img/fundoVerao8.webp';
    const SEQ_REGAR = [0, 1, 2, 2, 2, 2, 2, 1, 0, 0];
    const reduzir = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    let t0 = performance.now(), gotas = [];
    function desenhar(agora) {
        const cfg = editando ? rascunho : salvo, t = (agora - t0) / 1000, W = palco.width, H = palco.height;
        px.imageSmoothingEnabled = false;
        if (fundo.complete && fundo.naturalWidth) {   // chão do jardim de verão, recortado embaixo
            const sw = fundo.naturalWidth * 0.5, sh = sw * H / W;
            px.drawImage(fundo, (fundo.naturalWidth - sw) / 2, fundo.naturalHeight - sh, sw, sh, 0, 0, W, H);
        } else { px.fillStyle = '#cfe3c4'; px.fillRect(0, 0, W, H); }
        if (cfg) {
            const z = 8, chao = H * 0.93, d = pose === 'parado' ? 1 : dir;
            let f;
            if (reduzir) f = 0;
            else if (pose === 'andar') f = Math.floor(t * 6) % 4;
            else if (pose === 'regar') f = SEQ_REGAR[Math.floor(t * 4) % SEQ_REGAR.length];
            else f = (t % 3.2) > 3.05 ? 1 : 0;
            const s = sprite(cfg, pose, f), w = A.W * z, x = W / 2 - (pose === 'regar' ? d * 4 * z : 0), top = chao - A.CHAO * z;
            px.fillStyle = 'rgba(20,36,20,.22)'; px.fillRect(Math.round(x - 7 * z), Math.round(chao - z), 14 * z, z);
            px.save(); px.translate(Math.round(x), Math.round(top)); if (d < 0) px.scale(-1, 1); px.drawImage(s.c, -w / 2, 0, w, A.H * z); px.restore();
            if (s.bico && f >= 1 && Math.random() < (f === 2 ? 0.5 : 0.15)) {
                const bx = d > 0 ? x - w / 2 + (s.bico[0] + 0.5) * z : x + w / 2 - (s.bico[0] + 0.5) * z;
                gotas.push({ x: bx, y: top + (s.bico[1] + 0.5) * z, vy: 60, vx: d * 30 });
            }
            px.fillStyle = '#9fd8f0';
            gotas = gotas.filter(g => { g.vy += 15; g.y += g.vy / 60; g.x += g.vx / 60; px.fillRect(Math.round(g.x / z) * z, Math.round(g.y / z) * z, z, 2 * z); return g.y < chao; });
        }
        requestAnimationFrame(desenhar);
    }
    requestAnimationFrame(desenhar);
    secao.querySelectorAll('[data-pose]').forEach(b => b.addEventListener('click', () => {
        pose = b.dataset.pose; gotas = [];
        secao.querySelectorAll('[data-pose]').forEach(o => o.setAttribute('aria-pressed', String(o === b)));
    }));
    $('Virar').addEventListener('click', () => { dir = -dir; });

    // ------------------------------------------------------------ resumo (modo visualização)
    function nomeDe(campo, valor) {
        const l = paleta[campo] || nomes[campo];
        const o = l.find(x => String(x.id) === String(valor));
        return o || { nome: String(valor) };
    }
    function resumo() {
        const dl = document.createElement('dl');
        for (const [campo, rotulo] of ROTULOS) {
            const o = nomeDe(campo, salvo[campo]), div = document.createElement('div'), dt = document.createElement('dt'), dd = document.createElement('dd');
            dt.textContent = rotulo;
            if (o.cor) { const s = document.createElement('span'); s.className = 'perfilAvCor'; s.style.background = o.cor; s.setAttribute('aria-hidden', 'true'); dd.appendChild(s); }
            dd.appendChild(document.createTextNode(o.nome));
            div.append(dt, dd); dl.appendChild(div);
        }
        $('Resumo').replaceChildren(dl);
        const p = document.createElement('p'); p.className = 'perfilAvOrigem'; p.textContent = ORIGEM[origem] || '';
        $('Resumo').appendChild(p);
    }

    // ------------------------------------------------------------ editor
    const abas = [...secao.querySelectorAll('[role=tab]')];
    function selecionarAba(t) {
        abas.forEach(o => { const sel = o === t; o.setAttribute('aria-selected', String(sel)); o.tabIndex = sel ? 0 : -1; document.getElementById(o.getAttribute('aria-controls')).hidden = !sel; });
    }
    abas.forEach((t, i) => {
        t.addEventListener('click', () => selecionarAba(t));
        t.addEventListener('keydown', e => {
            const d = e.key === 'ArrowRight' ? 1 : e.key === 'ArrowLeft' ? -1 : 0; if (!d) return;
            const n = abas[(i + d + abas.length) % abas.length]; n.focus(); selecionarAba(n);
        });
    });
    function montarGrupos(focar) {
        secao.querySelectorAll('.perfilAvGrupo').forEach(g => {
            const campo = g.dataset.campo, tipo = g.dataset.tipo, lista = tipo === 'cor' ? paleta[campo] : nomes[campo];
            const atual = lista.find(o => String(o.id) === String(rascunho[campo])) || lista[0];
            const titulo = document.createElement('p'); titulo.className = 'perfilAvGrupoTitulo'; titulo.id = 'perfilAvG-' + campo;
            titulo.textContent = g.dataset.titulo + ' '; const s = document.createElement('span'); s.textContent = atual.nome; titulo.appendChild(s);
            const box = document.createElement('div'); box.className = tipo === 'cor' ? 'perfilAvAmostras' : 'perfilAvMinis';
            box.setAttribute('role', 'radiogroup'); box.setAttribute('aria-labelledby', titulo.id);
            lista.forEach(o => {
                const b = document.createElement('button'), sel = String(o.id) === String(rascunho[campo]);
                b.type = 'button'; b.setAttribute('role', 'radio'); b.setAttribute('aria-checked', String(sel)); b.tabIndex = sel ? 0 : -1;
                if (tipo === 'cor') { b.className = 'perfilAvAmostra'; b.style.background = o.cor; b.setAttribute('aria-label', o.nome); b.title = o.nome; }
                else {
                    b.className = 'perfilAvMini' + (tipo === 'corpo' ? ' corpo' : '');
                    const c = document.createElement('canvas'); c.setAttribute('aria-hidden', 'true');
                    const teste = A.validar(Object.assign({}, rascunho, { [campo]: o.id }));
                    if (tipo === 'corpo') { c.width = 60; c.height = 74; corpo(c, teste); } else { c.width = 54; c.height = 54; cabeca(c, teste); }
                    b.append(c, document.createTextNode(o.nome));
                }
                b.addEventListener('click', () => { rascunho[campo] = o.id; rascunho = A.validar(rascunho); montarGrupos(campo); });
                box.appendChild(b);
            });
            box.addEventListener('keydown', e => {
                const d = { ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 }[e.key]; if (!d) return;
                e.preventDefault(); const bs = [...box.children], i = bs.indexOf(document.activeElement);
                bs[(i + d + bs.length) % bs.length].click();
            });
            g.replaceChildren(titulo, box);
            if (focar === campo) { const marcado = box.querySelector('[aria-checked=true]'); if (marcado) marcado.focus(); }
        });
    }
    function modo(edicao) {
        editando = edicao;
        $('Resumo').hidden = edicao; $('Editor').hidden = !edicao; $('Editar').hidden = edicao;
        if (edicao) { rascunho = Object.assign({}, salvo); montarGrupos(); selecionarAba(abas[0]); abas[0].focus(); }
        else { resumo(); }
    }
    function status(texto, erro) { const s = $('Status'); s.textContent = texto; s.classList.toggle('erro', !!erro); }
    $('Editar').addEventListener('click', () => { status(''); modo(true); });
    $('Cancelar').addEventListener('click', () => { status('Edição cancelada.'); modo(false); $('Editar').focus(); });
    $('Aleatorio').addEventListener('click', () => { rascunho = A.aleatorio(); montarGrupos(); status('Novo personagem sorteado. Salve para manter.'); });
    $('Salvar').addEventListener('click', async () => {
        const botao = $('Salvar'); botao.disabled = true; status('Salvando…');
        try {
            const r = await fetch('/api/avatar/me', { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(rascunho) });
            const corpoResposta = await r.json().catch(() => null);
            if (r.status === 401) { window.location.assign('/login'); return; }
            if (!r.ok) throw new Error(corpoResposta?.detail || corpoResposta?.message || 'Não foi possível salvar o personagem.');
            salvo = A.validar(corpoResposta.avatar); origem = corpoResposta.origem;
            modo(false); atualizarIdentidade(); status('Personagem salvo. Ele já aparece no seu jardim e no dashboard.'); $('Editar').focus();
        } catch (e) { status(e.message, true); }
        finally { botao.disabled = false; }
    });

    // ------------------------------------------------------------ carga inicial
    async function carregar() {
        status('Carregando seu personagem…');
        try {
            const r = await fetch('/api/avatar/me');
            if (r.status === 401) { window.location.assign('/login'); return; }
            const corpoResposta = await r.json().catch(() => null);
            if (!r.ok) throw new Error(corpoResposta?.detail || corpoResposta?.message || 'Não foi possível carregar o personagem.');
            salvo = A.validar(corpoResposta.avatar); origem = corpoResposta.origem;
            $('Editar').disabled = false; modo(false); atualizarIdentidade(); status('');
        } catch (e) { status(e.message, true); $('Tentar').hidden = false; }
    }
    $('Tentar').addEventListener('click', () => { $('Tentar').hidden = true; carregar(); });
    carregar();
})();
