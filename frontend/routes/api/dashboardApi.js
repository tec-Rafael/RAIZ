const express = require("express");
const wrap = require("express-async-error-wrapper");
const axios = require("axios");
const mock = require("../../mock/dashboardMock");

const router = express.Router();

const url_springboot = process.env.url_springboot;

// Enquanto o backend Java não tiver os endpoints do dashboard, as rotas abaixo
// respondem com dados simulados (mock/dashboardMock.js). Para passar a usar a
// API real, basta definir dashboard_mock=false no .env — a página não muda.
const usarMock = process.env.dashboard_mock !== "false";

const PERFIS = {
	colaborador: "Colaborador",
	lider: "Líder",
	gestor: "Gestor",
	administrador: "Administrador"
};

const MESES_VALIDOS = [1, 3, 6, 12];

// Repassa status HTTP e corpo reais do backend Java (400/401/403/404/409...)
// em vez de esconder tudo atrás de um 500 genérico.
function repassarErro(res, ex) {
	if (ex.response) {
		res.status(ex.response.status).json(ex.response.data);
	} else {
		res.status(500).json("Exceção na comunicação com o servidor remoto: " + ex.message);
	}
}

// Quem está pedindo os dados.
// SIMULAÇÃO: o perfil vem do seletor "Simular como" da página (?perfil=...).
// Quando o login passar a guardar o usuário, ele deve vir da sessão, e não da URL.
function obterSolicitante(req) {
	const perfil = req.query.perfil;
	if (!PERFIS[perfil]) return null;
	return { perfil: perfil, usuario: mock.USUARIOS_SIMULADOS[perfil] };
}

function obterMeses(req) {
	const meses = parseInt(req.query.meses, 10);
	return MESES_VALIDOS.indexOf(meses) >= 0 ? meses : null;
}

// Regras de visibilidade por perfil (D1: RF02, RF06–RF08, RB03–RB06, RNF02, RNF19).
// - Colaborador ... só o próprio dashboard pessoal.
// - Líder ......... pessoal (ele ou integrante da sua equipe) e geral da equipe.
// - Gestor ........ geral da sua área (agregado) e o próprio pessoal.
// - Administrador . geral da empresa; por área, geral ou pessoal de qualquer pessoa.
//                   Não tem dashboard pessoal próprio.
function podeVer(solicitante, alvo) {
	const u = solicitante.usuario;

	switch (solicitante.perfil) {
		case "colaborador":
			return alvo.tipo === "pessoa" && alvo.id === u.id;
		case "lider":
			if (alvo.tipo === "equipe") return alvo.id === u.equipeId;
			if (alvo.tipo === "pessoa") return mock.membrosEquipe(u.equipeId).indexOf(alvo.id) >= 0;
			return false;
		case "gestor":
			if (alvo.tipo === "area") return alvo.id === u.areaId;
			if (alvo.tipo === "pessoa") return alvo.id === u.id;
			return false;
		case "administrador":
			if (alvo.tipo === "empresa") return true;
			if (alvo.tipo === "area") return !!mock.area(alvo.id);
			if (alvo.tipo === "pessoa") return !!mock.pessoa(alvo.id) && alvo.id !== u.id;
			return false;
		default:
			return false;
	}
}

// Validação comum às rotas de dados. Devolve o solicitante e o período, ou já responde o erro.
function validar(req, res, alvo) {
	const solicitante = obterSolicitante(req);
	if (!solicitante) {
		res.status(400).json("Perfil inválido.");
		return null;
	}

	const meses = obterMeses(req);
	if (!meses) {
		res.status(400).json("Período inválido. Use 1, 3, 6 ou 12 meses.");
		return null;
	}

	if (alvo && !podeVer(solicitante, alvo)) {
		res.status(403).json("Este conteúdo não está disponível para o perfil " + PERFIS[solicitante.perfil] + ".");
		return null;
	}

	return { solicitante: solicitante, meses: meses };
}

function descricaoPessoa(p) {
	if (p.perfil === "administrador") return "Administrador(a)";
	const nomeArea = mock.area(p.areaId).nome;
	if (p.perfil === "gestor") return "Gestor(a) · Área " + nomeArea;
	const papel = p.perfil === "lider" ? "Líder" : "Colaborador(a)";
	return papel + " · Equipe " + mock.equipe(p.equipeId).nome + " · Área " + nomeArea;
}

function resumoDePessoa(p) {
	return { id: p.id, nome: p.nome, perfil: p.perfil, descricao: descricaoPessoa(p) };
}

// -----------------------------------------------------------------------------
// Simulação: usuários disponíveis no seletor "Simular como"
// -----------------------------------------------------------------------------

router.get("/simulacao/usuarios", wrap(async (req, res) => {
	res.json(Object.keys(PERFIS).map(function (perfil) {
		const u = mock.USUARIOS_SIMULADOS[perfil];
		return { perfil: perfil, perfilNome: PERFIS[perfil], id: u.id, nome: u.nome };
	}));
}));

// -----------------------------------------------------------------------------
// Opções de filtro permitidas ao perfil
// -----------------------------------------------------------------------------

router.get("/filtros", wrap(async (req, res) => {
	const solicitante = obterSolicitante(req);
	if (!solicitante) {
		res.status(400).json("Perfil inválido.");
		return;
	}

	if (!usarMock) {
		try {
			// Contrato proposto para o backend Java (ainda não existe).
			const response = await axios.get(url_springboot + "/api/dashboard/filtros", { params: req.query });
			res.json(response.data);
		} catch (ex) {
			repassarErro(res, ex);
		}
		return;
	}

	const u = solicitante.usuario;
	const resposta = {
		perfil: solicitante.perfil,
		perfilNome: PERFIS[solicitante.perfil],
		usuario: resumoDePessoa(u),
		periodos: mock.PERIODOS
	};

	if (solicitante.perfil === "lider") {
		const eq = mock.equipe(u.equipeId);
		resposta.equipe = { id: eq.id, nome: eq.nome };
		resposta.pessoas = [{ id: u.id, nome: u.nome + " (eu)" }].concat(eq.colaboradorIds.map(function (id) {
			return { id: id, nome: mock.pessoa(id).nome };
		}));
	}

	if (solicitante.perfil === "gestor") {
		const a = mock.area(u.areaId);
		resposta.area = { id: a.id, nome: a.nome };
	}

	if (solicitante.perfil === "administrador") {
		resposta.areas = mock.listarAreas().map(function (a) {
			const pessoasDaArea = [{ id: a.gestorId, nome: mock.pessoa(a.gestorId).nome, grupo: "Gestor" }];
			a.equipeIds.forEach(function (eqId) {
				const eq = mock.equipe(eqId);
				pessoasDaArea.push({ id: eq.liderId, nome: mock.pessoa(eq.liderId).nome + " — " + eq.nome, grupo: "Líderes" });
			});
			a.equipeIds.forEach(function (eqId) {
				const eq = mock.equipe(eqId);
				eq.colaboradorIds.forEach(function (id) {
					pessoasDaArea.push({ id: id, nome: mock.pessoa(id).nome + " — " + eq.nome, grupo: "Colaboradores" });
				});
			});
			return { id: a.id, nome: a.nome, pessoas: pessoasDaArea };
		});
	}

	res.json(resposta);
}));

// -----------------------------------------------------------------------------
// Dashboards
// -----------------------------------------------------------------------------

router.get("/pessoal/:id", wrap(async (req, res) => {
	const id = req.params.id;
	const ctx = validar(req, res, { tipo: "pessoa", id: id });
	if (!ctx) return;

	if (!usarMock) {
		try {
			// Contrato proposto para o backend Java (ainda não existe).
			const response = await axios.get(url_springboot + "/api/dashboard/pessoal/" + encodeURIComponent(id), { params: { meses: ctx.meses } });
			res.json(response.data);
		} catch (ex) {
			repassarErro(res, ex);
		}
		return;
	}

	const p = mock.pessoa(id);
	res.json(Object.assign({ pessoa: resumoDePessoa(p) }, mock.resumoPessoa(id, ctx.meses)));
}));

router.get("/equipe/:id", wrap(async (req, res) => {
	const id = req.params.id;
	const ctx = validar(req, res, { tipo: "equipe", id: id });
	if (!ctx) return;

	if (!usarMock) {
		try {
			// Contrato proposto para o backend Java (ainda não existe).
			const response = await axios.get(url_springboot + "/api/dashboard/equipe/" + encodeURIComponent(id), { params: { meses: ctx.meses } });
			res.json(response.data);
		} catch (ex) {
			repassarErro(res, ex);
		}
		return;
	}

	const eq = mock.equipe(id);
	const ids = mock.membrosEquipe(id);

	// RF07: o líder vê as informações de cada integrante da sua equipe.
	const integrantes = ids.map(function (pid) {
		const p = mock.pessoa(pid);
		const r = mock.resumoPessoa(pid, ctx.meses);
		return { id: p.id, nome: p.nome, perfil: p.perfil, creditos: r.creditos, competencia: r.competencia, tokens: r.tokens };
	}).sort(function (a, b) { return b.creditos - a.creditos; });

	res.json(Object.assign(
		{ equipe: { id: eq.id, nome: eq.nome, area: mock.area(eq.areaId).nome }, integrantes: integrantes },
		mock.resumoGrupo(ids, ctx.meses)
	));
}));

router.get("/area/:id", wrap(async (req, res) => {
	const id = req.params.id;
	const ctx = validar(req, res, { tipo: "area", id: id });
	if (!ctx) return;

	if (!usarMock) {
		try {
			// Contrato proposto para o backend Java (ainda não existe).
			const response = await axios.get(url_springboot + "/api/dashboard/area/" + encodeURIComponent(id), { params: { meses: ctx.meses } });
			res.json(response.data);
		} catch (ex) {
			repassarErro(res, ex);
		}
		return;
	}

	// RF08: visão agregada — nenhuma pessoa é identificada na resposta.
	const a = mock.area(id);
	res.json(Object.assign({ area: { id: a.id, nome: a.nome } }, mock.resumoGrupo(mock.membrosArea(id), ctx.meses)));
}));

router.get("/empresa", wrap(async (req, res) => {
	const ctx = validar(req, res, { tipo: "empresa" });
	if (!ctx) return;

	if (!usarMock) {
		try {
			// Contrato proposto para o backend Java (ainda não existe).
			const response = await axios.get(url_springboot + "/api/dashboard/empresa", { params: { meses: ctx.meses } });
			res.json(response.data);
		} catch (ex) {
			repassarErro(res, ex);
		}
		return;
	}

	const porArea = mock.listarAreas().map(function (a) {
		const r = mock.resumoGrupo(mock.membrosArea(a.id), ctx.meses);
		return { id: a.id, nome: a.nome, score: r.score, creditos: r.creditos, competencia: r.competencia, pessoas: r.pessoas };
	});

	res.json(Object.assign({ empresa: mock.EMPRESA, areas: porArea }, mock.resumoGrupo(mock.membrosEmpresa(), ctx.meses)));
}));

module.exports = router;
