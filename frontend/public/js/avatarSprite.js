/*
 * Avatar rAIz (window.RaizAvatar) — sprite em pixel art do avatar do usuário, gerado por código a partir
 * de uma configuração salva (JSON versao 2). Versão escolhida: cabeça menor e corpo mais longo.
 * Hoje é usado na animação da tela de login (avatar fixo); será usado também na home e no criador.
 * Pixel art em baixa resolução (grade 20x34), cabeça grande, contorno escuro COLORIDO
 * (tom escuro do próprio material, nunca preto), rosto com olhos, boca e bochechas.
 * Parado = de frente; andar e regar = de perfil (virado para a direita; dir = -1 espelha).
 *
 * Mapas em texto, 1 caractere = 1 pixel:
 *   pele l s S | camisa v c C | calça q p P | sapato n b B | cabelo k h H
 *   o = contorno (tom escuro do material vizinho; nas camadas de cabelo/barba = contorno do cabelo)
 *   w branco do olho, e íris, E pupila, r bochecha, m boca, f sarda, t cinto, . vazio
 */
(function (G) {
	"use strict";
	const W = 28, H = 38, CHAO = 37, OX = 4;   // protótipo: pés na linha 36;   // pés na linha 33; mapas de 20 colunas desenhados com OX de margem (espaço p/ o regador)

	// ------------------------------------------------------------------ paletas (iguais às do avatar atual)
	const PALETAS = {
		pele: [["p1", "Porcelana", "#f6d7c3"], ["p2", "Clara", "#efc2a0"], ["p3", "Pêssego", "#e3a982"], ["p4", "Dourada", "#c98c5c"],
			["p5", "Morena", "#a86f45"], ["p6", "Canela", "#8a5634"], ["p7", "Escura", "#6b3f26"], ["p8", "Retinta", "#4a2a1a"]],
		cabelo: [["c1", "Preto", "#2b2730"], ["c2", "Castanho escuro", "#4a2e22"], ["c3", "Castanho", "#7a4a2c"], ["c4", "Castanho claro", "#a8713f"],
			["c5", "Loiro", "#e2b65a"], ["c6", "Platinado", "#efe2c2"], ["c7", "Ruivo", "#c4581f"], ["c8", "Vermelho", "#b52b3a"],
			["c9", "Grisalho", "#a7a5a8"], ["c10", "Azul", "#3b6fc4"], ["c11", "Rosa", "#e27aa8"], ["c12", "Verde", "#3f9a55"]],
		olhos: [["o1", "Castanho", "#5a3420"], ["o2", "Preto", "#1f1b22"], ["o3", "Mel", "#a8742c"], ["o4", "Verde", "#3f8a4a"],
			["o5", "Azul", "#3b78c4"], ["o6", "Cinza", "#7b8794"], ["o7", "Violeta", "#7a4fb0"], ["o8", "Âmbar", "#d08a1c"]],
		camisa: [["r1", "Verde rAIz", "#1e7349"], ["r2", "Framboesa", "#d32e5e"], ["r3", "Azul", "#2f63b8"], ["r4", "Céu", "#5aa3d6"],
			["r5", "Mostarda", "#d9aa2b"], ["r6", "Laranja", "#e07a2c"], ["r7", "Lilás", "#8e6ac4"], ["r8", "Rosa", "#ef9ab8"],
			["r9", "Branco", "#eceae4"], ["r10", "Grafite", "#3d4048"], ["r11", "Vinho", "#7a2436"], ["r12", "Menta", "#7cc8a0"]],
		calca: [["k1", "Jeans", "#33508a"], ["k2", "Jeans claro", "#5f80b8"], ["k3", "Preto", "#2c2b31"], ["k4", "Cáqui", "#a8925f"],
			["k5", "Marrom", "#6a4630"], ["k6", "Verde oliva", "#56602f"], ["k7", "Cinza", "#7a7c84"], ["k8", "Bege", "#d6c49c"]]
	};
	for (const k in PALETAS) PALETAS[k] = PALETAS[k].map(function (a) { return { id: a[0], nome: a[1], cor: a[2] }; });
	const CABELOS = [["careca", "Careca"], ["curto", "Curto"], ["topete", "Topete"], ["espetado", "Espetado"], ["cacheado", "Cacheado"],
		["chanel", "Chanel"], ["longo", "Longo"], ["rabo", "Rabo de cavalo"], ["coque", "Coque"], ["moicano", "Moicano"]].map(function (a) { return { id: a[0], nome: a[1] }; });
	const FACIAIS = [["nenhum", "Nenhum"], ["bigode", "Bigode"], ["cavanhaque", "Cavanhaque"], ["barbaCurta", "Barba curta"], ["barbaCheia", "Barba cheia"]].map(function (a) { return { id: a[0], nome: a[1] }; });
	const GENEROS = [{ id: "masculino", nome: "Masculino" }, { id: "feminino", nome: "Feminino" }];

	// ------------------------------------------------------------------ cores
	function hexRgb(h) { h = h.replace("#", ""); return [0, 2, 4].map(function (i) { return parseInt(h.substr(i, 2), 16); }); }
	function rgbHsl(r, g, b) {
		r /= 255; g /= 255; b /= 255; const mx = Math.max(r, g, b), mn = Math.min(r, g, b), l = (mx + mn) / 2; let h = 0, s = 0;
		if (mx !== mn) { const d = mx - mn; s = l > 0.5 ? d / (2 - mx - mn) : d / (mx + mn);
			h = mx === r ? (g - b) / d + (g < b ? 6 : 0) : mx === g ? (b - r) / d + 2 : (r - g) / d + 4; h /= 6; }
		return [h, s, l];
	}
	function hslRgb(h, s, l) {
		h = ((h % 1) + 1) % 1; if (s === 0) return [l, l, l].map(function (v) { return Math.round(v * 255); });
		const f = function (p, q, t) { if (t < 0) t += 1; if (t > 1) t -= 1; if (t < 1 / 6) return p + (q - p) * 6 * t; if (t < 1 / 2) return q; if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6; return p; };
		const q = l < 0.5 ? l * (1 + s) : l + s - l * s, p = 2 * l - q;
		return [f(p, q, h + 1 / 3), f(p, q, h), f(p, q, h - 1 / 3)].map(function (v) { return Math.round(v * 255); });
	}
	const lim = function (v, a, b) { return Math.max(a, Math.min(b, v)); };
	// 4 tons: luz, base, sombra, contorno (escuro e levemente puxado para o vermelho/roxo, como na referência)
	function rampa(hex) {
		const c = hexRgb(hex), x = rgbHsl(c[0], c[1], c[2]), h = x[0], s = x[1], l = x[2];
		return [
			hslRgb(h + 0.012, s * 0.95, lim(l + 0.1, 0, 0.95)),
			c,
			hslRgb(h - 0.02, lim(s * 0.95 + 0.03, 0, 1), l - Math.min(0.12, l * 0.28)),
			hslRgb(h - 0.04, lim(s * 0.7 + 0.1, 0, 0.7), lim(l * 0.34, 0.035, 0.24))
		];
	}
	function mistura(a, b, t) { return [0, 1, 2].map(function (i) { return Math.round(a[i] + (b[i] - a[i]) * t); }); }
	const BRANCO = [250, 248, 242];
	const SAPATO = rampa("#6a4330"), CINTO = rampa("#4a3024"), REGADOR = rampa("#5f8fa8");

	const LETRA = {
		l: ["pele", 0], s: ["pele", 1], S: ["pele", 2], v: ["camisa", 0], c: ["camisa", 1], C: ["camisa", 2],
		q: ["calca", 0], p: ["calca", 1], P: ["calca", 2], n: ["sapato", 0], b: ["sapato", 1], B: ["sapato", 2],
		k: ["cabelo", 0], h: ["cabelo", 1], H: ["cabelo", 2], t: ["cinto", 1], T: ["cinto", 0],
		g: ["regador", 0], G: ["regador", 1], j: ["regador", 2]
	};
	const ESPECIAL = { w: 1, e: 1, E: 1, r: 1, m: 1, f: 1 };

	// ------------------------------------------------------------------ mapas: FRENTE
	const CABECA_FRENTE = [
		"......oooooooo......",
		"....oossllssssoo....",
		"...osllssssssssso...",
		"...oslssssssssSso...",
		"...osssssssssssSo...",
		"...osssssssssssSo...",
		"...osssssssssssSo...",
		"...osswEssssEwsSo...",
		"...osswessssewsSo...",
		"...osrrssssssrrSo...",
		"...oSssssmmsssSSo...",
		"....oSSsssssSSSo....",
		"......oooooooo......"];             // linhas 4..16
	const OLHOS_FECHADOS = { 11: "...ossssssssssSSo...", 12: "...ossEEssssEEsSo..." };
	const CILIOS = [[6, 10], [13, 10]];   // feminino: um pixel de cílio acima do canto externo
	const CORPO_FRENTE = {
		masculino: [
			".......oSSSSo.......",
			"....oocccSScccoo....",
			"...ovccccccccccCo...",
			"...ovcCccccccCcCo...",
			"...ovcCccccccCcCo...",
			"...olsoccccccoSSo...",
			"...olsoccccCCoSSo...",
			"...olsottttttoSSo...",
			"...ossoppppPPoSSo...",
			"....ooopppppPooo....",
			"......oppPpPPo......",
			"......oppPpPPo......",
			"......opPoopPo......",
			"......opPoopPo......",
			"......opPoopPo......",
			".....onbBoonbBo.....",
			".....oooo..oooo....."],          // linhas 17..33
		feminino: [
			".......oSSSSo.......",
			".....ooccSSccoo.....",
			"....ovccccccccCo....",
			"....ovoccccccoCo....",
			"....ovoccccCCoCo....",
			"....osoocccCooSo....",
			"....osoocccCooSo....",
			"....osoottttooSo....",
			"....osopppppPoSo....",
			"....ooopppppPooo....",
			"......oppPpPPo......",
			"......oppPpPPo......",
			"......opPoopPo......",
			"......opPoopPo......",
			"......opPoopPo......",
			".....onbBoonbBo.....",
			".....oooo..oooo....."]
	};

	// ------------------------------------------------------------------ mapas: PERFIL (virado para a direita)
	const CABECA_PERFIL = [
		"......oooooooo......",
		"....oossllssssoo....",
		"...osllssssssssso...",
		"...oslsssssssssso...",
		"...osssssssssssso...",
		"...osssssssssssso...",
		"...osssssSSsssssso..",
		"...osssssSlsssEsso..",
		"...osssssSSsssessso.",
		"...oSssssssssrrsso..",
		"...oSSsssssssmsso...",
		"....oSSSsssssssso...",
		".....ooooooooooo...."];             // linhas 4..16 (orelha em 9..11, olho na coluna 14, nariz na 18)
	// tronco de perfil (dy = 0 no contato; -1 na passagem)
	const TRONCO_PERFIL = [
		".......oSSSo........",
		"......occcccco......",
		"......ovccccCo......",
		"......ovccccCo......",
		"......ovccccCo......",
		"......ovccccCo......",
		"......ovcccCCo......",
		"......ottttTto......",
		"......oqpppPPo......",
		"......oqpppPPo......"];             // linhas 17..26
	// pernas: 1/2 = calça, 3/4/5 = sapato (base/sombra/luz); a de trás usa tons mais escuros
	const PERNAS = {
		frente: { y: 27, l: [
			"........o112o.......",
			"........o112o.......",
			".........o112o......",
			".........o112o......",
			"..........o112o.....",
			"..........o5334o....",
			"..........oooooo...."] },
		tras: { y: 27, l: [
			".......o112o........",
			".......o112o........",
			"......o112o.........",
			"......o112o.........",
			".....o112o..........",
			"....o5334o..........",
			"....oooooo.........."] },
		reta: { y: 26, l: [
			"........o112o.......",
			"........o112o.......",
			"........o112o.......",
			"........o112o.......",
			"........o112o.......",
			"........o112o.......",
			"........o5334o......",
			"........oooooo......"] },
		erguida: { y: 26, l: [
			".......o112o........",
			".......o112o........",
			".......o112o........",
			"......o112o.........",
			".....o112o..........",
			".....o5334o.........",
			".....oooooo........."] }
	};
	const BRACOS = {
		reto: { y: 19, l: [
			".......ovcCo........",
			".......ovcCo........",
			".......ovcCo........",
			"........olSo........",
			"........osSo........",
			"........osSo........",
			"........oSSo........",
			".........oo........."] },
		frente: { y: 19, l: [
			".......ovcCo........",
			".......ovcCo........",
			"........ovcCo.......",
			".........olSo.......",
			".........osSo.......",
			"..........osSo......",
			"..........oSSo......",
			"...........oo......."] },
		tras: { y: 19, l: [
			".......ovcCo........",
			".......ovcCo........",
			"......ovcCo.........",
			"......olSo..........",
			".....olSo...........",
			".....osSo...........",
			".....oSSo...........",
			"......oo............"] }
	};
	BRACOS.erguido = { y: 19, l: [
			".......ovcCo........",
			".......ovccCoooo....",
			"........ocCClsSSo...",
			".........ooooSSo....",
			"............oo......"] };
	// regador (fora do quadro de 20 colunas: usa a margem OX). Âncora = topo da alça, onde fica a mão.
	const REGADOR_MAPAS = [
		{ ancora: [4, 1], bico: [11, 2], l: [
			"...ooo......",
			"..o...o.....",
			".ooooooo..oo",
			".ogGGGjo.oGo",
			".ogGGGjooGo.",
			".ogGGGjGGo..",
			".ogGGGjoo...",
			".ooooooo...."] },
		{ ancora: [3, 1], bico: [11, 8], l: [
			"..ooo.......",
			".o...o......",
			".oooooo.....",
			".ogGGGjo....",
			".ogGGGGjo...",
			"..ogGGGjGo..",
			"...ojjjoGGo.",
			"....ooo..oGo",
			"..........oo"] }];
	// o regador é sempre o mesmo desenho (o bico não muda); só o braço muda e, no quadro 2, a água sai com mais força
	const REGAR = [{ b: "frente", r: 0 }, { b: "erguido", r: 0 }, { b: "erguido", r: 0 }];
	function maoDe(br) {   // pixel de pele mais à frente/abaixo do braço = mão
		let m = null;
		br.l.forEach(function (s, y) { for (let x = 0; x < s.length; x++) if ("lsS".indexOf(s[x]) >= 0 && (!m || x + y > m[0] + m[1] - br.y)) m = [x, br.y + y]; });
		return m;
	}
	const FRENTE_COR = { 1: "p", 2: "P", 3: "b", 4: "B", 5: "n" };
	const TRAS_COR = { 1: "P", 2: "P", 3: "B", 4: "B", 5: "b", v: "C", c: "C", l: "S", s: "S" };
	function trocar(l, mapa) { return l.map(function (s) { return s.replace(/./g, function (c) { return mapa[c] || c; }); }); }
	// caminhada de 4 quadros: contato, passagem, contato (pernas trocadas), passagem
	const ANDAR = [
		{ pf: "frente", pt: "tras", bf: "tras", bt: "frente", dy: 0 },
		{ pf: "reta", pt: "erguida", bf: "reto", bt: "reto", dy: -1 },
		{ pf: "tras", pt: "frente", bf: "frente", bt: "tras", dy: 0 },
		{ pf: "erguida", pt: "reta", bf: "reto", bt: "reto", dy: -1 }];
	const SARDAS_PERFIL = [[12, 13], [15, 12], [12, 14]];

	// ------------------------------------------------------------------ cabelos e barbas (texto: "@nome[.tras] linhaInicial")
	function lerBlocos(txt) {
		const r = {};
		txt.split("@").slice(1).forEach(function (b) {
			const ls = b.replace(/\s+$/, "").split("\n"), cab = ls[0].trim().split(/\s+/);
			r[cab[0]] = { y: +cab[1], l: ls.slice(1) };
		});
		return r;
	}
	// <TXT>
	const TXT_FRENTE = `
@curto 3
......oooooooo......
....oohhkkkkhhoo....
...ohkkkkhhhhhhho...
..ohkkkhhhhhhhhhHo..
..ohkhhhhhhhhhhhHo..
..ohhhhhhhhhhhhHHo..
..ohhhHhhhhHhhhHHo..
..ohhHohhHohhHohHo..
..oHHo.oo.o.oo.oHo..
...oo...........oo..
@topete 1
.......oooooo.......
.....oohkkkkhoo.....
....ohkkkkhhhhHo....
...ohkkhhhhhhhhHo...
..ohkkhhhhhhhhhhHo..
..ohkhhhhhhhhhhhHo..
..ohhhhhhhhhhhhhHo..
..ohhhhhhhhhhhhHHo..
..ohhHhhhhhhhhHHHo..
..oHHoooooohhHoHHo..
..oHHo......oo.oHo..
...oo...........oo..
@espetado 0
.....o...o...o......
....oko.oko.oho.o...
...okhookhhohhhoho..
...ohkkkhhhhhhhhHo..
..ohkkhhhhhhhhhhHHo.
..ohkhhhhhhhhhhhhHo.
..ohhhhhhhhhhhhhHo..
.ohhhhhhhhhhhhhhHHo.
..ohhhhhhhhhhhhhHo..
..ohhHhHhhhHhHhhHo..
..oHHooHooHoHooHHo..
..oHHo.o..o.o..oHo..
...oo...........oo..
@cacheado 1
.....oo.oooo.oo.....
....ohkokkkhohho....
...ohkkkkhhhhhhHo...
..ohkkhkhhhhhhhhHo..
.ohkkhhhhhhhhhhhhHo.
.ohkhhhhhhhhhhhhhHo.
ohkhhhhhhhhhhhhhhHHo
ohhhhhhhhHhhhhHhhhHo
ohhhHhHhHoHhHhoHhHHo
ohhHo.o.o..o.o.oHhHo
ohhHo..........oHhHo
ohHHo..........oHHHo
.oHHo..........oHHo.
.ohHHo........oHHHo.
..oHo..........oHo..
...o............o...
@chanel 3
......oooooooo......
....oohhkkkkhhoo....
...ohkkkkhhhhhhho...
..ohkkhhhhhhhhhhHo..
..ohkhhhhhhhhhhhHo..
..ohhhhhhhhhhhhhHo..
..ohhhhhhhhhhhhhHo..
..ohhhhhhhhhhhhhHo..
..ohhoooooooooohHo..
..ohho........ohHo..
..ohHo........oHHo..
..ohHo........oHHo..
..ohHHo......oHHHo..
..oHHHo......oHHHo..
...ooo........ooo...
@longo 3
......oooooooo......
....oohhkkkkhhoo....
...ohkkkkhhhhhhho...
..ohkkhhhhhhhhhhHo..
..ohkhhhhhhhhhhhHo..
..ohhhhhhhoHhhhhHo..
..ohhhhhhHohhhhhHo..
..ohhhhhHo.ohhhhHo..
..ohhhhHo...ohhhHo..
..ohhHo......oHhHo..
..ohho........ohHo..
..ohHo........oHHo..
..ohHo........oHHo..
..ohHo........oHHo..
@longo.tras 13
..ohhhhhhhhhhhhhHo..
..ohhhhhhhhhhhhhHo..
..ohhhhhhhhhhhhhHo..
..ohhhhhhhhhhhhhHo..
..ohhhhhhhhhhhhhHo..
..ohhhhhhhhhhhhhHo..
..ohhhhhhhhhhhhhHo..
..ohhhhhhhhhhhhhHo..
..ohHhhhhhhhhhhHHo..
..oHoHhhhhhhhhHoHo..
...o.oooooooooo.o...
@rabo 3
......oooooooo......
....oohhkkkkhhoo....
...ohkkkkhhhhhhho...
..ohkkhhhhhhhhhhHo..
..ohkhhhhhhhhhhhHo..
..ohhhhhhhhhhhhhHo..
..ohhhhhHHhhhhhHHo..
..ohHHoo..oohhHHHo..
..oHo..........oHo..
...o............o...
@rabo.tras 6
...............ooo..
..............ohhHo.
..............ohhHo.
..............ohHHo.
...............ohHo.
...............ohHo.
...............ohHHo
...............oHHo.
...............oHHo.
...............oHo..
................o...
@coque 0
.......oooooo.......
......ohkkkhho......
......ohkhhhHo......
.....oooHHHHooo.....
....oohhkkkkhhoo....
...ohkkkkhhhhhhho...
..ohkkhhhhhhhhhhHo..
..ohkhhhhhhhhhhhHo..
..ohhhhhhhhhhhhhHo..
..ohhhhhHHhhhhhHHo..
..ohHHoo..oohhHHHo..
..oHo..........oHo..
...o............o...
@moicano 0
........oooo........
.......ohkhHo.......
.......ohkhHo.......
......ohkhhHHo......
......ohkhhHHo......
.......ohhHHo.......
.......ohhHHo.......
........oHHo........
.........oo.........
@bigode 13
.......ohhhho.......
@cavanhaque 13
.......ohhhho.......
....................
.......ohhhho.......
........oHHo........
@barbaCurta 13
....h...hhhh...h....
....hh..h..h..hh....
....ohhhhhhhhhho....
......oHHHHHHo......
.......oooooo.......
@barbaCheia 12
....h..........h....
....hh..hhhh..hh....
....hhh.h..h.hhh....
....ohhhhhhhhhho....
.....ohhhhhhhho.....
......oHHHHHHo......
.......oooooo.......
`;
	const TXT_PERFIL = `
@curto 3
......oooooooo......
....oohhkkkkkhoo....
...ohhkkkkkkkkhho...
..ohhkkhhhhhhhhhho..
..ohhhhhhhhhhhhhhHo.
..ohhhhhhhhhhhhhHHo.
..ohhhhhhhhhhhhHHHo.
..ohhhhho.ooooooo...
..ohhhhHo...........
..ohhhHo............
..oHHHo.............
...ooo..............
@topete 1
..........oooo......
........oohkkkoo....
......oohkkkkhhho...
....oohhkkkkhhhhHo..
...ohhkkkkhhhhhhHHo.
..ohhkkhhhhhhhhhhHo.
..ohhhhhhhhhhhhhHHo.
..ohhhhhhhhhhhhHHo..
..ohhhhhhhhhhHHoo...
..ohhhhho.ooooo.....
..ohhhhHo...........
..ohhhHo............
..oHHHo.............
...ooo..............
@espetado 0
...o....o....o......
..oko..oko..oko.....
..okhoookhooohho....
...ohkkhkkkhhhhhoo..
..ohhkkkkhhhhhhhhho.
..ohkkhhhhhhhhhhhho.
..ohhhhhhhhhhhhhhHo.
..ohhhhhhhhhhhhhhHo.
..ohhhhhhhhhhhhhHHo.
..ohhhhhhhhhhhHoHHo.
..ohhhhho.oooo.o.o..
..ohhhhHo...........
..ohhhHo............
..oHHHo.............
...ooo..............
@cacheado 1
.....oo.oooo.oo.....
....ohkokkkhohho....
...ohkkkkhhhhhhho...
..ohkkhkhhhhhhhhho..
.ohkkhhhhhhhhhhhhho.
ohkhhhhhhhhhhhhhhHo.
ohhhhhhhhhhhhhhhhHo.
ohhhhhhhhhhhhhhhHHo.
ohhhhhhhhhhhhhHoHHo.
ohhhhhhho.ooooo.o...
ohhhHhhHo...........
ohhHohhHo...........
.oHHoHHo............
..oo.oo.............
@chanel 3
......oooooooo......
....oohhkkkkkhoo....
...ohhkkkkkkkkhho...
..ohhkkhhhhhhhhhho..
..ohhhhhhhhhhhhhhHo.
..ohhhhhhhhhhhhhhHo.
..ohhhhhhhhhhhhhHHo.
..ohhhhhhhoooooooo..
..ohhhhhhHo.........
..ohhhhhhHo.........
..ohhhhhhHo.........
..ohhhhhHHo.........
..oHhHhHHHo.........
...oooooooo.........
@longo 3
......oooooooo......
....oohhkkkkkhoo....
...ohhkkkkkkkkhho...
..ohhkkhhhhhhhhhho..
..ohhhhhhhhhhhhhhHo.
..ohhhhhhhhhhhhhhHo.
..ohhhhhhhhhhhhhHHo.
..ohhhhhhhoooooooo..
..ohhhhhhHo.........
..ohhhhhhHo.........
..ohhhhhhHo.........
..ohhhhhHHo.........
..ohhhhhHHo.........
..ohhhhhHo..........
@longo.tras 14
..ohhhhhHHo.........
..ohhhhhHo..........
..ohhhhhHo..........
..ohhhhhHo..........
..ohhhhHHo..........
..ohhhhHo...........
..ohhhhHo...........
..ohhhHHo...........
..ohHhHo............
...oHoHo............
....o.o.............
@rabo 3
......oooooooo......
....oohhkkkkkhoo....
...ohhkkkkkkkkhho...
.oohhkkhhhhhhhhhho..
ohhohhhhhhhhhhhhhHo.
ohhhohhhhhhhhhhhHHo.
ohhhohhhhhhhhHHHo...
ohhHohhhho.oooo.....
ohhHoohhHo..........
.ohHo.ohHo..........
.ohHo..oo...........
.ohHo...............
..oHo...............
..oHo...............
...o................
@coque 0
...oooo.............
..ohkkho............
..ohkhhHo...........
..ohhhHHoooooo......
...oHHoohkkkkhoo....
...ohhkkkkkkkkhho...
..ohhkkhhhhhhhhhho..
..ohhhhhhhhhhhhhhHo.
..ohhhhhhhhhhhhhHHo.
..ohhhhhhhhhhhHHHo..
..ohhhhho.ooooooo...
..ohhhhHo...........
..ohhhHo............
..oHHHo.............
...ooo..............
@moicano 0
.......ooooooo......
......ohkkkkkHo.....
.....ohkkhhhhHHo....
....ohkhhhhhhhHHo...
...ohhhhhhhhhhhHo...
...oHhhhhhhhhhHo....
....oooooooooooo....
@bigode 13
..............hhHo..
@cavanhaque 13
..............hhHo..
....................
.............ohHo...
..............hHo...
..............oo....
@barbaCurta 13
............h.hhHo..
...........hh.......
........ohhhhhhho...
.........oHHHHHHo...
..........oooooo....
@barbaCheia 12
............h.......
...........hh.hhHo..
..........hhh.hHo...
.......ohhhhhhhhHo..
........ohhhhhhHHo..
.........oHHHHHHo...
..........oooooo....
`;
	// </TXT>
	const FRENTE = lerBlocos(TXT_FRENTE), PERFIL = lerBlocos(TXT_PERFIL);
	const SARDAS_FRENTE = [[6, 14], [7, 13], [12, 13], [13, 14]];

	// ------------------------------------------------------------------ PROTÓTIPO: cabeça menor, corpo mais longo
	// A cabeça (e tudo que fica no "quadro" dela: cabelos, barbas, sardas, cílios) perde 2 colunas e 2 linhas
	// de pele lisa; os detalhes (olhos, boca, contornos) ficam intactos. O tronco ganha 1 linha e as pernas 2.
	const CORTE = { linhas: [7, 9], frente: [5, 14], perfil: [6, 12] };
	const acima = function (r) { return CORTE.linhas.filter(function (q) { return q > r; }).length; };
	function cortarBloco(b, cols) {
		const out = []; let y0 = null;
		b.l.forEach(function (s, i) {
			const r = b.y + i; if (CORTE.linhas.indexOf(r) >= 0) return;
			if (y0 === null) y0 = r + acima(r);
			let t = ""; for (let x = 0; x < s.length; x++) if (cols.indexOf(x) < 0) t += s[x];
			out.push("." + t + ".");
		});
		return { y: y0 === null ? b.y : y0, l: out };
	}
	function cortarPonto(p, cols) { return [p[0] - cols.filter(function (c) { return c < p[0]; }).length + 1, p[1] + acima(p[1])]; }
	const CAB_F = cortarBloco({ y: 4, l: CABECA_FRENTE }, CORTE.frente), CAB_P = cortarBloco({ y: 4, l: CABECA_PERFIL }, CORTE.perfil);
	for (const y in OLHOS_FECHADOS) OLHOS_FECHADOS[y] = cortarBloco({ y: +y, l: [OLHOS_FECHADOS[y]] }, CORTE.frente).l[0];
	[CILIOS, SARDAS_FRENTE].forEach(function (L) { L.forEach(function (p, i) { L[i] = cortarPonto(p, CORTE.frente); }); });
	SARDAS_PERFIL.forEach(function (p, i) { SARDAS_PERFIL[i] = cortarPonto(p, CORTE.perfil); });
	const CILIO_P = cortarPonto([15, 10], CORTE.perfil);
	for (const k in FRENTE) FRENTE[k] = cortarBloco(FRENTE[k], CORTE.frente);
	for (const k in PERFIL) PERFIL[k] = cortarBloco(PERFIL[k], CORTE.perfil);
	const dup = function (arr, idx) { arr.splice(idx, 0, arr[idx]); };
	["masculino", "feminino"].forEach(function (g) { const a = CORPO_FRENTE[g]; dup(a, 6); dup(a, 11); dup(a, 14); });
	dup(TRONCO_PERFIL, 5);
	for (const k in BRACOS) if (k !== "erguido") dup(BRACOS[k].l, 4);
	dup(PERNAS.frente.l, 1); dup(PERNAS.frente.l, 4); PERNAS.frente.y += 1;
	dup(PERNAS.tras.l, 1); dup(PERNAS.tras.l, 4); PERNAS.tras.y += 1;
	dup(PERNAS.reta.l, 2); dup(PERNAS.reta.l, 2); PERNAS.reta.y += 1;
	dup(PERNAS.erguida.l, 1); dup(PERNAS.erguida.l, 1); PERNAS.erguida.y += 1;

	// ------------------------------------------------------------------ montagem
	function novaGrade() { const g = []; for (let i = 0; i < W * H; i++) g.push(null); return g; }
	function familia(ch) { return LETRA[ch] ? LETRA[ch][0] : null; }
	// desenha um mapa na grade; "o" vira contorno da família indicada ou da vizinha mais comum no próprio mapa
	function camada(g, linhas, x0, y0, famContorno, espelhar) {
		const h = linhas.length, w = linhas[0].length;
		const ch = function (x, y) { if (x < 0 || y < 0 || x >= w || y >= h) return "."; return linhas[y][espelhar ? w - 1 - x : x]; };
		for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
			const c = ch(x, y), X = x0 + x + OX, Y = y0 + y;
			if (c === "." || c === " " || X < 0 || Y < 0 || X >= W || Y >= H) continue;
			if (c === "x") { g[Y * W + X] = null; continue; }
			if (c === "o") {
				let fam = famContorno;
				if (!fam) {
					const cont = {};
					for (let dy = -1; dy <= 1; dy++) for (let dx = -1; dx <= 1; dx++) {
						const f = familia(ch(x + dx, y + dy)); if (f) cont[f] = (cont[f] || 0) + (dx && dy ? 1 : 2);
					}
					let m = 0; for (const f in cont) if (cont[f] > m) { m = cont[f]; fam = f; }
					if (!fam) { const atual = g[Y * W + X]; fam = atual && atual.fam ? atual.fam : "pele"; }
				}
				g[Y * W + X] = { fam: fam, tom: 3 };
			} else if (ESPECIAL[c]) g[Y * W + X] = { esp: c };
			else if (LETRA[c]) g[Y * W + X] = { fam: LETRA[c][0], tom: LETRA[c][1] };
		}
	}
	function pintar(g, x, y, celula) { x += OX; if (x >= 0 && y >= 0 && x < W && y < H) g[y * W + x] = celula; }

	function cores(cfg) {
		const P = function (lista, id) { return (lista.find(function (x) { return x.id === id; }) || lista[0]).cor; };
		const R = {
			pele: rampa(P(PALETAS.pele, cfg.pele)), cabelo: rampa(P(PALETAS.cabelo, cfg.corCabelo)), olho: rampa(P(PALETAS.olhos, cfg.olhos)),
			camisa: rampa(P(PALETAS.camisa, cfg.camisa)), calca: rampa(P(PALETAS.calca, cfg.calca)), sapato: SAPATO, cinto: CINTO, regador: REGADOR
		};
		R.esp = {
			w: BRANCO, e: R.olho[1], E: R.olho[3],
			r: mistura(R.pele[1], [236, 112, 118], 0.32), f: mistura(R.pele[2], [120, 58, 34], 0.45),
			m: mistura(R.pele[3], [176, 60, 70], cfg.genero === "feminino" ? 0.45 : 0.2)
		};
		return R;
	}
	function rasterizar(g, R) {
		const d = new Uint8ClampedArray(W * H * 4);
		g.forEach(function (c, i) {
			if (!c) return; const cor = c.esp ? R.esp[c.esp] : R[c.fam][c.tom];
			d[i * 4] = cor[0]; d[i * 4 + 1] = cor[1]; d[i * 4 + 2] = cor[2]; d[i * 4 + 3] = 255;
		});
		return d;
	}

	// ------------------------------------------------------------------ poses
	function frente(cfg, i) {
		const g = novaGrade(), fem = cfg.genero === "feminino", cab = cfg.cabelo;
		const bloco = function (nome) { const b = FRENTE[nome]; if (b) camada(g, b.l, 0, b.y, "cabelo"); };
		bloco(cab + ".tras");
		camada(g, CAB_F.l, 0, CAB_F.y);
		if (i === 1) for (const y in OLHOS_FECHADOS) camada(g, [OLHOS_FECHADOS[y]], 0, +y);
		if (fem && i !== 1) CILIOS.forEach(function (p) { pintar(g, p[0], p[1], { esp: "E" }); });
		if (cfg.sardas) SARDAS_FRENTE.forEach(function (p) { pintar(g, p[0], p[1], { esp: "f" }); });
		camada(g, CORPO_FRENTE[cfg.genero], 0, 17);
		if (cfg.facial !== "nenhum") bloco(cfg.facial);
		bloco(cab);
		return g;
	}
	function perfil(cfg, i, rega) {
		const g = novaGrade(), fem = cfg.genero === "feminino", cab = cfg.cabelo;
		const q = rega ? { pf: "reta", pt: "reta", bf: rega.b, bt: "reto", dy: -1 } : ANDAR[i % 4], dy = q.dy;
		let bico = null;
		const bloco = function (nome) { const b = PERFIL[nome]; if (b) camada(g, b.l, 0, b.y + dy, "cabelo"); };
		camada(g, trocar(BRACOS[q.bt].l, TRAS_COR), 0, BRACOS[q.bt].y + dy);
		camada(g, trocar(PERNAS[q.pt].l, TRAS_COR), 0, PERNAS[q.pt].y);
		camada(g, TRONCO_PERFIL, 0, 17 + dy);
		camada(g, trocar(PERNAS[q.pf].l, FRENTE_COR), 0, PERNAS[q.pf].y);
		bloco(cab + ".tras");
		if (rega) {
			const mao = maoDe(BRACOS[q.bf]), R = REGADOR_MAPAS[rega.r], x0 = mao[0] - R.ancora[0], y0 = mao[1] + dy - R.ancora[1];
			camada(g, R.l, x0, y0); bico = [x0 + R.bico[0] + OX, y0 + R.bico[1]];
		}
		camada(g, BRACOS[q.bf].l, 0, BRACOS[q.bf].y + dy);
		camada(g, CAB_P.l, 0, CAB_P.y + dy);
		if (fem) pintar(g, CILIO_P[0], CILIO_P[1] + dy, { esp: "E" });
		if (cfg.sardas) SARDAS_PERFIL.forEach(function (p) { pintar(g, p[0], p[1] + dy, { esp: "f" }); });
		if (cfg.facial !== "nenhum") bloco(cfg.facial);
		bloco(cab);
		g.bico = bico;
		return g;
	}
	const POSES = { parado: 2, andar: 4, regar: 3 };

	function quadro(config, estado, i) {
		const cfg = validar(config), R = cores(cfg);
		let g;
		if (estado === "andar") g = perfil(cfg, i);
		else if (estado === "regar") g = perfil(cfg, 0, REGAR[((i % 3) + 3) % 3]);
		else g = frente(cfg, i % 2);
		return { largura: W, altura: H, dados: rasterizar(g, R), bico: g.bico || null };
	}

	function validar(c) {
		c = c || {}; const ok = function (l, id) { return l.some(function (x) { return x.id === id; }); };
		return {
			versao: 2, genero: c.genero === "feminino" ? "feminino" : "masculino",
			pele: ok(PALETAS.pele, c.pele) ? c.pele : "p3", olhos: ok(PALETAS.olhos, c.olhos) ? c.olhos : "o1",
			cabelo: ok(CABELOS, c.cabelo) ? c.cabelo : "curto", corCabelo: ok(PALETAS.cabelo, c.corCabelo) ? c.corCabelo : "c3",
			facial: ok(FACIAIS, c.facial) ? c.facial : "nenhum", sardas: !!c.sardas,
			camisa: ok(PALETAS.camisa, c.camisa) ? c.camisa : "r1", calca: ok(PALETAS.calca, c.calca) ? c.calca : "k1"
		};
	}

	function aleatorio() {
		const s = function (l) { return l[Math.floor(Math.random() * l.length)].id; }, gen = Math.random() < 0.5 ? "masculino" : "feminino";
		return validar({ genero: gen, pele: s(PALETAS.pele), olhos: s(PALETAS.olhos), cabelo: s(CABELOS),
			corCabelo: Math.random() < 0.8 ? PALETAS.cabelo[Math.floor(Math.random() * 9)].id : s(PALETAS.cabelo),
			facial: gen === "masculino" && Math.random() < 0.45 ? s(FACIAIS.slice(1)) : "nenhum", sardas: Math.random() < 0.25,
			camisa: s(PALETAS.camisa), calca: s(PALETAS.calca) });
	}
	G.RaizAvatar = {
		W: W, H: H, CHAO: CHAO, PALETAS: PALETAS, CABELOS: CABELOS, FACIAIS: FACIAIS, GENEROS: GENEROS,
		quadros: function (e) { return POSES[e] || 2; }, quadro: quadro, validar: validar, aleatorio: aleatorio, _int: { CABECA_FRENTE, CORPO_FRENTE, CABECA_PERFIL, TRONCO_PERFIL }
	};
})(typeof window !== "undefined" ? window : globalThis);
