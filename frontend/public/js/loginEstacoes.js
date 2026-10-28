/*
 * Fundo animado da tela de login — "ciclo da árvore".
 * Um único cenário em tela cheia que conta uma história em loop:
 *   1. Excelente: a semente brota e cresce até a árvore completa;
 *   2. transição para o Alto (árvore completa) — passarinhos saem voando da copa;
 *   3. quando eles saem da tela, transição para o Médio — as folhas começam a cair;
 *   4. uma folha laranja da própria árvore se solta, cai devagar e, quando toca o chão,
 *      o Baixo se espalha a partir dela;
 *   5. no Baixo a árvore regride até a semente, a chuva para e uma gota d'água cai;
 *   6. quando a gota toca a semente, transição para o Excelente e o ciclo recomeça.
 * Usa os SVGs de /public/img/FramesArvore/<nivel>/ e os fundos /public/img/fundo*.webp.
 * (window.RAIZ_ASSETS, se existir, troca os caminhos por data-URIs — usado só no protótipo.)
 */
(function () {
	"use strict";

	const cv = document.getElementById("canvasLoginEstacoes");
	if (!cv) return;
	const ctx = cv.getContext("2d");

	const FUNDOS = { baixo: "/public/img/fundoSeca.webp", medio: "/public/img/fundoOutono.webp", alto: "/public/img/fundoPrimavera.webp", excelente: "/public/img/fundoVerao8.webp" };
	const NIVEIS = ["baixo", "medio", "alto", "excelente"];
	const BW = 576, BH = 324, H = BH * 4;          // fundos nativos 576x324; o canvas trabalha a 4x
	const reduzirMovimento = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
	function fonte(caminho) { return (window.RAIZ_ASSETS && window.RAIZ_ASSETS[caminho]) || caminho; }

	// ------------------------------------------------------------------ assets
	const assets = {};
	function carregarImagem(src) {
		return new Promise(function (ok, erro) { const i = new Image(); i.onload = function () { ok(i); }; i.onerror = erro; i.src = fonte(src); });
	}
	async function carregar() {
		const bc = document.createElement("canvas"); bc.width = BW; bc.height = BH;
		const bx = bc.getContext("2d", { willReadFrequently: true }); bx.imageSmoothingEnabled = false;
		const tc = document.createElement("canvas"); tc.width = tc.height = 160;
		const tx = tc.getContext("2d", { willReadFrequently: true }); tx.imageSmoothingEnabled = false;
		for (const n of NIVEIS) {
			const fundo = await carregarImagem(FUNDOS[n]);
			bx.clearRect(0, 0, BW, BH); bx.drawImage(fundo, 0, 0, BW, BH);
			const sprites = [];
			for (let e = 1; e <= 4; e++) {
				const img = await carregarImagem("/public/img/FramesArvore/" + n + "/arvore_estagio_" + e + "_vetorial.svg");
				tx.clearRect(0, 0, 160, 160); tx.drawImage(img, 0, 0, 160, 160);
				sprites.push(tx.getImageData(0, 0, 160, 160));
			}
			const fc = document.createElement("canvas"); fc.width = BW; fc.height = BH;
			fc.getContext("2d").putImageData(bx.getImageData(0, 0, BW, BH), 0, 0);
			assets[n] = { fundo: fc, fundoPx: bx.getImageData(0, 0, BW, BH), sprites: sprites };
		}
	}

	// --------------------------------------------------------------- layout
	// O canvas tem sempre 1296 px de altura e a largura acompanha a proporção da área dele na página
	// (a coluna da esquerda do login). O fundo (16:9) é desenhado em "cover", ancorado embaixo,
	// para o chão nunca ser cortado. A árvore fica centralizada nessa área.
	let W, BG = {}, TX, BASEY, K, NT = {};
	function layout() {
		const cw = cv.clientWidth || window.innerWidth, ch = cv.clientHeight || window.innerHeight;
		W = Math.max(300, Math.round(H * cw / ch));
		cv.width = W; cv.height = H; ctx.imageSmoothingEnabled = false;
		const s = Math.max(W / (BW * 4), 1);
		BG = { w: BW * 4 * s, h: H * s }; BG.x = (W - BG.w) / 2; BG.y = H - BG.h;
		BASEY = BG.y + BG.h * 0.92;
		const css = ch / H;                                 // px de CSS por px do canvas
		TX = W / 2;
		K = Math.max(2, Math.min(6, Math.floor(Math.min(cw * 0.7 / (105 * css), ch * 0.6 / (128 * css)))));
		NT = { x: (TX - BG.x) / BG.w * BW, y: (BASEY - BG.y) / BG.h * BH };   // árvore no espaço nativo do fundo
		montarMascaras();
	}

	// ------------------------------------------------------ máscaras de transição
	const T_SOBE = new Float32Array(25600), T_DESCE = new Float32Array(25600);
	const M_MELHORA = new Float32Array(BW * BH), M_PIORA = new Float32Array(BW * BH), RUIDO = new Float32Array(BW * BH);
	const BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]];
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
		for (let y = 0; y < BH; y++) for (let x = 0; x < BW; x++) {
			const gx = x / 32, gy = y / 32, i = gx | 0, j = gy | 0, u = gx - i, v = gy - j;
			RUIDO[y * BW + x] = (gb[j][i] * (1 - u) + gb[j][i + 1] * u) * (1 - v) + (gb[j + 1][i] * (1 - u) + gb[j + 1][i + 1] * u) * v;
		}
	})();
	const M_FOLHA = new Float32Array(BW * BH);
	function montarMascaraFolha(cx, cy) {   // cx, cy no canvas: o Baixo se espalha a partir desse ponto
		const nx = (cx - BG.x) / BG.w * BW, ny = (cy - BG.y) / BG.h * BH;
		for (let y = 0; y < BH; y++) for (let x = 0; x < BW; x++) {
			const k = y * BW + x, b = (BAYER[y & 3][x & 3] + 0.5) / 16;
			M_FOLHA[k] = Math.min(1, Math.hypot((x - nx) / 380, (y - ny) / 260) * 0.8 + RUIDO[k] * 0.14 + b * 0.06);
		}
	}
	function montarMascaras() {   // depende da posição da árvore
		for (let y = 0; y < BH; y++) for (let x = 0; x < BW; x++) {
			const k = y * BW + x, b = (BAYER[y & 3][x & 3] + 0.5) / 16;
			M_MELHORA[k] = Math.min(1, Math.hypot((x - NT.x) / 360, (y - NT.y + 20) / 300) * 0.8 + RUIDO[k] * 0.14 + b * 0.06);
			M_PIORA[k] = Math.min(1, (y / BH) * 0.75 + RUIDO[k] * 0.18 + b * 0.07);
		}
	}

	// ----------------------------------------------------------------- estado
	const cena = { nivel: "excelente", estagio: 1 };
	let fase = null, t = 0, trans = null;      // trans: troca de nível ou de estágio em andamento
	let parts = [], passaros = [], folha = null, gota = null, relampago = 0, chuva = 1, brilho = 0, pulo = 9, puloForca = 0.03;

	function suave(x) { return x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2; }
	function aleat(a, b) { return a + Math.random() * (b - a); }
	// ponto do sprite (0..160) -> canvas
	function px(sx) { return TX + (sx - 80) * K; }
	function py(sy) { return BASEY + (sy - 148) * K; }

	// Roteiro. Cada fase tem duração (ou condição) e o que acontece ao entrar.
	const ROTEIRO = [
		{ id: "semente", d: 2.4, entrar: function () { cena.nivel = "excelente"; cena.estagio = 1; chuva = 1; folha = null; } },
		{ id: "crescer", para: 2, d: 1.5 }, { id: "pausa", d: 0.9 },
		{ id: "crescer", para: 3, d: 1.6 }, { id: "pausa", d: 1.0 },
		{ id: "crescer", para: 4, d: 1.8 },
		{ id: "plena", d: 2.6, entrar: function () { frutosBrilham(); } },
		{ id: "nivel", para: "alto", mascara: "melhora", d: 2.2 },
		{ id: "pausa", d: 1.3 },
		{ id: "passaros", d: 30, entrar: soltarPassaros, fim: function () { return passaros.length === 0 && t > 1.5; } },
		{ id: "nivel", para: "medio", mascara: "piora", d: 2.2 },
		{ id: "folhas", d: 3.6, entrar: prenderFolhaHeroi },
		{ id: "folhaHeroi", d: 30, entrar: soltarFolhaHeroi, fim: function () { return folha && folha.estado === "pousada" && folha.t > 0.9; } },
		{ id: "nivel", para: "baixo", mascara: "folha", d: 2.8 },
		{ id: "pausa", d: 1.2 },
		{ id: "regredir", para: 3, d: 1.4 }, { id: "pausa", d: 0.9 },
		{ id: "regredir", para: 2, d: 1.3 }, { id: "pausa", d: 0.9 },
		{ id: "regredir", para: 1, d: 1.2 },
		{ id: "calmaria", d: 2.0 },
		{ id: "gota", d: 30, entrar: soltarGota, fim: function () { return gota === null; } },
		{ id: "nivel", para: "excelente", mascara: "melhora", d: 2.2 }
	];
	let passo = -1;

	function proximaFase() {
		passo = (passo + 1) % ROTEIRO.length; fase = ROTEIRO[passo]; t = 0; trans = null;
		if (fase.entrar) fase.entrar();
		if (fase.id === "crescer" || fase.id === "regredir") trans = { tipo: "estagio", de: cena.estagio, para: fase.para, sobe: fase.id === "crescer", p: 0 };
		if (fase.id === "nivel") {
			trans = { tipo: "nivel", de: cena.nivel, para: fase.para, mascara: fase.mascara, bp: 0, tp: 0 };
			if (fase.mascara === "piora") for (let i = 0; i < 12; i++) parts.push(folhaSolta(aleat(45, 115), aleat(40, 95), ["#6b5a4b", "#4a3d34", "#8a7a60"]));
			if (fase.mascara === "folha" && folha) montarMascaraFolha(px(folha.sx), py(folha.sy));   // o Baixo se espalha a partir da folha
		}
	}

	function avancar(dt) {
		t += dt; pulo += dt; brilho = Math.max(0, brilho - dt);
		const f = fase;
		if (trans && trans.tipo === "estagio") {
			const x = Math.min(1, t / f.d); trans.p = suave(x) * 1.05;
			if (trans.sobe && Math.random() < dt * 40) parts.push({ x: px(aleat(50, 110)), y: py(150 - x * (40 + trans.para * 25)), vx: aleat(-20, 20), vy: aleat(-80, -40), cor: Math.random() < 0.5 ? "#d8ffb0" : "#fff2a8", z: K, vida: aleat(0.5, 1), pisca: true });
			if (x >= 1) { cena.estagio = trans.para; pulo = 0; puloForca = trans.sobe ? 0.035 : -0.02; if (trans.sobe) brilhos(26); trans = null; }
		}
		if (trans && trans.tipo === "nivel") {
			{
				trans.bp = suave(Math.min(1, t / (f.d * 0.86))) * 1.25;
				trans.tp = suave(Math.min(1, Math.max(0, (t - 0.35) / (f.d * 0.72)))) * 1.05;
				if (trans.mascara === "melhora" && Math.random() < dt * 30) parts.push({ x: px(aleat(30, 130)), y: py(150 - trans.tp * 130), vx: aleat(-15, 15), vy: aleat(-120, -60), cor: Math.random() < 0.5 ? "#d8ffb0" : "#fff2a8", z: K, vida: 0.9, pisca: true });
			}
			if (trans.mascara === "folha" && folha) folha.murcha = Math.min(1, t / (f.d * 0.8));
			if (t >= f.d) {
				cena.nivel = trans.para;
				if (trans.mascara === "melhora") { brilhos(30); pulo = 0; puloForca = 0.03; }
				if (trans.mascara === "folha") { esfarelarFolha(); relampago = 0.3; }
				trans = null;
			}
		}
		// fases especiais
		if (f.id === "semente") {   // a semente "respira" e dá dois pulinhos antes de brotar
			if ((t > 0.9 && t - dt <= 0.9) || (t > 1.6 && t - dt <= 1.6)) { pulo = 0; puloForca = 0.05; }
			if (Math.random() < dt * 6) parts.push({ x: px(80 + aleat(-14, 14)), y: py(aleat(128, 140)), vx: 0, vy: aleat(-25, -10), cor: "#fff6c8", z: K, vida: aleat(0.6, 1.2), pisca: true });
		}
		if (f.id === "folhas") {    // folhas caem da copa, cada vez mais
			if (Math.random() < dt * (2 + t * 2.2)) parts.push(folhaSolta(aleat(40, 120), aleat(35, 95), ["#d6762c", "#a64626", "#e2963e", "#b8602a"]));
		}
		if (f.id === "calmaria") chuva = Math.max(0, 1 - t / f.d);
		if (f.fim ? f.fim() : t >= f.d) proximaFase();
	}

	// --------------------------------------------------------------- eventos
	function brilhos(n) {
		for (let i = 0; i < n; i++) {
			const a = Math.random() * Math.PI * 2, r = 20 + Math.random() * 60;
			parts.push({ x: px(80) + Math.cos(a) * r * K * 0.9, y: py(150 - cena.estagio * 28) + Math.sin(a) * r * K * 0.5, vx: 0, vy: aleat(-70, -30), vida: aleat(0.8, 1.6), cor: Math.random() < 0.5 ? "#fff6c8" : "#ffffff", z: K, pisca: true });
		}
	}
	function frutosBrilham() {   // estrelinhas pela copa quando a árvore fica completa
		brilho = 2.4;
		for (let i = 0; i < 40; i++) parts.push({ x: px(aleat(38, 122)), y: py(aleat(30, 98)), vx: 0, vy: aleat(-20, -5), vida: aleat(0.6, 2.2), atraso: aleat(0, 1.4), cor: Math.random() < 0.5 ? "#fff6c8" : "#ffd27a", z: K, estrela: true });
	}
	function folhaSolta(sx, sy, cores) {
		return { x: px(sx), y: py(sy), vx: aleat(-10, 30), vy: aleat(20, 50), g: 30, vmax: aleat(70, 110), balanco: aleat(0.8, 1.6), fase: Math.random() * 6, cor: cores[Math.random() * cores.length | 0], z: K, vida: 8, folha: true, chao: py(aleat(136, 146)) };
	}
	function soltarPassaros() {
		passaros = [];
		const saidas = [[60, 52], [96, 44], [112, 64], [74, 70], [50, 78], [104, 84], [84, 58]];
		saidas.forEach(function (s, i) {
			passaros.push({ x: px(s[0]), y: py(s[1]), atraso: 0.2 + i * 0.28, vx: aleat(110, 160), vy: aleat(-160, -110), ax: aleat(30, 60), bat: Math.random(), cor: "#2b2a36" });
		});
		for (let i = 0; i < 10; i++) parts.push(folhaSolta(aleat(45, 115), aleat(40, 90), ["#5ea548", "#78c254", "#f6b8cf"]));
	}
	// A folha "heroína" é uma das folhas laranjas da borda da copa (coordenadas no espaço do sprite,
	// para continuar no lugar certo se a janela mudar de tamanho).
	let pontoFolha = { sx: 118, sy: 66 };
	function acharBordaCopa() {   // pixel laranja mais à direita da copa do Médio, na altura do meio da copa
		const d = assets.medio.sprites[3].data; let melhor = null;
		for (let y = 58; y <= 74; y++) for (let x = 159; x > 80; x--) {
			const i = (y * 160 + x) * 4;
			if (d[i + 3] > 200 && d[i] > 150 && d[i] > d[i + 1] + 40) { if (!melhor || x > melhor.sx) melhor = { sx: x + 2, sy: y }; break; }
		}
		if (melhor) pontoFolha = melhor;
	}
	function prenderFolhaHeroi() {
		folha = { estado: "presa", sx: pontoFolha.sx, sy: pontoFolha.sy, x0: pontoFolha.sx, t: 0, murcha: 0 };
	}
	function soltarFolhaHeroi() {
		if (!folha) prenderFolhaHeroi();
		folha.estado = "tremendo"; folha.t = 0;
		for (let i = 0; i < 6; i++) parts.push({ x: px(folha.sx) + aleat(-4, 4) * K, y: py(folha.sy) + aleat(-4, 4) * K, vx: 0, vy: aleat(-30, -10), cor: "#fff2c8", z: K, vida: aleat(0.4, 0.9), atraso: aleat(0, 0.5), pisca: true });
	}
	function esfarelarFolha() {
		if (!folha) return;
		for (let i = 0; i < 14; i++) parts.push({ x: px(folha.sx) + aleat(-3, 3) * K, y: py(folha.sy) + aleat(-2, 1) * K, vx: aleat(-60, 140), vy: aleat(-90, -20), g: 160, cor: ["#5b4a3f", "#3e322b", "#7a6550"][Math.random() * 3 | 0], z: K, vida: aleat(0.8, 1.6) });
		folha = null;
	}
	function soltarGota() {
		gota = { x: TX, y: -40, vy: 120, alvo: py(133) };
	}

	function atualizarObjetos(dt) {
		// passarinhos
		for (const b of passaros) {
			if (b.atraso > 0) { b.atraso -= dt; continue; }
			b.vx += b.ax * dt; b.vy -= 30 * dt; b.x += b.vx * dt; b.y += b.vy * dt + Math.sin(b.bat * 6) * 1.5; b.bat += dt * 1.6;
		}
		passaros = passaros.filter(function (b) { return b.x < W + 60 && b.y > -60; });
		// folha que se solta da árvore do Médio
		if (folha) {
			const fo = folha; fo.t += dt;
			if (fo.estado === "tremendo" && fo.t > 1.3) { fo.estado = "caindo"; fo.t = 0; fo.x0 = fo.sx; fo.y0 = fo.sy; }
			if (fo.estado === "caindo") {
				const fi = fo.t * 1.7;                                            // balanço de pêndulo
				fo.sx = fo.x0 + Math.sin(fi) * 16 + fo.t * 5;
				fo.sy += dt * (6 + 11 * Math.abs(Math.cos(fi)));                  // mais rápida no meio do balanço
				fo.giro = Math.cos(fi);
				if (fo.sy >= 143) {
					fo.sy = 143; fo.estado = "pousada"; fo.t = 0; fo.giro = 1;
					for (let i = 0; i < 5; i++) parts.push({ x: px(fo.sx), y: py(144), vx: aleat(-70, 70), vy: aleat(-60, -20), g: 200, cor: "#c9b27a", z: K, vida: 0.5 });
				}
			}
		}
		// gota d'água final
		if (gota) {
			gota.vy += 900 * dt; gota.y += gota.vy * dt;
			if (gota.y >= gota.alvo) {
				for (let i = 0; i < 10; i++) parts.push({ x: gota.x, y: gota.alvo, vx: aleat(-220, 220), vy: aleat(-260, -120), g: 1100, cor: Math.random() < 0.5 ? "#9fd8f0" : "#cdeefa", z: K, vida: 0.6 });
				parts.push({ anel: true, x: gota.x, y: gota.alvo + K * 3, r: K * 2, vida: 0.7, z: K });
				pulo = 0; puloForca = 0.05; gota = null;
			}
		}
	}

	// ------------------------------------------------------------- clima
	const CLIMA = {
		excelente: { taxa: 2.2, criar: function () { return { x: aleat(0, W), y: H * 0.6 + Math.random() * H * 0.35, vx: aleat(-10, 10), vy: aleat(-25, -10), vida: aleat(3, 5), cor: "#ffe9a0", z: 4, pisca: true }; } },
		alto: { taxa: 4, criar: function () { return { x: aleat(-0.1, 1.1) * W, y: -10, vx: aleat(30, 60), vy: aleat(45, 75), vida: 30, cor: Math.random() < 0.5 ? "#f6b8cf" : "#ffffff", z: 5, balanco: 1 }; } },
		medio: { taxa: 3.5, criar: function () { return { x: aleat(-20, W), y: -10, vx: aleat(60, 110), vy: aleat(55, 85), vida: 30, cor: ["#d6762c", "#a64626", "#e2963e"][Math.random() * 3 | 0], z: 6, balanco: 1.6, vento: true }; } },
		baixo: { taxa: 90, criar: function () { return { x: aleat(-0.15, 1.15) * W, y: -20, vx: -140, vy: aleat(900, 1100), vida: 2, cor: "rgba(170,175,210,.55)", z: 4, chuva: true }; } }
	};
	let acumulado = 0;
	function atualizarParticulas(dt) {
		const nivelClima = trans && trans.tipo === "nivel" ? trans.para : cena.nivel;
		const c = CLIMA[nivelClima];
		const pausaClima = fase && fase.id === "folhaHeroi";   // só a folha da árvore em cena, sem folhas do fundo
		acumulado += pausaClima ? 0 : dt * c.taxa * (W / 2304) * (nivelClima === "baixo" ? chuva : 1);
		while (acumulado > 1) { acumulado--; parts.push(c.criar()); }
		if (nivelClima === "baixo" && chuva > 0.6 && !(trans && trans.tipo === "nivel") && Math.random() < dt * 0.15) relampago = 0.2;
		relampago = Math.max(0, relampago - dt * 1.3);
		const agora = performance.now(), novas = [];
		for (const q of parts) {
			if (q.atraso > 0) { q.atraso -= dt; continue; }
			q.vida -= dt;
			if (q.anel) { q.r += dt * 60 * K; continue; }
			if (q.balanco) q.vx += Math.sin(agora / 400 + q.y * 0.01 + (q.fase || 0)) * q.balanco * 6;
			if (q.g) q.vy = Math.min(q.vmax || 1e9, q.vy + q.g * dt);
			if (q.folha && q.chao && q.y >= q.chao) { q.vx = 0; q.vy = 0; q.g = 0; q.balanco = 0; q.vida = Math.min(q.vida, 1.2); }
			q.x += q.vx * dt; q.y += q.vy * dt;
			if (q.y > H + 20 || q.x > W + 40) q.vida = 0;
		}
		parts = parts.filter(function (q) { return q.vida > 0; }).concat(novas);
	}

	// --------------------------------------------------------------- desenho
	const spr = document.createElement("canvas"); spr.width = spr.height = 160; const sx = spr.getContext("2d");
	const sprOut = new ImageData(160, 160);
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
	function realceDe(m) { return m === "melhora" ? [40, 55, 20] : [-35, -35, -20]; }

	function desenharFundo() {
		let img = assets[cena.nivel].fundo;
		if (trans && trans.tipo === "nivel" && trans.bp > 0) {
			if (trans.bp >= 1.2) img = assets[trans.para].fundo;
			else {
				const o = bgOut.data;
				misturar(assets[trans.de].fundoPx.data, assets[trans.para].fundoPx.data, trans.mascara === "melhora" ? M_MELHORA : trans.mascara === "folha" ? M_FOLHA : M_PIORA, trans.bp, realceDe(trans.mascara), o);
				for (let k = 3; k < o.length; k += 4) o[k] = 255;
				bgx.putImageData(bgOut, 0, 0); img = bgc;
			}
		}
		ctx.drawImage(img, BG.x, BG.y, BG.w, BG.h);
	}

	function desenharArvore() {
		const e = cena.estagio - 1;
		if (trans && trans.tipo === "estagio") {
			const A = assets[cena.nivel].sprites;
			misturar(A[trans.de - 1].data, A[trans.para - 1].data, trans.sobe ? T_SOBE : T_DESCE, trans.p, trans.sobe ? [70, 70, 40] : [-35, -35, -20], sprOut.data);
			sx.putImageData(sprOut, 0, 0);
		} else if (trans && trans.tipo === "nivel" && trans.tp > 0 && trans.tp < 1.05) {
			const melhora = trans.mascara === "melhora";
			misturar(assets[trans.de].sprites[e].data, assets[trans.para].sprites[e].data, melhora ? T_SOBE : T_DESCE, trans.tp, melhora ? [60, 80, 30] : [-40, -40, -30], sprOut.data);
			sx.putImageData(sprOut, 0, 0);
		} else {
			const n = trans && trans.tipo === "nivel" && trans.tp >= 1.05 ? trans.para : cena.nivel;
			sx.putImageData(assets[n].sprites[e], 0, 0);
		}
		let sq = 1;
		if (pulo < 0.45) sq = 1 + Math.sin(pulo / 0.45 * Math.PI * 2) * puloForca * (1 - pulo / 0.45);
		else if (cena.estagio === 1 && fase && fase.id === "semente") sq = 1 + Math.sin(t * 5) * 0.012;  // respira
		const w = 160 * K * (2 - sq), h = 160 * K * sq;
		ctx.drawImage(spr, TX - w / 2, BASEY - 148 * K * sq, w, h);   // base da árvore (linha 148 do sprite) fica presa ao chão
	}

	function quad(x, y, z, cor) { ctx.fillStyle = cor; ctx.fillRect(Math.round(x / z) * z, Math.round(y / z) * z, z, z); }

	const PASSARO = [
		["X.....X", ".X...X.", "..XXX..", "...X..."],
		["...X...", "..XXX..", ".X...X.", "X.....X"]
	];
	function desenharPassaro(b) {
		const fr = PASSARO[Math.floor(b.bat * 8) % 2], z = Math.max(3, K + 1);
		ctx.fillStyle = b.cor;
		for (let j = 0; j < fr.length; j++) for (let i = 0; i < 7; i++) if (fr[j][i] === "X") ctx.fillRect(Math.round(b.x / z) * z + (i - 3) * z, Math.round(b.y / z) * z + j * z, z, z);
	}
	const FOLHA = {
		aberta: ["...os..", ".oobbo.", "obblbbo", "obbvbbo", ".obbbo.", "..ooo.."],
		espelho: ["..so...", ".obboo.", "obblbbo", "obbvbbo", ".obbbo.", "..ooo.."],
		fina: ["..os..", "..obo.", ".obbo.", ".obvo.", "..oo..", "......"]
	};
	function misturaCor(a, b, t) {
		const pa = parseInt(a.slice(1), 16), pb = parseInt(b.slice(1), 16);
		const r = Math.round(((pa >> 16) & 255) * (1 - t) + ((pb >> 16) & 255) * t), g = Math.round(((pa >> 8) & 255) * (1 - t) + ((pb >> 8) & 255) * t), bl = Math.round((pa & 255) * (1 - t) + (pb & 255) * t);
		return "rgb(" + r + "," + g + "," + bl + ")";
	}
	function desenharFolhaHeroi(fo) {
		const g = fo.giro === undefined ? 1 : fo.giro;
		const fr = g > 0.35 ? FOLHA.aberta : g < -0.35 ? FOLHA.espelho : FOLHA.fina;
		const m = fo.murcha || 0;   // murcha: laranja -> marrom acinzentado durante a transição
		const pal = { o: misturaCor("#4a1f12", "#2a221e", m), b: misturaCor("#e8822e", "#6b5a4b", m), l: misturaCor("#ffc96e", "#8a7a60", m), v: misturaCor("#a64626", "#4a3d34", m), s: misturaCor("#5a3a22", "#3e322b", m) };
		// presa ou tremendo: a folha pisca um brilho suave para chamar a atenção antes de cair
		if ((fo.estado === "presa" && fase && fase.id === "folhas" && t > 1.2) || fo.estado === "tremendo") {
			const k = 0.5 + 0.5 * Math.sin(performance.now() / 160);
			pal.b = misturaCor("#e8822e", "#ffb347", k); pal.l = misturaCor("#ffc96e", "#fff1b8", k);
		}
		const z = Math.round(K * 2);
		let x = px(fo.sx), y = py(fo.sy);
		if (fo.estado === "tremendo") x += Math.sin(fo.t * 38) * K * 0.7 * Math.min(1, fo.t * 2);
		const ox = Math.round(x / z) * z - 3 * z, oy = Math.round(y / z) * z - 3 * z;
		for (let j = 0; j < fr.length; j++) for (let i = 0; i < fr[j].length; i++) { const c = pal[fr[j][i]]; if (c) { ctx.fillStyle = c; ctx.fillRect(ox + i * z, oy + j * z, z, z); } }
	}
	function desenharGota(g) {
		const z = Math.round(K * 1.4), x = Math.round(g.x / z) * z, y = Math.round(g.y / z) * z;
		ctx.fillStyle = "rgba(159,216,240,.35)"; ctx.fillRect(x, y - 6 * z, z, 4 * z);       // rastro
		const forma = [".c.", "cbc", "bbb", "bdb", ".b."], pal = { c: "#cdeefa", b: "#6fbfe6", d: "#3d8fc0" };
		for (let j = 0; j < 5; j++) for (let i = 0; i < 3; i++) { const c = forma[j][i]; if (pal[c]) { ctx.fillStyle = pal[c]; ctx.fillRect(x + (i - 1) * z, y + (j - 4) * z, z, z); } }
		ctx.fillStyle = "#ffffff"; ctx.fillRect(x - z, y - 2 * z, z, z);
	}

	function desenhar() {
		desenharFundo();
		if (brilho > 0) {   // halo suave atrás da árvore quando ela fica completa
			const a = Math.min(1, brilho) * 0.18, r = 70 * K;
			const g = ctx.createRadialGradient(TX, py(70), r * 0.2, TX, py(70), r);
			g.addColorStop(0, "rgba(255,240,170," + a + ")"); g.addColorStop(1, "rgba(255,240,170,0)");
			ctx.fillStyle = g; ctx.fillRect(TX - r, py(70) - r, r * 2, r * 2);
		}
		desenharArvore();
		for (const q of parts) {
			if (q.atraso > 0) continue;
			if (q.anel) { const c = "rgba(205,238,250," + Math.max(0, q.vida / 0.7) + ")", n = Math.max(12, Math.round(q.r / K * 1.6)); for (let i = 0; i < n; i++) { const a = i / n * Math.PI * 2; quad(q.x + Math.cos(a) * q.r, q.y + Math.sin(a) * q.r * 0.3, K, c); } continue; }   // anel em pixels
			if (q.pisca && Math.random() < 0.25) continue;
			if (q.chuva) { ctx.fillStyle = q.cor; ctx.fillRect(Math.round(q.x / 4) * 4, Math.round(q.y / 4) * 4, 4, 20); continue; }
			if (q.estrela) { const z = q.z, c = q.cor, v = q.vida; if (Math.sin(v * 9) > -0.2) { quad(q.x, q.y, z, c); if (v > 0.4) { quad(q.x - z, q.y, z, c); quad(q.x + z, q.y, z, c); quad(q.x, q.y - z, z, c); quad(q.x, q.y + z, z, c); } } continue; }
			quad(q.x, q.y, q.z, q.cor);
			if (q.folha || q.vento) quad(q.x + q.z, q.y + q.z, q.z, q.cor);
		}
		for (const b of passaros) if (!(b.atraso > 0)) desenharPassaro(b);
		if (folha) desenharFolhaHeroi(folha);
		if (gota) desenharGota(gota);
		if (relampago > 0) { ctx.fillStyle = "rgba(225,222,255," + relampago + ")"; ctx.fillRect(0, 0, W, H); }
	}

	// ------------------------------------------------------------------ loop
	let ultimo = performance.now();
	function loop(agora) {
		const dt = Math.min(0.05, (agora - ultimo) / 1000); ultimo = agora;
		avancar(dt); atualizarObjetos(dt); atualizarParticulas(dt); desenhar();
		requestAnimationFrame(loop);
	}

	let timerResize, pronto = false;
	function aoRedimensionar() {
		if (!pronto) return;
		clearTimeout(timerResize);
		timerResize = setTimeout(function () { layout(); if (reduzirMovimento) desenhar(); }, 120);
	}
	if (window.ResizeObserver) new ResizeObserver(aoRedimensionar).observe(cv);
	else window.addEventListener("resize", aoRedimensionar);

	window.raizLoginCiclo = { cena: cena, fase: function () { return fase && fase.id; } };   // útil para depurar no console

	carregar().then(function () {
		layout(); pronto = true; acharBordaCopa();
		if (reduzirMovimento) { cena.nivel = "excelente"; cena.estagio = 4; desenhar(); return; }   // sem animação: árvore completa, parada
		proximaFase();
		requestAnimationFrame(loop);
	}).catch(function (e) { console.warn("Fundo animado do login não carregou:", e); });
})();
