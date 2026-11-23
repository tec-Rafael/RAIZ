/*
 * Personagem do usuário no jardim (home), com o sprite de avatarSprite.js (window.RaizAvatar).
 * O avatar vem de GET /api/avatar/me; enquanto não chega (ou se falhar), o personagem não aparece e a
 * árvore volta a usar as gotas d'água.
 * Parado = de frente (pisca); andar e regar = de perfil. A cada evolução de estágio ele vai até a árvore
 * e rega; o crescimento espera a promessa de regar() terminar. Não tem loop próprio: o homeArvore.js chama
 * raizAvatarHome.desenhar(ctx, geo) a cada quadro, depois da árvore.
 * Tamanho: proporcional à árvore (no desktop, 30% da altura visível). Sombra de contato em pixel art sob os pés.
 * Arrastar: o personagem pode ser pego com o mouse ou o dedo e fica pendurado pelo topo da cabeça, balançando.
 * Solto do alto, ele cai (braços para cima), amassa ao bater no chão, levanta poeira e reclama.
 * Ao começar a andar, às vezes ele fala uma frase curta num balão.
 */
(function () {
	"use strict";
	const cv = document.getElementById("canvasArvoreHome"), C = window.RaizAvatar;
	if (!cv || !C) return;
	const raiz = cv.parentElement;
	// A área do cenário captura o arraste, e os painéis mantêm a rolagem pelo CSS.
	const FIG = 32, ALTURA = 0.30;
	const PASSEIO = ["Que dia bonito para crescer!", "Será que a árvore está com sede?", "Hoje eu aprendo algo novo.",
		"Esse jardim está ficando lindo.", "Já conferiu seus desafios?", "Um curso por dia, uma folha nova!",
		"Hmm, cheiro de grama.", "Lá-lá-lá…", "Cuidar da árvore é cuidar de mim.", "Quantas moedas será que eu tenho?"];
	const RECLAMA = ["Ai! Isso doeu!", "Ei, mais cuidado!", "Ui, minhas costas…", "Precisava disso?", "Que tombo!", "Me põe no chão devagar!"];
	const sorteio = function (l) { return l[Math.floor(Math.random() * l.length)]; };
	let cfg = null, cache = {};
	const reduzir = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

	function spr(e, i) {
		const k = e + i; if (cache[k]) return cache[k];
		const q = C.quadro(cfg, e, i), c = document.createElement("canvas"); c.width = C.W; c.height = C.H;
		c.getContext("2d").putImageData(new ImageData(q.dados, C.W, C.H), 0, 0);
		return cache[k] = { c: c, bico: q.bico };
	}
	// sombra de contato: elipse em pixel art, borda mais clara e miolo mais escuro
	let sombra = null;
	function elipse(rx, ry) {
		if (sombra) return sombra;
		const w = 2 * rx + 1, h = 2 * ry + 1, c = document.createElement("canvas"); c.width = w; c.height = h;
		const x = c.getContext("2d"), o = x.createImageData(w, h);
		for (let j = -ry; j <= ry; j++) for (let i = -rx; i <= rx; i++) {
			const d = Math.pow(i / (rx + .5), 2) + Math.pow(j / (ry + .5), 2); if (d > 1) continue;
			const p = ((j + ry) * w + i + rx) * 4; o.data[p] = 20; o.data[p + 1] = 36; o.data[p + 2] = 20; o.data[p + 3] = d <= .38 ? 107 : 66;
		}
		x.putImageData(o, 0, 0); return sombra = c;
	}

	// modo: solo (passeia e rega), segurado (pendurado no ponteiro), caindo, pouso (amassa e reclama)
	const av = { x: null, dir: 1, pose: "parado", rel: 0, espera: 4, alvo: null, rega: null,
		modo: "solo", alt: 0, vx: 0, vy: 0, ang: 0, w: 0, hx: 0, hy: 0, t: 0, reclamar: false, fala: null, ultimaFala: 0 };
	let geo = null, z = 12, gotas = [], poeira = [], ultimo = 0;
	// Tamanho proporcional à árvore, igual em qualquer tela: z = K × RAZAO, sem arredondar.
	// RAZAO é a proporção do desktop, onde a árvore é limitada pela altura (K = altura visível × 0,852 / 115) e o
	// personagem ocupa 30% da altura visível (32 × z). Assim, no desktop continua com 30% da altura e, no celular,
	// onde a árvore é limitada pela largura, encolhe junto com ela. Limite de segurança: 32% da largura.
	const RAZAO = ALTURA * 115 / (FIG * 0.60 * 2.4 / 1.69);   // ≈ 1,265
	function escala() {
		const k = geo.K || geo.visivelH * 0.60 * 2.4 / 1.69 / 115;
		z = Math.max(2, Math.min(k * RAZAO, geo.visivelW * 0.32 / 16));
	}
	function limites() { const m = Math.max(6 * z, geo.visivelW / 2 - 16 * z); return [geo.W / 2 - m, geo.W / 2 + m]; }
	function pose(p) { if (av.pose !== p) { av.pose = p; av.rel = 0; } }
	function falar(texto, ms) { av.fala = { texto: texto, ate: performance.now() + (ms || 3000) }; av.ultimaFala = performance.now(); }
	function andarAte(x, dt) {
		const d = x - av.x, p = 20 * z * dt; if (Math.abs(d) <= p) { av.x = x; return true; }
		av.dir = d > 0 ? 1 : -1; av.x += av.dir * p; pose("andar"); return false;
	}
	function passear(dt) {
		if (av.alvo === null) {
			pose("parado"); av.espera -= dt;
			if (av.espera <= 0) {
				const l = limites(); let x, n = 0; do { x = l[0] + Math.random() * (l[1] - l[0]); } while (Math.abs(x - av.x) < Math.min(30 * z, (l[1] - l[0]) / 3) && ++n < 8);
				av.alvo = x;
				if (Math.random() < .45 && performance.now() - av.ultimaFala > 7000) falar(sorteio(PASSEIO), 3000);
			}
		} else if (andarAte(av.alvo, dt)) { av.alvo = null; av.espera = 1.5 + Math.random() * 3; }
	}
	// rega: ir até a árvore -> segurar -> erguer -> despejar -> baixar
	const ROTEIRO = [{ q: 0, t: .25 }, { q: 1, t: .25 }, { q: 2, t: 1.4 }, { q: 1, t: .2 }, { q: 0, t: .2 }];
	function regar() {
		if (av.rega) return av.rega.promessa;
		if (av.modo !== "solo") return Promise.resolve();   // pendurado ou caindo: a árvore cresce sem esperar a rega
		let ok; const promessa = new Promise(function (r) { ok = r; });
		const lado = av.x < geo.W / 2 ? 1 : -1;
		av.alvo = null; av.rega = { promessa: promessa, ok: ok, lado: lado, x: geo.W / 2 - lado * 14 * z, fase: -1, t: 0, acc: 0 };
		av.rega.timer = setTimeout(terminar, 8000);   // segurança: a árvore nunca espera para sempre
		return promessa;
	}
	function terminar() { const r = av.rega; if (!r) return; clearTimeout(r.timer); av.rega = null; av.espera = 2.2; pose("parado"); r.ok(); }
	function atualizarRega(dt) {
		const r = av.rega;
		if (r.fase < 0) { if (andarAte(r.x, dt)) { r.fase = 0; r.t = 0; av.dir = r.lado; pose("regar"); } return; }
		r.t += dt; if (r.t >= ROTEIRO[r.fase].t) { r.t = 0; r.fase++; if (r.fase >= ROTEIRO.length) terminar(); }
	}

	// ------------------------------------------------------------ arrastar com o mouse ou o dedo
	let pegar = null, suprimirClique = false;
	function pontoCanvas(e) {
		const r = cv.getBoundingClientRect(), s = Math.max(r.width / cv.width, r.height / cv.height);
		return { x: (e.clientX - r.left - (r.width - cv.width * s) / 2) / s, y: (e.clientY - r.top - (r.height - cv.height * s)) / s };
	}
	function chaoY() { return geo.chao + z; }
	function acertou(p) {
		if (!cfg || !geo) return false;
		if (av.modo === "segurado") return false;
		const pes = chaoY() - av.alt;
		return Math.abs(p.x - av.x) < 9 * z && p.y <= pes + 2 * z && p.y >= pes - 34 * z;
	}
	const CONTROLE = "a:not(#abrirTrilha),button,input,select,textarea,label,.offcanvas,.gardenHud,.gardenFloatingActions,.gardenLeftActions,.gardenToast";
	raiz.addEventListener("pointerdown", function (e) {
		if (e.button !== 0 || pegar || e.target.closest(CONTROLE)) return;
		const p = pontoCanvas(e); if (!acertou(p)) return;
		e.preventDefault();
		suprimirClique = true;
		raiz.setPointerCapture(e.pointerId);
		pegar = { x0: e.clientX, y0: e.clientY, moveu: false, p: p, t: performance.now(), vx: 0, vy: 0, vxAnt: 0 };
	});
	raiz.addEventListener("pointermove", function (e) {
		const p = pontoCanvas(e);
		if (!pegar) { raiz.style.cursor = acertou(p) ? "grab" : ""; return; }
		if (!pegar.moveu && Math.hypot(e.clientX - pegar.x0, e.clientY - pegar.y0) < 6) return;
		const agora = performance.now(), dt = Math.max(.008, (agora - pegar.t) / 1000);
		if (!pegar.moveu) {
			pegar.moveu = true; terminar(); av.alvo = null; av.modo = "segurado"; av.ang = 0; av.w = 0; av.fala = null;
			av.hx = p.x; av.hy = p.y; raiz.style.cursor = "grabbing";
		}
		pegar.vx = pegar.vx * .5 + (p.x - pegar.p.x) / dt * .5; pegar.vy = pegar.vy * .5 + (p.y - pegar.p.y) / dt * .5;
		pegar.p = p; pegar.t = agora; av.hx = p.x; av.hy = p.y;
	});
	function largar() {
		if (!pegar) return;
		const g = pegar; pegar = null; raiz.style.cursor = "";
		setTimeout(function () { suprimirClique = false; }, 0);
		if (!g.moveu || av.modo !== "segurado") return;
		av.modo = "caindo"; av.vx = Math.max(-60 * z, Math.min(60 * z, g.vx)); av.vy = Math.max(-40 * z, Math.min(40 * z, g.vy));
		av.reclamar = av.alt > 14 * z;   // de uma altura de ~metade do personagem para cima, o tombo rende reclamação
		if (reduzir) { av.alt = 0; pousar(); }
	}
	raiz.addEventListener("pointerup", largar);
	raiz.addEventListener("pointercancel", largar);
	raiz.addEventListener("lostpointercapture", largar);
	raiz.addEventListener("click", function (e) { if (suprimirClique && e.detail !== 0) { e.preventDefault(); e.stopPropagation(); } }, true);

	function pousar() {
		av.alt = 0; av.vx = av.vy = 0;
		if (av.reclamar) {
			av.modo = "pouso"; av.t = 0; falar(sorteio(RECLAMA), 2800);
			for (let k = 0; k < 10; k++) poeira.push({ x: av.x + (Math.random() - .5) * 10 * z, y: chaoY() - z, vx: (Math.random() - .5) * 60 * z, vy: -Math.random() * 18 * z, vida: .5 + Math.random() * .3 });
		} else { av.modo = "solo"; av.espera = 1.2; pose("parado"); }
	}
	function fisica(dt) {
		if (av.modo === "segurado") {
			// pêndulo preso no ponteiro: balança conforme a aceleração horizontal da mão
			const vxp = (av.hx - (av.hxAnt === undefined ? av.hx : av.hxAnt)) / Math.max(dt, .008); av.hxAnt = av.hx;
			const ax = Math.max(-500 * z, Math.min(500 * z, (vxp - (av.vxpAnt || 0)) / Math.max(dt, .008))); av.vxpAnt = vxp;
			const L = 18 * z, G = 150 * z;
			av.w += (-(G / L) * Math.sin(av.ang) - 4 * av.w - (ax / L) * Math.cos(av.ang)) * dt;
			av.ang = Math.max(-1.1, Math.min(1.1, av.ang + av.w * dt));
			av.x = av.hx; av.alt = Math.max(0, chaoY() - (av.hy + (C.CHAO - 4) * z));
		} else if (av.modo === "caindo") {
			av.vy += 160 * z * dt; av.x += av.vx * dt; av.alt -= av.vy * dt; av.ang += -av.ang * 5 * dt;
			const l0 = geo.W / 2 - geo.visivelW / 2 + 10 * z, l1 = geo.W / 2 + geo.visivelW / 2 - 10 * z;
			if (av.x < l0 || av.x > l1) { av.x = Math.max(l0, Math.min(l1, av.x)); av.vx *= -.4; }
			if (av.alt <= 0) pousar();
		} else if (av.modo === "pouso") {
			av.t += dt; if (av.t >= .45) { av.modo = "solo"; av.espera = 2.5; pose("parado"); }
		}
	}

	// balão de fala em pixel art, sempre dentro da área visível
	function balao(ctx, texto, cx, topoCabeca) {
		const r = cv.getBoundingClientRect(), s = Math.max(r.width / cv.width, r.height / cv.height) || 1;
		const u = 2 / s, fonte = 14 / s, pad = 11 / s;
		ctx.save(); ctx.font = "600 " + fonte + "px Arial, Helvetica, sans-serif"; ctx.textAlign = "center"; ctx.textBaseline = "middle";
		const bw = ctx.measureText(texto).width + 2 * pad, bh = fonte * 1.4 + pad;
		const vl = geo.W / 2 - geo.visivelW / 2 + 6 * u, vr = geo.W / 2 + geo.visivelW / 2 - 6 * u, vt = cv.height - geo.visivelH + 6 * u;
		const x = Math.max(vl, Math.min(vr - bw, cx - bw / 2)), y = Math.max(vt, topoCabeca - bh - 16 / s);
		// mesma estética da caixa de mensagem do assistente: verde-escuro, filete verde-claro por dentro e texto creme
		ctx.fillStyle = "#0c2217"; ctx.fillRect(x, y - u, bw, bh + 2 * u); ctx.fillRect(x - u, y, bw + 2 * u, bh);
		ctx.fillStyle = "#163d2b"; ctx.fillRect(x, y, bw, bh);
		ctx.fillStyle = "#b8d38d"; ctx.fillRect(x + u, y + u, bw - 2 * u, u); ctx.fillRect(x + u, y + bh - 2 * u, bw - 2 * u, u);
		ctx.fillRect(x + u, y + u, u, bh - 2 * u); ctx.fillRect(x + bw - 2 * u, y + u, u, bh - 2 * u);
		const tx = Math.max(x + 3 * u, Math.min(x + bw - 6 * u, cx - 2 * u));
		ctx.fillStyle = "#0c2217"; ctx.fillRect(tx - u, y + bh, 5 * u, u); ctx.fillRect(tx, y + bh + u, 3 * u, u); ctx.fillRect(tx + u, y + bh + 2 * u, u, u);
		ctx.fillStyle = "#163d2b"; ctx.fillRect(tx, y + bh, 3 * u, u); ctx.fillRect(tx + u, y + bh + u, u, u);
		ctx.fillStyle = "#f3f1df"; ctx.fillText(texto, x + bw / 2, y + bh / 2 + u / 2);
		ctx.restore();
	}

	function desenhar(ctx, g) {
		geo = g; if (!cfg) return;
		escala();
		const agora = performance.now(), dt = ultimo ? Math.min(.05, (agora - ultimo) / 1000) : 0; ultimo = agora;
		if (av.x === null) av.x = Math.max(limites()[0], g.W / 2 - 24 * z);
		av.rel += reduzir ? 0 : dt;
		if (av.modo !== "solo") fisica(dt);
		else if (!reduzir) { if (av.rega) atualizarRega(dt); else passear(dt); }
		const chao = g.chao + z, pes = chao - av.alt, w = C.W * z, piscar = (av.rel % 3.2) > 3.05 ? 1 : 0;
		// sombra: diminui quanto mais alto ele está
		const el = elipse(8, 2), f = Math.max(.35, 1 - av.alt / (70 * z)), ox = av.modo === "solo" && av.pose !== "parado" ? av.dir * z : z * .5;
		ctx.save(); ctx.globalAlpha = av.modo === "solo" ? 1 : .55 + .45 * f;
		ctx.drawImage(el, Math.round(av.x + ox - 8.5 * z * f), Math.round(chao - 3.5 * z), el.width * z * f, el.height * z);
		ctx.restore();
		let topoCabeca = pes - 37 * z;
		if (av.modo === "segurado") {
			const s = spr("parado", piscar);
			ctx.save(); ctx.translate(Math.round(av.hx), Math.round(av.hy)); ctx.rotate(av.ang);
			ctx.drawImage(s.c, -w / 2, -4 * z, w, C.H * z); ctx.restore();
			if (Math.abs(av.ang) > .35 && !reduzir) { ctx.fillStyle = "#9fd8f0"; ctx.fillRect(av.hx + (av.ang > 0 ? 8 : -9) * z, av.hy + 2 * z, z, 2 * z); }
			topoCabeca = av.hy - 2 * z;
		} else if (av.modo === "caindo") {
			const s = spr("caindo", reduzir ? 0 : Math.floor(av.rel * 8) % 2), meio = pes - 19 * z;
			ctx.save(); ctx.translate(Math.round(av.x), Math.round(meio)); ctx.rotate(av.ang * .6 + (reduzir ? 0 : Math.sin(av.rel * 20) * .06));
			ctx.drawImage(s.c, -w / 2, -(C.CHAO - 19) * z, w, C.H * z); ctx.restore();
		} else if (av.modo === "pouso") {
			const k = Math.min(1, av.t / .45), sy = .76 + .24 * k, sx = 1.2 - .2 * k, s = spr("parado", 1);
			ctx.save(); ctx.translate(Math.round(av.x), Math.round(pes)); ctx.scale(sx, sy);
			ctx.drawImage(s.c, -w / 2, -C.CHAO * z, w, C.H * z); ctx.restore();
		} else {
			const e = av.pose;
			const i = e === "andar" ? Math.floor(av.rel * 6) % 4 : e === "regar" ? ROTEIRO[Math.min(av.rega.fase, 4)].q : piscar;
			const s = spr(e, i), top = chao - C.CHAO * z, d = e === "parado" ? 1 : av.dir;
			ctx.save(); ctx.translate(Math.round(av.x), Math.round(top)); if (d < 0) ctx.scale(-1, 1); ctx.drawImage(s.c, -w / 2, 0, w, C.H * z); ctx.restore();
			if (e === "regar" && s.bico && av.rega && av.rega.fase >= 1 && av.rega.fase <= 2) {
				av.rega.acc += dt * (i === 2 ? 22 : 6);
				const bx = d > 0 ? av.x - w / 2 + (s.bico[0] + .5) * z : av.x + w / 2 - (s.bico[0] + .5) * z, by = top + (s.bico[1] + .5) * z, u = z / 4;
				while (av.rega.acc >= 1) { av.rega.acc--; gotas.push({ x: bx, y: by, vx: d * (20 + Math.random() * 30) * u, vy: (30 + Math.random() * 40) * u, vida: 2 }); }
			}
		}
		// gotas e respingos
		const solo = g.chao - z, u = z / 4, novas = [];
		for (const q of gotas) {
			q.vida -= dt; q.vy += (q.resp ? 1400 : 2200) * u * dt; q.x += q.vx * dt; q.y += q.vy * dt;
			if (!q.resp && q.y >= solo) { q.vida = 0; for (let k = 0; k < 2; k++) novas.push({ x: q.x, y: solo, vx: (Math.random() - .5) * 140 * u, vy: (-110 - Math.random() * 60) * u, vida: .3, resp: true }); }
		}
		gotas = gotas.filter(function (q) { return q.vida > 0; }).concat(novas);
		for (const q of gotas) { ctx.fillStyle = q.resp ? "#cdeefa" : "#9fd8f0"; ctx.fillRect(Math.round(q.x / z) * z, Math.round(q.y / z) * z, z, q.resp ? z : 2 * z); }
		// poeira do tombo
		for (const q of poeira) { q.vida -= dt; q.vy += 40 * z * dt; q.x += q.vx * dt; q.y = Math.min(chao - z, q.y + q.vy * dt); }
		poeira = poeira.filter(function (q) { return q.vida > 0; });
		for (const q of poeira) { ctx.fillStyle = "rgba(232,220,192," + Math.min(1, q.vida * 2) + ")"; ctx.fillRect(Math.round(q.x / z) * z, Math.round(q.y / z) * z, z, z); }
		if (av.fala && agora < av.fala.ate) balao(ctx, av.fala.texto, av.x, topoCabeca); else av.fala = null;
	}
	window.raizAvatarHome = {
		desenhar: desenhar,
		ativo: function () { return !!cfg; },
		regar: function () { return cfg && geo && !reduzir && !document.hidden ? regar() : Promise.resolve(); },
		trocar: function (n) { cfg = C.validar(n); cache = {}; }
	};
	fetch("/api/avatar/me").then(function (r) { return r.ok ? r.json() : null; })
		.then(function (corpo) { if (corpo && corpo.avatar) window.raizAvatarHome.trocar(corpo.avatar); })
		.catch(function () { /* sem personagem, a home continua com as gotas */ });
})();
