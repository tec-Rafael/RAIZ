/* Motor visual da árvore: saúde e estágios recebidos da API do jardim.
   Não concede moedas e não mantém estado de jogo no navegador. */
(function () {
	"use strict";

	const cv = document.getElementById("canvasArvoreHome");
	if (!cv) return;
	const ctx = cv.getContext("2d");

	const NIVEIS = ["baixo", "medio", "alto", "excelente"];
	const FUNDOS = { baixo: "/public/img/fundoSeca.webp", medio: "/public/img/fundoOutono.webp", alto: "/public/img/fundoVerao8.webp", excelente: "/public/img/fundoPrimavera.webp" };
	const TOTAL_ESTAGIOS = 4;
	const BW = 576, BH = 324, W = BW * 4, H = BH * 4;
    let K, BASEX, BASEY, visualEstagio=1, VIS_W=W, VIS_H=H;
    // Escala única para todos os estágios (a do estágio 4, sem "pulo"), com a árvore ~42% maior (0,60 → 0,85).
    const PROPORCAO4=0.60*2.4/1.69, AFASTAR=2.4/1.69;
    // Artes hd (grade 320×320, o dobro da grade lógica de 160) com contorno e grama nos estágios 3 e 4.
    const ARTE="/public/img/FramesArvore/hd_contorno/", ARTE_V="?v=1";
    function ajustarLayout() {
        const rect=cv.getBoundingClientRect();
        const scale=Math.max(rect.width/W,rect.height/H);
        K=Math.min(rect.height*PROPORCAO4/(115*scale),rect.width*.78/(110*scale));
        const link=document.getElementById("abrirTrilha");
        if(link){
            link.style.width=Math.max(44,[34,42,78,110][visualEstagio-1]*K*scale)+"px";
            link.style.height=Math.max(44,[14,34,74,115][visualEstagio-1]*K*scale)+"px";
        }
        BASEX=W/2-80*K;
        BASEY=(rect.height*.94-(rect.height-H*scale))/scale-148*K;
        VIS_W=rect.width/scale;VIS_H=rect.height/scale;
    }
    ajustarLayout();window.addEventListener('resize',ajustarLayout);
	const reduzirMovimento = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
	cv.width = W; cv.height = H; ctx.imageSmoothingEnabled = false;

	function normalizarNivel(n) { return NIVEIS.indexOf(n) >= 0 ? n : "medio"; }
	function normalizarEstagio(e) { e = parseInt(e, 10); return isNaN(e) ? 1 : Math.max(1, Math.min(TOTAL_ESTAGIOS, e)); }

	// ---------------------------------------------------------------- assets
	const assets = {};
    const decorAssets={};
    const DECORACOES=['flores','pedras','cogumelos','cerca','colmeia','placa'];
	function carregarImagem(src) {
		return new Promise(function (ok, erro) { const i = new Image(); i.onload = function () { ok(i); }; i.onerror = erro; i.src = src; });
	}
	async function carregar() {
        await Promise.all(DECORACOES.map(async item=>{
            const img=await carregarImagem('/public/img/decoracoes/'+item+'.png');
            if(item==='flores'||item==='pedras'){
                // Os tiles incluem grama; recorta a cor no canvas para integrar ao cenário de cada AMS.
                const tile=document.createElement('canvas');tile.width=tile.height=16;
                const t=tile.getContext('2d');t.drawImage(img,0,0);const pixels=t.getImageData(0,0,16,16);
                for(let i=0;i<pixels.data.length;i+=4){const [r,g,b]=pixels.data.slice(i,i+3);if(g-r>15&&g-b>15)pixels.data[i+3]=0;}
                t.putImageData(pixels,0,0);decorAssets[item]=tile;
            }else decorAssets[item]=img;
        }));
		const bc = document.createElement("canvas"); bc.width = BW; bc.height = BH;
		const bx = bc.getContext("2d", { willReadFrequently: true }); bx.imageSmoothingEnabled = false;
		const tc = document.createElement("canvas"); tc.width = tc.height = 320;
		const tx = tc.getContext("2d", { willReadFrequently: true }); tx.imageSmoothingEnabled = false;
		for (const n of NIVEIS) {
			const fundo = await carregarImagem(FUNDOS[n]);
			bx.clearRect(0, 0, BW, BH); bx.drawImage(fundo, 0, 0, BW, BH);
			const sprites = [];
			for (let e = 1; e <= TOTAL_ESTAGIOS; e++) {
				const img = await carregarImagem(ARTE + n + "/arvore_estagio_" + e + ".png" + ARTE_V);
				tx.clearRect(0, 0, 320, 320); tx.drawImage(img, 0, 0, 320, 320);
				sprites.push(tx.getImageData(0, 0, 320, 320));
			}
			assets[n] = { fundo: fundo, fundoPx: bx.getImageData(0, 0, BW, BH), sprites: sprites };
		}
	}

	function SPR(n) { return assets[n].sprites; }
	// Personagem do jardim (avatarHome.js): quando está ativo, ele rega a árvore antes de cada crescimento no lugar das gotas.
	function avatarAtivo() { return !!(window.raizAvatarHome && window.raizAvatarHome.ativo()); }
	// ---------------------------------------------------- máscaras de transição
	const T_SOBE = new Float32Array(25600), T_DESCE = new Float32Array(25600);
	const M_MELHORA = new Float32Array(BW * BH), M_PIORA = new Float32Array(BW * BH);
	const T2_SOBE = new Float32Array(102400), T2_DESCE = new Float32Array(102400);   // as mesmas máscaras na grade hd
	(function () {
		let s = 7; const r = function () { return (s = (s * 16807) % 2147483647) / 2147483647; };
		const g = []; for (let j = 0; j <= 20; j++) { g[j] = []; for (let i = 0; i <= 20; i++) g[j][i] = r(); }
		for (let y = 0; y < 160; y++) for (let x = 0; x < 160; x++) {
			const gx = x / 8, gy = y / 8, i = gx | 0, j = gy | 0, u = gx - i, v = gy - j;
			const n = (g[j][i] * (1 - u) + g[j][i + 1] * u) * (1 - v) + (g[j + 1][i] * (1 - u) + g[j + 1][i + 1] * u) * v;
			T_SOBE[y * 160 + x] = Math.min(1, Math.max(0, (150 - y) / 128 * 0.82 + n * 0.18 + Math.abs(x - 80) / 80 * 0.06));
			T_DESCE[y * 160 + x] = Math.min(1, Math.max(0, (y - 20) / 130 * 0.82 + n * 0.18));
		}
		const gb = []; for (let j = 0; j <= 12; j++) { gb[j] = []; for (let i = 0; i <= 20; i++) gb[j][i] = r(); }
		const B = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]];
		for (let y = 0; y < BH; y++) for (let x = 0; x < BW; x++) {
			const gx = x / 32, gy = y / 32, i = gx | 0, j = gy | 0, u = gx - i, v = gy - j;
			const n = (gb[j][i] * (1 - u) + gb[j][i + 1] * u) * (1 - v) + (gb[j + 1][i] * (1 - u) + gb[j + 1][i + 1] * u) * v;
			const bay = (B[y & 3][x & 3] + 0.5) / 16;
			M_MELHORA[y * BW + x] = Math.min(1, Math.hypot((x - 288) / 330, (y - 298) / 300) * 0.8 + n * 0.14 + bay * 0.06);
			M_PIORA[y * BW + x] = Math.min(1, (y / BH) * 0.75 + n * 0.18 + bay * 0.07);
		}
		for (let y = 0; y < 320; y++) for (let x = 0; x < 320; x++) { const k = (y >> 1) * 160 + (x >> 1); T2_SOBE[y * 320 + x] = T_SOBE[k]; T2_DESCE[y * 320 + x] = T_DESCE[k]; }
	})();

	// ---------------------------------------------------------------- estado
	const estado = { nivel: "medio", estagio: 1 }; // estágio 1..4
	let fila = [];      // animações pendentes
	let atual = null;   // animação em andamento
	let parts = [], relampago = 0;

    let resolverAnimacao = null, timerAnimacao=null, decoracao = [], carregado = false;
    let prontoResolve, prontoReject;
    const pronto = new Promise((resolve,reject) => { prontoResolve=resolve; prontoReject=reject; });

	// Monta a fila: primeiro a mudança de nível (no estágio antigo), depois os estágios um a um.
	function planejar(nivel, estagio) {
		fila = [];
		let n = estado.nivel, e = estado.estagio;
		if (nivel !== n) { fila.push({ tipo: "nivel", de: n, para: nivel }); n = nivel; }
		while (e !== estagio) { const prox = e + (estagio > e ? 1 : -1); fila.push({ tipo: prox > e ? "crescer" : "encolher", de: e, para: prox }); e = prox; }
	}

	function iniciarProxima() {
		atual = fila.shift() || null;
		if (!atual) { clearTimeout(timerAnimacao); if (resolverAnimacao) { const resolver=resolverAnimacao; resolverAnimacao=null; resolver(); } return; }
		atual.t = 0; atual.p = 0; atual.bp = 0; atual.pulo = 1; atual.fase = atual.tipo === "crescer" ? (avatarAtivo() ? "rega" : "agua") : "anim";
		if (atual.tipo === "nivel") {
			atual.melhora = NIVEIS.indexOf(atual.para) > NIVEIS.indexOf(atual.de);
			estado.nivel = atual.para; // o clima já passa a ser o do nível novo
			if (!atual.melhora) for (let i = 0; i < 14; i++) parts.push({ x: BASEX + (50 + Math.random() * 60) * K, y: BASEY + (40 + Math.random() * 50) * K, vx: (Math.random() - 0.5) * 60, vy: 60 + Math.random() * 80, g: 120, cor: ["#6b5a4b", "#4a3d34", "#8a7a60"][Math.random() * 3 | 0], z: K, vida: 2.5, folha: true });
		}
		if (atual.fase === "agua") gotas(atual.de);
		if (atual.fase === "rega") {
			const passo = atual;
			window.raizAvatarHome.regar().then(function () { if (atual === passo && passo.fase === "rega") { passo.fase = "anim"; passo.t = 0; } });
		}
	}

	function suave(x) { return x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2; }

	function avancar(dt) {
		if (!atual) return;
		const a = atual; a.t += dt;
		if (a.tipo === "nivel") {
			a.bp = suave(Math.min(1, a.t / 1.9)) * 1.25;
			a.p = suave(Math.min(1, Math.max(0, (a.t - 0.35) / 1.6))) * 1.05;
			if (a.melhora && Math.random() < dt * 30) parts.push({ x: BASEX + (30 + Math.random() * 100) * K, y: BASEY + (150 - a.p * 130) * K, vx: (Math.random() - 0.5) * 30, vy: -60 - Math.random() * 60, cor: Math.random() < 0.5 ? "#d8ffb0" : "#fff2a8", z: K, vida: 0.9, pisca: true });
			if (!a.melhora && Math.random() < dt * 18) parts.push({ x: BASEX + (30 + Math.random() * 100) * K, y: BASEY + (20 + a.p * 120) * K, vx: (Math.random() - 0.5) * 40, vy: 80 + Math.random() * 60, cor: "rgba(40,36,48,.8)", z: K, vida: 1.2 });
			if (a.t > 1.95 && a.fase === "anim") { a.fase = "pulo"; a.pulo = 0; if (a.melhora) brilhos(); }
		} else if (a.tipo === "crescer") {
			if (a.fase === "agua" && a.t > 0.75) { a.fase = "anim"; a.t = 0; }
			if (a.fase === "anim") { const x = Math.min(1, a.t / 1.5); a.p = suave(x) * 1.05; if (x >= 1) { a.fase = "pulo"; a.t = 0; a.pulo = 0; estado.estagio = a.para; brilhos(); } }
		} else { // encolher
			if (a.fase === "anim") { const x = Math.min(1, a.t / 1.3); a.p = suave(x) * 1.05; if (x >= 1) { a.fase = "pulo"; a.t = 0; a.pulo = 0; estado.estagio = a.para; } }
		}
		if (a.fase === "pulo") { a.pulo += dt; if (a.pulo >= 0.45) iniciarProxima(); }
	}

	// --------------------------------------------------------------- desenho
	const spr = document.createElement("canvas"); spr.width = spr.height = 320; const sx = spr.getContext("2d");
	const sprOut = new ImageData(320, 320);
	const bgc = document.createElement("canvas"); bgc.width = BW; bgc.height = BH; const bgx = bgc.getContext("2d");
	const bgOut = new ImageData(BW, BH);

	function misturar(de, para, T, p, realce, o) {
		for (let k = 0, n = T.length; k < n; k++) {
			const i = k * 4, s = p > T[k] ? para : de;
			if (s && s[i + 3]) {
				o[i] = s[i]; o[i + 1] = s[i + 1]; o[i + 2] = s[i + 2]; o[i + 3] = s[i + 3];
				if (s === para && p - T[k] < 0.03) { o[i] = Math.max(0, Math.min(255, o[i] + realce[0])); o[i + 1] = Math.max(0, Math.min(255, o[i + 1] + realce[1])); o[i + 2] = Math.max(0, Math.min(255, o[i + 2] + realce[2])); }
			} else o[i + 3] = 0;
		}
	}

	function desenharFundo() {
		const a = atual;
		if (!a || a.tipo !== "nivel" || a.bp >= 1.2) { ctx.drawImage(assets[estado.nivel].fundo, 0, 0, W, H); return; }
		const M = a.melhora ? M_MELHORA : M_PIORA, o = bgOut.data;
		misturar(assets[a.de].fundoPx.data, assets[a.para].fundoPx.data, M, a.bp, a.melhora ? [40, 55, 20] : [-35, -35, -20], o);
		for (let k = 3; k < o.length; k += 4) o[k] = 255;
		bgx.putImageData(bgOut, 0, 0); ctx.drawImage(bgc, 0, 0, W, H);
	}

	function desenharArvore() {
        if(visualEstagio!==estado.estagio){visualEstagio=estado.estagio;ajustarLayout();}
		const a = atual, e = estado.estagio - 1;
		const pulo = 1;   // sem o "pulo" de escala (decisão do Matteus)
		if (!a || a.fase === "agua" || a.fase === "rega" || a.fase === "pulo") sx.putImageData(SPR(estado.nivel)[e], 0, 0);
		else if (a.tipo === "nivel") misturar(SPR(a.de)[e].data, SPR(a.para)[e].data, a.melhora ? T2_SOBE : T2_DESCE, a.p, a.melhora ? [60, 80, 30] : [-40, -40, -30], sprOut.data);
		else misturar(SPR(estado.nivel)[a.de - 1].data, SPR(estado.nivel)[a.para - 1].data, a.tipo === "crescer" ? T2_SOBE : T2_DESCE, a.p, a.tipo === "crescer" ? [70, 70, 40] : [-35, -35, -20], sprOut.data);
		if (a && a.fase === "anim") sx.putImageData(sprOut, 0, 0);
		const w = 160 * K * (2 - pulo), h = 160 * K * pulo;
		ctx.drawImage(spr, BASEX - (w - 160 * K) / 2, BASEY + 148 * K * (1 - pulo), w, h);
	}

	function px(x, y, cor, z) { ctx.fillStyle = cor; ctx.fillRect(Math.round(x / z) * z, Math.round(y / z) * z, z, z); }

	function desenhar() {
		desenharFundo(); desenharArvore(); desenharDecoracoes();
		if (window.raizAvatarHome) window.raizAvatarHome.desenhar(ctx, { W: W, chao: BASEY + 148 * K, visivelH: VIS_H, visivelW: VIS_W, K: K });
		for (const q of parts) {
			if (q.pisca && Math.random() < 0.25) continue;
			if (q.chuva) { ctx.fillStyle = q.cor; ctx.fillRect(Math.round(q.x / 4) * 4, Math.round(q.y / 4) * 4, 4, 20); continue; }
			px(q.x, q.y, q.cor, q.z);
			if (q.folha) px(q.x + q.z, q.y + q.z, q.cor, q.z);
			if (q.gota) px(q.x, q.y - q.z, q.cor, q.z);
		}
		if (relampago > 0) { ctx.fillStyle = "rgba(225,222,255," + relampago + ")"; ctx.fillRect(0, 0, W, H); }
	}

	// ------------------------------------------------------------ partículas
	function gotas(estagioDe) {
		for (let i = 0; i < 7; i++) parts.push({ x: BASEX + (80 + (Math.random() - 0.5) * 26) * K, y: BASEY + (estagioDe >= 3 ? 40 : 95) * K - Math.random() * 120, vx: 0, vy: 500 + Math.random() * 200, cor: "#9fd8f0", z: K, gota: true, ateY: BASEY + 138 * K, vida: 3 });
	}
	function brilhos() {
		for (let i = 0; i < 26; i++) {
			const a = Math.random() * Math.PI * 2, r = 20 + Math.random() * 60;
			parts.push({ x: BASEX + 80 * K + Math.cos(a) * r * K * 0.9, y: BASEY + (150 - estado.estagio * 28) * K + Math.sin(a) * r * K * 0.5, vx: 0, vy: -30 - Math.random() * 40, vida: 0.8 + Math.random() * 0.8, cor: Math.random() < 0.5 ? "#fff6c8" : "#ffffff", z: K, pisca: true });
		}
	}
	const CLIMA = {
		alto: { taxa: 2.2, criar: function () { return { x: Math.random() * W, y: H * 0.62 + Math.random() * H * 0.3, vx: (Math.random() - 0.5) * 20, vy: -10 - Math.random() * 15, vida: 3 + Math.random() * 2, cor: "#ffe9a0", z: 4, pisca: true }; } },
		excelente: { taxa: 5, criar: function () { return { x: Math.random() * W * 1.2 - W * 0.1, y: -10, vx: 30 + Math.random() * 30, vy: 45 + Math.random() * 30, vida: 30, cor: Math.random() < 0.5 ? "#f6b8cf" : "#ffffff", z: 5, balanco: 1 }; } },
		medio: { taxa: 4, criar: function () { return { x: -20 + Math.random() * W, y: -10, vx: 60 + Math.random() * 50, vy: 55 + Math.random() * 30, vida: 30, cor: ["#d6762c", "#a64626", "#e2963e"][Math.random() * 3 | 0], z: 6, balanco: 1.6, folha: true }; } },
		baixo: { taxa: 4, criar: function () { return { x: -20, y: H * 0.45 + Math.random() * H * 0.4, vx: 80 + Math.random() * 40, vy: -8, vida: 20, cor: "rgba(174,145,94,.35)", z: 4 }; } }
	};
	let acumulado = 0;
	function atualizarParticulas(dt) {
		const c = CLIMA[estado.nivel]; acumulado += dt * c.taxa;
		while (acumulado > 1) { acumulado--; parts.push(c.criar()); }
		relampago = Math.max(0, relampago - dt * 1.4);
		const agora = performance.now(), novas = [];
		for (const q of parts) {
			q.vida -= dt;
			if (q.balanco) q.vx += Math.sin(agora / 400 + q.y * 0.01) * q.balanco * 6;
			q.x += q.vx * dt; q.y += q.vy * dt;
			if (q.gota && q.y >= q.ateY) { q.vida = 0; for (let i = 0; i < 3; i++) novas.push({ x: q.x, y: q.ateY, vx: (Math.random() - 0.5) * 160, vy: -120 - Math.random() * 80, g: 900, cor: "#cdeefa", z: K, vida: 0.35 }); }
			if (q.g) q.vy += q.g * dt;
			if (q.y > H + 20 || q.x > W + 40) q.vida = 0;
		}
		parts = parts.filter(function (q) { return q.vida > 0; }).concat(novas);
	}

	// ------------------------------------------------------------------ loop
	let ultimo = performance.now();
	function loop(agora) {
		const decorrido=Math.max(0,(agora-ultimo)/1000),dt=Math.min(0.05,decorrido);ultimo=agora;
		avancar(Math.min(3,decorrido)); atualizarParticulas(dt); desenhar();
		requestAnimationFrame(loop);
	}

    function desenharDecoracoes() {
        const ground=BASEY+148*K;
        const scale=Math.max(cv.clientWidth/W,cv.clientHeight/H);
        const unit=Math.min(5,cv.clientWidth/(150*scale));
        // As posições foram pensadas para a árvore antiga; com a árvore ~42% maior, o x se afasta do tronco na mesma proporção.
        function item(nome,x,y=0,size=16){const img=decorAssets[nome];if(img)ctx.drawImage(img,W/2+x*AFASTAR*unit,ground+(y-size)*unit,size*unit,size*unit);}
        for(const nome of decoracao){
            if(nome==='flores')for(const x of [-90,-65,45,70])item(nome,x,5);
            if(nome==='pedras')for(let i=0;i<4;i++)item(nome,-8,20+i*15);
            if(nome==='cogumelos'){item(nome,-50,3);item(nome,36,8);}
            if(nome==='cerca')for(const x of [-110,-94,-78,62,78,94])item(nome,x,-6);
            if(nome==='colmeia')item(nome,-76,-5,24);
            if(nome==='placa')item(nome,54,-2,22);
        }
    }
    function irPara(nivel, estagio, {animar=true}={}) {
        clearTimeout(timerAnimacao);
        nivel=normalizarNivel(nivel);estagio=normalizarEstagio(estagio);
        if (resolverAnimacao) { resolverAnimacao(); resolverAnimacao=null; }
        fila=[];atual=null;
        if (!animar || reduzirMovimento || document.hidden) { estado.nivel=nivel;estado.estagio=estagio;desenhar();return Promise.resolve(); }
        return new Promise(resolve => {
            resolverAnimacao=resolve;planejar(nivel,estagio);
            // Animação é visual: uma aba lenta nunca pode bloquear os controles indefinidamente.
            // Cada crescimento inclui a caminhada e a rega do personagem; o próprio personagem desiste em 8 s.
            const limite=fila.reduce((t,p)=>t+(p.tipo==='crescer'&&avatarAtivo()?9000:3000),500);
            timerAnimacao=setTimeout(()=>{estado.nivel=nivel;estado.estagio=estagio;fila=[];atual=null;desenhar();const concluir=resolverAnimacao;resolverAnimacao=null;concluir?.();},Math.min(30000,limite));
            iniciarProxima();
        });
    }
    window.raizArvoreHome = {
        irPara, estado, pronto,
        decorar(item) { decoracao=String(item||'').split(',').filter(x=>DECORACOES.includes(x)); if (reduzirMovimento && carregado) desenhar(); },
        adubar() { if(!reduzirMovimento)gotas(estado.estagio); }
    };
    carregar().then(function () {
        estado.nivel=normalizarNivel(cv.dataset.nivel);estado.estagio=normalizarEstagio(cv.dataset.estagio);
        carregado=true;desenhar(); if(!reduzirMovimento)requestAnimationFrame(loop);
        prontoResolve();
    }).catch(function (erro) { console.warn('Árvore da home não carregou:',erro);prontoReject(new Error('Não foi possível carregar os sprites da árvore. Atualize a página.')); });
})();
