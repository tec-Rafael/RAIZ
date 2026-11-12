/*
 * Personagem do usuário no jardim (home), com o sprite de avatarSprite.js (window.RaizAvatar).
 * O avatar vem de GET /api/avatar/me; enquanto não chega (ou se falhar), o personagem não aparece e a
 * árvore volta a usar as gotas d'água.
 * Parado = de frente (pisca); andar e regar = de perfil. A cada evolução de estágio ele vai até a árvore
 * e rega; o crescimento espera a promessa de regar() terminar. Não tem loop próprio: o homeArvore.js chama
 * raizAvatarHome.desenhar(ctx, geo) a cada quadro, depois da árvore.
 * Tamanho: 30% da altura visível (limitado a 32% da largura). Sombra de contato em pixel art sob os pés.
 */
(function () {
	"use strict";
	const cv = document.getElementById("canvasArvoreHome"), C = window.RaizAvatar;
	if (!cv || !C) return;
	const FIG = 32, ALTURA = 0.30;
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

	const av = { x: null, dir: 1, pose: "parado", rel: 0, espera: 4, alvo: null, rega: null };
	let geo = null, z = 12, gotas = [], ultimo = 0;
	function escala() { z = Math.max(3, Math.min(Math.round(geo.visivelH * ALTURA / FIG), Math.floor(geo.visivelW * 0.32 / 16))); }
	function limites() { const m = Math.max(6 * z, geo.visivelW / 2 - 16 * z); return [geo.W / 2 - m, geo.W / 2 + m]; }
	function pose(p) { if (av.pose !== p) { av.pose = p; av.rel = 0; } }
	function andarAte(x, dt) {
		const d = x - av.x, p = 20 * z * dt; if (Math.abs(d) <= p) { av.x = x; return true; }
		av.dir = d > 0 ? 1 : -1; av.x += av.dir * p; pose("andar"); return false;
	}
	function passear(dt) {
		if (av.alvo === null) {
			pose("parado"); av.espera -= dt;
			if (av.espera <= 0) { const l = limites(); let x, n = 0; do { x = l[0] + Math.random() * (l[1] - l[0]); } while (Math.abs(x - av.x) < Math.min(30 * z, (l[1] - l[0]) / 3) && ++n < 8); av.alvo = x; }
		} else if (andarAte(av.alvo, dt)) { av.alvo = null; av.espera = 1.5 + Math.random() * 3; }
	}
	// rega: ir até a árvore -> segurar -> erguer -> despejar -> baixar
	const ROTEIRO = [{ q: 0, t: .25 }, { q: 1, t: .25 }, { q: 2, t: 1.4 }, { q: 1, t: .2 }, { q: 0, t: .2 }];
	function regar() {
		if (av.rega) return av.rega.promessa;
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
	function desenhar(ctx, g) {
		geo = g; if (!cfg) return;
		escala();
		const agora = performance.now(), dt = ultimo ? Math.min(.05, (agora - ultimo) / 1000) : 0; ultimo = agora;
		if (av.x === null) av.x = Math.max(limites()[0], g.W / 2 - 24 * z);
		if (!reduzir) { av.rel += dt; if (av.rega) atualizarRega(dt); else passear(dt); }
		const chao = g.chao + z, e = av.pose;
		const i = e === "andar" ? Math.floor(av.rel * 6) % 4 : e === "regar" ? ROTEIRO[Math.min(av.rega.fase, 4)].q : ((av.rel % 3.2) > 3.05 ? 1 : 0);
		const s = spr(e, i), w = C.W * z, top = chao - C.CHAO * z, d = e === "parado" ? 1 : av.dir;
		const el = elipse(8, 2), ox = e === "parado" ? z * .5 : av.dir * z;
		ctx.drawImage(el, Math.round(av.x + ox - 8.5 * z), Math.round(chao - 3.5 * z), el.width * z, el.height * z);
		ctx.save(); ctx.translate(Math.round(av.x), Math.round(top)); if (d < 0) ctx.scale(-1, 1); ctx.drawImage(s.c, -w / 2, 0, w, C.H * z); ctx.restore();
		if (e === "regar" && s.bico && av.rega && av.rega.fase >= 1 && av.rega.fase <= 2) {
			av.rega.acc += dt * (i === 2 ? 22 : 6);
			const bx = d > 0 ? av.x - w / 2 + (s.bico[0] + .5) * z : av.x + w / 2 - (s.bico[0] + .5) * z, by = top + (s.bico[1] + .5) * z, u = z / 4;
			while (av.rega.acc >= 1) { av.rega.acc--; gotas.push({ x: bx, y: by, vx: d * (20 + Math.random() * 30) * u, vy: (30 + Math.random() * 40) * u, vida: 2 }); }
		}
		// gotas e respingos
		const solo = g.chao - z, u = z / 4, novas = [];
		for (const q of gotas) {
			q.vida -= dt; q.vy += (q.resp ? 1400 : 2200) * u * dt; q.x += q.vx * dt; q.y += q.vy * dt;
			if (!q.resp && q.y >= solo) { q.vida = 0; for (let k = 0; k < 2; k++) novas.push({ x: q.x, y: solo, vx: (Math.random() - .5) * 140 * u, vy: (-110 - Math.random() * 60) * u, vida: .3, resp: true }); }
		}
		gotas = gotas.filter(function (q) { return q.vida > 0; }).concat(novas);
		for (const q of gotas) { ctx.fillStyle = q.resp ? "#cdeefa" : "#9fd8f0"; ctx.fillRect(Math.round(q.x / z) * z, Math.round(q.y / z) * z, z, q.resp ? z : 2 * z); }
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
