/* =============================================================================
   Dashboard rAIz — DADOS SIMULADOS (mock)
   -----------------------------------------------------------------------------
   Usado apenas pelas rotas de routes/api/dashboardApi.js enquanto o backend
   Java ainda não expõe os endpoints do dashboard. Nada aqui é dado real.

   Quando a API real existir, basta desligar o mock (DASHBOARD_MOCK=false no
   .env) — a página e as rotas continuam iguais; só a origem dos dados muda.

   Rastreabilidade (documento D1):
   - AI Maturity Score e indicadores ...... RF13, RF14, RF15, RF16
   - Competência em IA .................... RF06, RF07, RF08, RF09
   - Uso de modelos / tokens .............. RF17, RF18
   - AI Credits (gamificação) ............. RF20, RF21, RF22, RF23
   - Evolução por período ................. RF09, RF12, RF15, RF22

   PROVISÓRIO (ainda não definido pela equipe — RB09 e RB11):
   fórmula do score (aqui: média simples dos 8 indicadores), faixas de nível,
   regras de AI Credits, estágios da árvore, metas e conquistas.
   ============================================================================= */
"use strict";

// -----------------------------------------------------------------------------
// Catálogos
// -----------------------------------------------------------------------------

// RF16 — indicadores considerados na avaliação de maturidade.
const INDICADORES = [
	{ id: "aiLiteracy", nome: "AI Literacy" },
	{ id: "governanca", nome: "Governança" },
	{ id: "usoResponsavel", nome: "Uso Responsável" },
	{ id: "produtividade", nome: "Produtividade" },
	{ id: "finops", nome: "FinOps" },
	{ id: "seguranca", nome: "Segurança" },
	{ id: "colaboracao", nome: "Colaboração" },
	{ id: "desenvCompetencias", nome: "Desenvolvimento de Competências" }
];

// Indicadores usados no perfil de competência individual (RF06). PROVISÓRIO.
const INDICADORES_PESSOAIS = ["aiLiteracy", "usoResponsavel", "produtividade", "seguranca", "colaboracao", "desenvCompetencias"];

// RF18 — modelos de IA analisados.
const MODELOS = ["ChatGPT", "Claude", "Gemini", "Copilot"];

// Faixas PROVISÓRIAS de nível de competência (0–100).
const NIVEIS_COMPETENCIA = [
	{ nome: "Iniciante", minimo: 0 },
	{ nome: "Intermediário", minimo: 50 },
	{ nome: "Avançado", minimo: 75 }
];

// Faixas PROVISÓRIAS de AI Credits acumulados por estágio da árvore (1 a 5).
const ESTAGIOS_ARVORE = [0, 400, 900, 1500, 2200];

// Metas e conquistas de EXEMPLO (RB11 ainda não definida).
const METAS = [
	{ id: "trilhaLiteracy", titulo: "Concluir a trilha de AI Literacy", unidade: "módulos", alvo: 8 },
	{ id: "boasPraticas", titulo: "Compartilhar boas práticas com a equipe", unidade: "publicações", alvo: 5 },
	{ id: "usoSeguro", titulo: "Usar apenas ferramentas aprovadas", unidade: "dias sem ocorrência", alvo: 30 },
	{ id: "creditos", titulo: "Acumular AI Credits no mês", unidade: "créditos", alvo: 120 }
];

const CONQUISTAS = [
	{ id: "primeirosPassos", titulo: "Primeiros passos", descricao: "Primeiro uso registrado na plataforma", icone: "bi-flag" },
	{ id: "guardiao", titulo: "Guardião dos dados", descricao: "Nenhum incidente de segurança no trimestre", icone: "bi-shield-check" },
	{ id: "mentor", titulo: "Mentor", descricao: "Ajudou colegas a evoluir no uso de IA", icone: "bi-people" },
	{ id: "eficiente", titulo: "Uso eficiente", descricao: "Reduziu o consumo de tokens mantendo a produtividade", icone: "bi-lightning-charge" },
	{ id: "estudioso", titulo: "Aprendiz contínuo", descricao: "Concluiu três capacitações em IA", icone: "bi-mortarboard" },
	{ id: "explorador", titulo: "Explorador", descricao: "Utilizou mais de um modelo de IA com bons resultados", icone: "bi-compass" }
];

const PERIODOS = [
	{ meses: 1, texto: "Último mês" },
	{ meses: 3, texto: "Últimos 3 meses" },
	{ meses: 6, texto: "Últimos 6 meses" },
	{ meses: 12, texto: "Últimos 12 meses" }
];

// -----------------------------------------------------------------------------
// Estrutura organizacional fictícia: Empresa → Área (Gestor) → Equipe (Líder) → Pessoas
// -----------------------------------------------------------------------------

const EMPRESA = { id: "EMP-01", nome: "Empresa Demonstração" };

const ADMINISTRADOR = { id: "ADM-01", nome: "Helena Duarte", perfil: "administrador", areaId: null, equipeId: null };

const AREAS_BASE = [
	{
		id: "AR-TEC", nome: "Tecnologia", gestor: "Ricardo Almeida",
		equipes: [
			{ id: "EQ-PLAT", nome: "Plataforma", lider: "Camila Rocha", colaboradores: ["Ana Souza", "Bruno Lima", "Diego Martins", "Fernanda Costa"] },
			{ id: "EQ-DADOS", nome: "Dados", lider: "Marcos Pereira", colaboradores: ["Juliana Alves", "Lucas Ferreira", "Paula Ribeiro", "Thiago Gomes"] }
		]
	},
	{
		id: "AR-MKT", nome: "Marketing", gestor: "Patrícia Nunes",
		equipes: [
			{ id: "EQ-CONT", nome: "Conteúdo", lider: "Rafaela Dias", colaboradores: ["Gabriel Santos", "Isabela Moreira", "Mateus Carvalho", "Larissa Teixeira"] },
			{ id: "EQ-PERF", nome: "Performance", lider: "Eduardo Barros", colaboradores: ["Beatriz Freitas", "Vinícius Araújo", "Natália Castro", "Rodrigo Pinto"] }
		]
	},
	{
		id: "AR-FIN", nome: "Financeiro", gestor: "Sérgio Monteiro",
		equipes: [
			{ id: "EQ-CONTAB", nome: "Contabilidade", lider: "Aline Cardoso", colaboradores: ["Felipe Rezende", "Mariana Lopes", "Otávio Farias", "Carolina Mendes"] },
			{ id: "EQ-PLAN", nome: "Planejamento", lider: "Renato Vieira", colaboradores: ["Letícia Campos", "Gustavo Ramos", "Sabrina Melo", "André Queiroz"] }
		]
	}
];

const pessoas = {};
const equipes = {};
const areas = {};

function slug(texto) {
	return texto.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().replace(/[^a-z0-9]+/g, "-");
}

AREAS_BASE.forEach(function (areaBase) {
	const gestorId = "P-" + slug(areaBase.gestor);
	pessoas[gestorId] = { id: gestorId, nome: areaBase.gestor, perfil: "gestor", areaId: areaBase.id, equipeId: null };

	const area = { id: areaBase.id, nome: areaBase.nome, gestorId: gestorId, equipeIds: [] };

	areaBase.equipes.forEach(function (equipeBase) {
		const liderId = "P-" + slug(equipeBase.lider);
		pessoas[liderId] = { id: liderId, nome: equipeBase.lider, perfil: "lider", areaId: areaBase.id, equipeId: equipeBase.id };

		const colaboradorIds = equipeBase.colaboradores.map(function (nome) {
			const id = "P-" + slug(nome);
			pessoas[id] = { id: id, nome: nome, perfil: "colaborador", areaId: areaBase.id, equipeId: equipeBase.id };
			return id;
		});

		equipes[equipeBase.id] = { id: equipeBase.id, nome: equipeBase.nome, areaId: areaBase.id, liderId: liderId, colaboradorIds: colaboradorIds };
		area.equipeIds.push(equipeBase.id);
	});

	areas[areaBase.id] = area;
});

// Quem "está logado" em cada perfil do seletor "Simular como" da página.
const USUARIOS_SIMULADOS = {
	colaborador: pessoas["P-ana-souza"],
	lider: pessoas["P-camila-rocha"],
	gestor: pessoas["P-ricardo-almeida"],
	administrador: ADMINISTRADOR
};

// -----------------------------------------------------------------------------
// Linha do tempo: 24 meses (até 12 exibidos + 12 para comparar com o período anterior)
// -----------------------------------------------------------------------------

const TOTAL_MESES = 24;
const MESES_ABREV = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"];

function rotulosMeses() {
	const hoje = new Date();
	const rotulos = [];
	for (let i = TOTAL_MESES - 1; i >= 0; i--) {
		const d = new Date(hoje.getFullYear(), hoje.getMonth() - i, 1);
		rotulos.push(MESES_ABREV[d.getMonth()] + "/" + String(d.getFullYear()).slice(2));
	}
	return rotulos;
}

// -----------------------------------------------------------------------------
// Gerador determinístico: a mesma pessoa sempre gera os mesmos números (RNF18)
// -----------------------------------------------------------------------------

function hashTexto(texto) {
	let h = 2166136261;
	for (let i = 0; i < texto.length; i++) {
		h ^= texto.charCodeAt(i);
		h = Math.imul(h, 16777619);
	}
	return h >>> 0;
}

function criarAleatorio(semente) {
	let a = semente;
	return function () {
		a |= 0; a = a + 0x6D2B79F5 | 0;
		let t = Math.imul(a ^ a >>> 15, 1 | a);
		t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t;
		return ((t ^ t >>> 14) >>> 0) / 4294967296;
	};
}

function limitar(valor, minimo, maximo) {
	return Math.max(minimo, Math.min(maximo, valor));
}

const historicoCache = {};

function historicoPessoa(pessoaId) {
	if (historicoCache[pessoaId]) return historicoCache[pessoaId];

	const rnd = criarAleatorio(hashTexto(pessoaId));
	const base = 38 + rnd() * 45;
	const tendencia = 0.15 + rnd() * 0.55;
	const volumeUso = 0.5 + rnd() * 1.2;

	const offsets = {};
	INDICADORES.forEach(function (ind) { offsets[ind.id] = (rnd() - 0.5) * 24; });

	const pesosModelos = MODELOS.map(function () { return 0.2 + rnd(); });
	const somaPesos = pesosModelos.reduce(function (s, p) { return s + p; }, 0);

	const meses = [];
	for (let m = 0; m < TOTAL_MESES; m++) {
		const indicadores = {};
		INDICADORES.forEach(function (ind) {
			indicadores[ind.id] = limitar(base + offsets[ind.id] + tendencia * m + (rnd() - 0.5) * 8, 5, 98);
		});

		const tokens = Math.round((60 + rnd() * 90) * volumeUso * 1000);

		meses.push({
			indicadores: indicadores,
			creditos: Math.round(limitar(25 + base * 0.9 + tendencia * m * 2 + (rnd() - 0.5) * 30, 5, 200)),
			tokens: tokens,
			tokensPorModelo: pesosModelos.map(function (p) { return Math.round(tokens * p / somaPesos); }),
			interacoes: Math.round(tokens / (900 + rnd() * 600))
		});
	}

	const historico = {
		meses: meses,
		metas: METAS.map(function (meta) {
			return { titulo: meta.titulo, unidade: meta.unidade, alvo: meta.alvo, atual: Math.round(meta.alvo * limitar(0.15 + rnd() * 0.9, 0, 1)) };
		}),
		conquistas: CONQUISTAS.map(function (c) {
			return { titulo: c.titulo, descricao: c.descricao, icone: c.icone, desbloqueada: c.id === "primeirosPassos" || rnd() < (base / 100) + 0.1 };
		})
	};

	historicoCache[pessoaId] = historico;
	return historico;
}

// -----------------------------------------------------------------------------
// Cálculos
// -----------------------------------------------------------------------------

function media(valores) {
	return valores.length ? valores.reduce(function (s, v) { return s + v; }, 0) / valores.length : 0;
}

function soma(valores) {
	return valores.reduce(function (s, v) { return s + v; }, 0);
}

function competenciaDoMes(mes) {
	return media(INDICADORES_PESSOAIS.map(function (id) { return mes.indicadores[id]; }));
}

// AI Maturity Score — PROVISÓRIO: média simples dos 8 indicadores (RB09).
function scoreDe(indicadores) {
	return media(INDICADORES.map(function (ind) { return indicadores[ind.id]; }));
}

function nivelCompetencia(valor) {
	let nivel = NIVEIS_COMPETENCIA[0].nome;
	NIVEIS_COMPETENCIA.forEach(function (n) { if (valor >= n.minimo) nivel = n.nome; });
	return nivel;
}

function estagioArvore(creditosAcumulados) {
	let estagio = 1;
	ESTAGIOS_ARVORE.forEach(function (minimo, i) { if (creditosAcumulados >= minimo) estagio = i + 1; });
	const proximo = estagio < ESTAGIOS_ARVORE.length ? ESTAGIOS_ARVORE[estagio] : null;
	return {
		estagio: estagio,
		totalEstagios: ESTAGIOS_ARVORE.length,
		creditosAcumulados: creditosAcumulados,
		faltamParaProximo: proximo === null ? 0 : proximo - creditosAcumulados,
		ultimoEstagio: proximo === null
	};
}

function indicesPeriodo(meses) {
	const atual = [];
	const anterior = [];
	for (let i = TOTAL_MESES - meses; i < TOTAL_MESES; i++) atual.push(i);
	for (let i = TOTAL_MESES - 2 * meses; i < TOTAL_MESES - meses; i++) anterior.push(i);
	return { atual: atual, anterior: anterior };
}

function variacaoPercentual(atual, anterior) {
	return anterior ? ((atual - anterior) / anterior) * 100 : null;
}

function rotulosDoPeriodo(meses) {
	const todos = rotulosMeses();
	return indicesPeriodo(meses).atual.map(function (i) { return todos[i]; });
}

// -----------------------------------------------------------------------------
// Resumos (formato que a API devolve para a página)
// -----------------------------------------------------------------------------

function resumoPessoa(pessoaId, meses) {
	const hist = historicoPessoa(pessoaId);
	const idx = indicesPeriodo(meses);
	const doPeriodo = idx.atual.map(function (i) { return hist.meses[i]; });
	const doAnterior = idx.anterior.map(function (i) { return hist.meses[i]; });

	const creditos = soma(doPeriodo.map(function (m) { return m.creditos; }));
	const tokens = soma(doPeriodo.map(function (m) { return m.tokens; }));

	const competencias = INDICADORES_PESSOAIS.map(function (id) {
		return {
			id: id,
			nome: INDICADORES.find(function (i) { return i.id === id; }).nome,
			valor: media(doPeriodo.map(function (m) { return m.indicadores[id]; }))
		};
	});
	const competencia = media(competencias.map(function (c) { return c.valor; }));

	return {
		rotulos: rotulosDoPeriodo(meses),
		creditos: creditos,
		variacaoCreditos: variacaoPercentual(creditos, soma(doAnterior.map(function (m) { return m.creditos; }))),
		tokens: tokens,
		variacaoTokens: variacaoPercentual(tokens, soma(doAnterior.map(function (m) { return m.tokens; }))),
		interacoes: soma(doPeriodo.map(function (m) { return m.interacoes; })),
		competencia: competencia,
		nivel: nivelCompetencia(competencia),
		competencias: competencias,
		serieCreditos: doPeriodo.map(function (m) { return m.creditos; }),
		serieCompetencia: doPeriodo.map(competenciaDoMes),
		modelos: MODELOS.map(function (nome, j) { return { nome: nome, tokens: soma(doPeriodo.map(function (m) { return m.tokensPorModelo[j]; })) }; }),
		arvore: estagioArvore(soma(hist.meses.map(function (m) { return m.creditos; }))),
		metas: hist.metas,
		conquistas: hist.conquistas
	};
}

function resumoGrupo(pessoaIds, meses) {
	const idx = indicesPeriodo(meses);
	const hists = pessoaIds.map(historicoPessoa);

	function somaNoMes(i, campo) { return soma(hists.map(function (h) { return h.meses[i][campo]; })); }

	function indicadoresMediosNoMes(i) {
		const r = {};
		INDICADORES.forEach(function (ind) { r[ind.id] = media(hists.map(function (h) { return h.meses[i].indicadores[ind.id]; })); });
		return r;
	}

	const creditos = soma(idx.atual.map(function (i) { return somaNoMes(i, "creditos"); }));
	const creditosAnterior = soma(idx.anterior.map(function (i) { return somaNoMes(i, "creditos"); }));
	const tokens = soma(idx.atual.map(function (i) { return somaNoMes(i, "tokens"); }));
	const tokensAnterior = soma(idx.anterior.map(function (i) { return somaNoMes(i, "tokens"); }));

	const indicadoresPeriodo = {};
	INDICADORES.forEach(function (ind) {
		indicadoresPeriodo[ind.id] = media(idx.atual.map(function (i) { return indicadoresMediosNoMes(i)[ind.id]; }));
	});

	const score = scoreDe(indicadoresPeriodo);
	const scoreAnterior = media(idx.anterior.map(function (i) { return scoreDe(indicadoresMediosNoMes(i)); }));

	const competencias = pessoaIds.map(function (id) { return resumoPessoa(id, meses).competencia; });

	return {
		rotulos: rotulosDoPeriodo(meses),
		pessoas: pessoaIds.length,
		creditos: creditos,
		variacaoCreditos: variacaoPercentual(creditos, creditosAnterior),
		tokens: tokens,
		variacaoTokens: variacaoPercentual(tokens, tokensAnterior),
		competencia: media(competencias),
		score: score,
		variacaoScore: score - scoreAnterior,
		indicadores: INDICADORES.map(function (ind) { return { id: ind.id, nome: ind.nome, valor: indicadoresPeriodo[ind.id] }; }),
		serieScore: idx.atual.map(function (i) { return scoreDe(indicadoresMediosNoMes(i)); }),
		serieCreditos: idx.atual.map(function (i) { return somaNoMes(i, "creditos"); }),
		serieCompetencia: idx.atual.map(function (i) { return media(hists.map(function (h) { return competenciaDoMes(h.meses[i]); })); }),
		modelos: MODELOS.map(function (nome, j) {
			return { nome: nome, tokens: soma(idx.atual.map(function (i) { return soma(hists.map(function (h) { return h.meses[i].tokensPorModelo[j]; })); })) };
		}),
		// Distribuição agregada: só quantidades, nenhuma pessoa identificada (RF08).
		distribuicao: NIVEIS_COMPETENCIA.map(function (n) {
			return { nivel: n.nome, quantidade: competencias.filter(function (c) { return nivelCompetencia(c) === n.nome; }).length };
		})
	};
}

// -----------------------------------------------------------------------------
// Estrutura
// -----------------------------------------------------------------------------

function pessoa(id) { return pessoas[id] || (id === ADMINISTRADOR.id ? ADMINISTRADOR : null); }
function equipe(id) { return equipes[id] || null; }
function area(id) { return areas[id] || null; }
function listarAreas() { return Object.keys(areas).map(function (id) { return areas[id]; }); }

function membrosEquipe(equipeId) {
	const eq = equipes[equipeId];
	return eq ? [eq.liderId].concat(eq.colaboradorIds) : [];
}

function membrosArea(areaId) {
	const a = areas[areaId];
	if (!a) return [];
	let ids = [a.gestorId];
	a.equipeIds.forEach(function (eqId) { ids = ids.concat(membrosEquipe(eqId)); });
	return ids;
}

function membrosEmpresa() {
	return Object.keys(pessoas);
}

module.exports = {
	EMPRESA: EMPRESA,
	PERIODOS: PERIODOS,
	USUARIOS_SIMULADOS: USUARIOS_SIMULADOS,
	pessoa: pessoa,
	equipe: equipe,
	area: area,
	listarAreas: listarAreas,
	membrosEquipe: membrosEquipe,
	membrosArea: membrosArea,
	membrosEmpresa: membrosEmpresa,
	resumoPessoa: resumoPessoa,
	resumoGrupo: resumoGrupo
};
