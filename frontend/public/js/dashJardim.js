/* Jardim em pixel art no pé do dashboard (canvas #rdJardim), no estilo da árvore e do personagem,
   com os itens da loja do jardim (sprites Kenney Tiny Town, CC0) e as árvores hd com contorno.
   Só decoração: aria-hidden, sem cliques. O layout é sorteado com semente fixa, então não muda a cada visita. */
(() => {
    'use strict';
    const cv = document.getElementById('rdJardim');
    if (!cv) return;
    const ctx = cv.getContext('2d');
    const DEC = '/public/img/decoracoes/', ARV = '/public/img/FramesArvore/hd_contorno/excelente/';
    const URLS = window.RAIZ_JARDIM_IMGS || {
        cerca: DEC + 'cerca.png', cogumelos: DEC + 'cogumelos.png', colmeia: DEC + 'colmeia.png', flores: DEC + 'flores.png',
        pedras: DEC + 'pedras.png', pinheiro: DEC + 'pinheiro.png', placa: DEC + 'placa.png', presentes: DEC + 'presentes.png',
        esquilo: DEC + 'esquilo-frames.png', passaro: DEC + 'passaro-frames.png',
        arvore2: ARV + 'arvore_estagio_2.png', arvore3: ARV + 'arvore_estagio_3.png', arvore4: ARV + 'arvore_estagio_4.png'
    };
    // Cores da grama dos tiles da loja: o chão usa as mesmas, então flores e pedras se fundem nele.
    const GRAMA = '#84c669', TUFO = '#65a556', BRILHO = '#a6dc84', TERRA = '#bd6c4a', TERRA_ESC = '#763b36';
    const P = 3;               // 1 pixel dos sprites da loja = 3px na tela
    const ALTURA = 210, CHAO = 60;
    const BASE_ARVORE = 289;   // última linha com pixel nas artes hd de 320×320
    const reduzir = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const img = {};
    let cena = null, quadro = 0;

    function rng(seed) { return () => (seed = (seed * 16807) % 2147483647) / 2147483647; }

    function montar(W) {
        const r = rng(11), itens = [], topo = ALTURA - CHAO;
        // borda do chão: degraus de 0 a 2 pixels
        const borda = [];
        for (let c = 0, alvo = 1, atual = 1; c <= Math.ceil(W / P); c++) {
            if (c % 6 === 0) alvo = Math.floor(r() * 3);
            if (atual < alvo && r() < .5) atual++; else if (atual > alvo && r() < .5) atual--;
            borda.push(atual);
        }
        const tufos = [];
        for (let i = 0; i < W / 14; i++) tufos.push([Math.floor(r() * W / P), Math.floor(r() * (CHAO / P - 4)) + 3, r() < .5]);
        // sequência que se repete ao longo da largura
        const seq = ['arvore4', 'cogumelos', 'colmeia', 'flores', 'cercaPassaro', 'pedras', 'arvore3', 'esquilo', 'placa', 'flores',
            'presentes', 'pinheiro', 'pedras', 'arvore2', 'flores', 'cogumelos'];
        let x = 20 + r() * 40, i = 0;
        while (x < W - 40) {
            const tipo = seq[i % seq.length]; i++;
            const atras = topo + 6 + Math.floor(r() * 8), frente = topo + 24 + Math.floor(r() * 18);
            if (tipo.startsWith('arvore')) {
                const lado = { arvore4: 200, arvore3: 180, arvore2: 150 }[tipo];   // arte de 320px desenhada com este lado
                const larg = { arvore4: .62, arvore3: .5, arvore2: .18 }[tipo] * lado; // largura visível da copa
                itens.push({ tipo, x: x + larg / 2 - lado / 2, pe: atras, lado });
                x += larg + 14 + r() * 24;
            } else if (tipo === 'cercaPassaro') {
                const n = 3 + Math.floor(r() * 2);
                itens.push({ tipo: 'cerca', x, pe: atras, n });
                itens.push({ tipo: 'passaro', x: x + 16 * P * Math.floor(r() * n), pe: atras - 12 * P + 3, anima: true });
                x += n * 16 * P + 30 + r() * 30;
            } else if (tipo === 'flores' || tipo === 'pedras') {
                itens.push({ tipo, x, pe: ALTURA, tile: true });   // tile 16×16 com grama: vai até o fim do chão
                x += 16 * P + 8 + r() * 26;
            } else {
                itens.push({ tipo, x, pe: frente, anima: tipo === 'esquilo' });
                x += 16 * P + 10 + r() * 30;
            }
        }
        itens.sort((a, b) => (a.tile ? -1 : 0) - (b.tile ? -1 : 0) || a.pe - b.pe);
        return { W, topo, borda, tufos, itens };
    }

    function sprite(nome, sx, x, y, linhaBase) {
        const im = img[nome]; if (!im) return;
        ctx.drawImage(im, sx, 0, 16, 16, Math.round(x), Math.round(y - linhaBase * P), 16 * P, 16 * P);
    }

    function desenhar() {
        const W = cv.clientWidth; if (!W) return;
        const dpr = Math.min(window.devicePixelRatio || 1, 2);
        if (!cena || cena.W !== W) { cena = montar(W); cv.width = Math.round(W * dpr); cv.height = Math.round(ALTURA * dpr); }
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.clearRect(0, 0, W, ALTURA);
        const { topo, borda, tufos, itens } = cena;
        // chão de grama em blocos
        borda.forEach((b, c) => {
            const y = topo + b * P;
            ctx.fillStyle = TUFO; ctx.fillRect(c * P, y - P, P, P);
            ctx.fillStyle = BRILHO; ctx.fillRect(c * P, y, P, P);
            ctx.fillStyle = GRAMA; ctx.fillRect(c * P, y + P, P, ALTURA - y - P);
        });
        tufos.forEach(([c, l, alto]) => {
            ctx.fillStyle = TUFO; const x = c * P, y = topo + l * P;
            ctx.fillRect(x, y, P, P); ctx.fillRect(x + 2 * P, y, P, P); ctx.fillRect(x + P, y - P, P, P);
            if (alto) ctx.fillRect(x + P, y - 2 * P, P, P);
        });
        // faixa de terra no rodapé
        ctx.fillStyle = TERRA; ctx.fillRect(0, ALTURA - 2 * P, W, 2 * P);
        ctx.fillStyle = TERRA_ESC; ctx.fillRect(0, ALTURA - P, W, P);
        for (const it of itens) {
            ctx.imageSmoothingEnabled = false;
            if (it.tipo.startsWith('arvore')) {
                const im = img[it.tipo]; if (!im) continue;
                ctx.imageSmoothingEnabled = true; ctx.imageSmoothingQuality = 'high';
                ctx.drawImage(im, it.x, it.pe - BASE_ARVORE / 320 * it.lado, it.lado, it.lado);
            } else if (it.tipo === 'cerca') {
                for (let k = 0; k < it.n; k++) sprite('cerca', 0, it.x + k * 16 * P, it.pe, 14);
            } else if (it.tile) {
                sprite(it.tipo, 0, it.x, it.pe - 2 * P, 16);
            } else {
                const base = { cogumelos: 14, placa: 15 }[it.tipo] || 16;
                sprite(it.tipo, it.anima ? 16 * quadro : 0, it.x, it.pe, base);
            }
        }
    }

    Promise.all(Object.entries(URLS).map(([k, u]) => new Promise(ok => {
        const i = new Image(); i.onload = () => { img[k] = i; ok(); }; i.onerror = ok; i.src = u;
    }))).then(() => {
        desenhar();
        let t; window.addEventListener('resize', () => { clearTimeout(t); t = setTimeout(desenhar, 120); });
        if (!reduzir) setInterval(() => { quadro = 1 - quadro; desenhar(); }, 520);
    });
})();
