/*
 * Fundo animado da tela de login — "quatro estações".
 * Divide a tela em 4 faixas verticais (Baixo, Médio, Alto, Excelente), cada uma com o seu
 * cenário em pixel art e uma árvore que cresce em loop, todas sincronizadas:
 * broto -> caule -> árvore completa -> volta ao broto.
 * Usa os SVGs de /public/img/FramesArvore/<tema>/ e os fundos /public/img/fundo*.webp.
 */
(function () {
	"use strict";

	const cv = document.getElementById("canvasLoginEstacoes");
	if (!cv) return;
	const ctx = cv.getContext("2d");

	const TEMAS = [ // esquerda -> direita
		{ id: "baixo", fundo: "/public/img/fundoSeca.webp" },
		{ id: "medio", fundo: "/public/img/fundoOutono.webp" },
		{ id: "alto", fundo: "/public/img/fundoPrimavera.webp" },
		{ id: "excelente", fundo: "/public/img/fundoVerao8.webp" }
	];
	const ESTAGIOS_LOOP = [2, 3, 4]; // broto, caule, árvore completa (arquivos arvore_estagio_N)
	const BW = 576, BH = 324;        // resolução nativa dos fundos (os .webp são 4x)
	const SC = 4, H = BH * SC, BASE = 0.92;
	const BAY = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]];
	const reduzirMovimento = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

	let NW, W, faixa, K, fundoCanvas;
	const assets = {};

	function carregarImagem(src) {
		return new Promise(function (ok, erro) {
			const img = new Image();
			img.onload = function () { ok(img); };
			img.onerror = erro;
			img.src = src;
		});
	}

	async function carregar() {
		const bc = document.createElement("canvas"); bc.width = BW; bc.height = BH;
		const bx = bc.getContext("2d", { willReadFrequently: true }); bx.imageSmoothingEnabled = false;
		const tc = document.createElement("canvas"); tc.width = tc.height = 160;
		const tx = tc.getContext("2d", { willReadFrequently: true }); tx.imageSmoothingEnabled = false;
		for (const t of TEMAS) {
			const fundo = await carregarImagem(t.fundo);
			bx.clearRect(0, 0, BW, BH); bx.drawImage(fundo, 0, 0, BW, BH);
			const sprites = [];
			for (const n of ESTAGIOS_LOOP) {
				const img = await carregarImagem("/public/img/FramesArvore/" + t.id + "/arvore_estagio_" + n + "_vetorial.svg");
				tx.clearRect(0, 0, 160, 160); tx.drawImage(img, 0, 0, 160, 160);
				sprites.push(tx.getImageData(0, 0, 160, 160));
			}
			assets[t.id] = { fundo: bx.getImageData(0, 0, BW, BH), sprites: sprites };
		}
	}

	// Monta o panorama estático (4 cenários lado a lado, com emenda pontilhada entre eles).
	function montarFundo() {
		NW = Math.max(320, Math.round(BH * window.innerWidth / window.innerHeight));
		W = NW * SC; faixa = NW / 4;
		cv.width = W; cv.height = H; ctx.imageSmoothingEnabled = false;
		K = Math.max(1, Math.min(4, Math.floor(faixa * SC / 165)));
		const img = new ImageData(NW, BH), o = img.data, EMENDA = 3;
		for (let x = 0; x < NW; x++) {
			const fi = Math.min(3, Math.floor(x / faixa)), local = x - fi * faixa;
			for (let y = 0; y < BH; y++) {
				let f = fi, lx = local;
				const dD = faixa - local, dE = local, b = (BAY[y & 3][x & 3] + 0.5) / 16;
				if (fi < 3 && dD < EMENDA && b < (EMENDA - dD) / (EMENDA * 2)) { f = fi + 1; lx = -dD; }
				if (fi > 0 && dE < EMENDA && b < (EMENDA - dE) / (EMENDA * 2)) { f = fi - 1; lx = faixa + dE; }
				let sx = Math.round(BW / 2 - faixa / 2 + lx); sx = ((sx % BW) + BW) % BW;
				const a = assets[TEMAS[f].id].fundo.data, i = (y * BW + sx) * 4, j = (y * NW + x) * 4;
				o[j] = a[i]; o[j + 1] = a[i + 1]; o[j + 2] = a[i + 2]; o[j + 3] = 255;
			}
		}
		fundoCanvas = document.createElement("canvas"); fundoCanvas.width = NW; fundoCanvas.height = BH;
		fundoCanvas.getContext("2d").putImageData(img, 0, 0);
	}

	// Limiares de revelação pixel a pixel (crescer = de baixo para cima; encolher = de cima para baixo).
	const T_SOBE = new Float32Array(25600), T_DESCE = new Float32Array(25600);
	(function () {
		let s = 11; const r = function () { return (s = (s * 16807) % 2147483647) / 2147483647; };
		const g = []; for (let j = 0; j <= 20; j++) { g[j] = []; for (let i = 0; i <= 20; i++) g[j][i] = r(); }
		for (let y = 0; y < 160; y++) for (let x = 0; x < 160; x++) {
			const gx = x / 8, gy = y / 8, i = gx | 0, j = gy | 0, u = gx - i, v = gy - j;
			const n = (g[j][i] * (1 - u) + g[j][i + 1] * u) * (1 - v) + (g[j + 1][i] * (1 - u) + g[j + 1][i + 1] * u) * v;
			T_SOBE[y * 160 + x] = Math.min(1, Math.max(0, (150 - y) / 128 * 0.82 + n * 0.18));
			T_DESCE[y * 160 + x] = Math.min(1, Math.max(0, (y - 20) / 130 * 0.82 + n * 0.18));
		}
	})();

	// Roteiro do loop (índices em ESTAGIOS_LOOP). Todas as árvores seguem o mesmo relógio.
	const ROTEIRO = [
		{ tipo: "parado", d: 1.6 }, { tipo: "crescer", para: 1, d: 1.5 }, { tipo: "parado", d: 1.8 },
		{ tipo: "crescer", para: 2, d: 1.6 }, { tipo: "parado", d: 4.2 }, { tipo: "encolher", para: 0, d: 1.3 },
		{ tipo: "parado", d: 0.6 }
	];
	const relogio = { estagio: 0, passo: 0, tempo: 0, p: 0, de: 0, para: 0, modo: null, pulo: 9 };
	const arvores = TEMAS.map(function () {
		const c = document.createElement("canvas"); c.width = c.height = 160;
		return { c: c, cx: c.getContext("2d"), img: new ImageData(160, 160) };
	});

	function suave(x) { return x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2; }

	function avancarRelogio(dt) {
		const r = relogio; r.tempo += dt; r.pulo += dt;
		const passo = ROTEIRO[r.passo];
		if (passo.tipo !== "parado" && r.modo === null) { r.modo = passo.tipo; r.de = r.estagio; r.para = passo.para; }
		const x = Math.min(1, r.tempo / passo.d);
		if (r.modo) r.p = suave(x) * 1.05;
		if (x >= 1) {
			if (r.modo) {
				r.estagio = r.para;
				if (r.modo === "crescer") { r.pulo = 0; TEMAS.forEach(function (t, i) { brilhos(i); }); }
				r.modo = null;
			}
			r.passo = (r.passo + 1) % ROTEIRO.length; r.tempo = 0;
		}
	}

	function desenharArvore(i) {
		const a = arvores[i], S = assets[TEMAS[i].id].sprites, r = relogio;
		if (!r.modo) { a.cx.putImageData(S[r.estagio], 0, 0); return; }
		const de = S[r.de].data, para = S[r.para].data, o = a.img.data;
		const sobe = r.modo === "crescer", T = sobe ? T_SOBE : T_DESCE, p = r.p, e = sobe ? 55 : -35;
		for (let k = 0; k < 25600; k++) {
			const i4 = k * 4, s = p > T[k] ? para : de;
			if (s[i4 + 3]) {
				o[i4] = s[i4]; o[i4 + 1] = s[i4 + 1]; o[i4 + 2] = s[i4 + 2]; o[i4 + 3] = s[i4 + 3];
				if (s === para && p - T[k] < 0.035) { // borda brilhante da onda
					o[i4] = Math.max(0, Math.min(255, o[i4] + e)); o[i4 + 1] = Math.max(0, Math.min(255, o[i4 + 1] + e)); o[i4 + 2] = Math.max(0, Math.min(255, o[i4 + 2] + e * 0.6));
				}
			} else o[i4 + 3] = 0;
		}
		a.cx.putImageData(a.img, 0, 0);
	}

	function centroFaixa(i) { return (i + 0.5) * faixa * SC; }

	// Partículas (cada uma presa à sua faixa).
	let parts = [], relampago = 0;
	function brilhos(i) {
		for (let n = 0; n < 18; n++) {
			const a = Math.random() * 6.28, r = 20 + Math.random() * 50;
			parts.push({ f: i, x: centroFaixa(i) + Math.cos(a) * r * K, y: H * BASE - (68 + relogio.estagio * 28) * K + Math.sin(a) * r * K * 0.5, vx: 0, vy: -40 - Math.random() * 40, vida: 0.6 + Math.random() * 0.8, cor: Math.random() < 0.5 ? "#fff6c8" : "#ffffff", z: K, pisca: true });
		}
	}
	const CLIMA = {
		excelente: { taxa: 1.2, criar: function (x0, w) { return { x: x0 + Math.random() * w, y: H * 0.6 + Math.random() * H * 0.35, vx: (Math.random() - 0.5) * 20, vy: -12 - Math.random() * 14, vida: 3 + Math.random() * 2, cor: "#ffe9a0", z: 4, pisca: true }; } },
		alto: { taxa: 2.2, criar: function (x0, w) { return { x: x0 + Math.random() * w - 60, y: -8, vx: 25 + Math.random() * 25, vy: 45 + Math.random() * 30, vida: 40, cor: Math.random() < 0.5 ? "#f6b8cf" : "#ffffff", z: 5, balanco: 1 }; } },
		medio: { taxa: 2, criar: function (x0, w) { return { x: x0 + Math.random() * w - 80, y: -8, vx: 55 + Math.random() * 40, vy: 55 + Math.random() * 30, vida: 40, cor: ["#d6762c", "#a64626", "#e2963e"][Math.random() * 3 | 0], z: 6, balanco: 1.5, folha: true }; } },
		baixo: { taxa: 40, criar: function (x0, w) { return { x: x0 + Math.random() * w * 1.2, y: -20, vx: -140, vy: 900 + Math.random() * 200, vida: 2, cor: "rgba(170,175,210,.55)", z: 4, chuva: true }; } }
	};
	const acumulado = [0, 0, 0, 0];

	function atualizar(dt) {
		avancarRelogio(dt);
		TEMAS.forEach(function (t, i) {
			acumulado[i] += dt * CLIMA[t.id].taxa * (faixa / 144);
			while (acumulado[i] > 1) { acumulado[i]--; const q = CLIMA[t.id].criar(i * faixa * SC, faixa * SC); q.f = i; parts.push(q); }
		});
		if (Math.random() < dt * 0.1) relampago = 0.22;
		relampago = Math.max(0, relampago - dt * 1.3);
		const agora = performance.now();
		for (const q of parts) {
			q.vida -= dt;
			if (q.balanco) q.vx += Math.sin(agora / 400 + q.y * 0.01) * q.balanco * 6;
			q.x += q.vx * dt; q.y += q.vy * dt;
			if (q.y > H + 20) q.vida = 0;
		}
		parts = parts.filter(function (q) { return q.vida > 0; });
	}

	function desenhar() {
		ctx.drawImage(fundoCanvas, 0, 0, W, H);
		const r = relogio, sq = r.pulo < 0.45 ? 1 + Math.sin(r.pulo / 0.45 * Math.PI * 2) * 0.03 * (1 - r.pulo / 0.45) : 1;
		arvores.forEach(function (a, i) {
			desenharArvore(i);
			const w = 160 * K * (2 - sq), h = 160 * K * sq, bx = centroFaixa(i) - 80 * K, by = H * BASE - 148 * K;
			ctx.drawImage(a.c, bx - (w - 160 * K) / 2, by + 148 * K * (1 - sq), w, h);
		});
		for (const q of parts) {
			const x0 = q.f * faixa * SC; if (q.x < x0 || q.x > x0 + faixa * SC) continue;
			if (q.pisca && Math.random() < 0.25) continue;
			ctx.fillStyle = q.cor;
			if (q.chuva) { ctx.fillRect(Math.round(q.x / 4) * 4, Math.round(q.y / 4) * 4, 4, 20); continue; }
			const X = Math.round(q.x / q.z) * q.z, Y = Math.round(q.y / q.z) * q.z;
			ctx.fillRect(X, Y, q.z, q.z); if (q.folha) ctx.fillRect(X + q.z, Y + q.z, q.z, q.z);
		}
		if (relampago > 0) { ctx.fillStyle = "rgba(225,222,255," + relampago + ")"; ctx.fillRect(0, 0, faixa * SC, H); }
	}

	let ultimo = performance.now();
	function loop(agora) {
		const dt = Math.min(0.05, (agora - ultimo) / 1000); ultimo = agora;
		atualizar(dt); desenhar(); requestAnimationFrame(loop);
	}

	let timerResize;
	window.addEventListener("resize", function () {
		clearTimeout(timerResize);
		timerResize = setTimeout(function () { montarFundo(); if (reduzirMovimento) desenhar(); }, 120);
	});

	carregar().then(function () {
		montarFundo();
		if (reduzirMovimento) { relogio.estagio = 2; desenhar(); return; } // sem animação: árvores completas, estáticas
		requestAnimationFrame(loop);
	}).catch(function (e) { console.warn("Fundo animado do login não carregou:", e); });
})();
