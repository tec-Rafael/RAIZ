const express = require("express");
const wrap = require("express-async-error-wrapper");
const axios = require("axios");

const router = express.Router();

const url_springboot = process.env.url_springboot;

// Repassa as rotas do módulo de utilização de IA do backend Java
// (IaController: /api/utilizacao-ia). Ainda não são usadas pelo dashboard,
// que por enquanto trabalha com dados simulados (ver routes/api/dashboardApi.js).

// Repassa status HTTP e corpo reais do backend Java (400/401/403/404/409...)
// em vez de esconder tudo atrás de um 500 genérico.
function repassarErro(res, ex) {
	if (ex.response) {
		res.status(ex.response.status).json(ex.response.data);
	} else {
		res.status(500).json("Exceção na comunicação com o servidor remoto: " + ex.message);
	}
}

// Listagem paginada: ?page=0&size=50 (o backend limita size a 100).
router.get("/listar", wrap(async (req, res) => {
	try {
		const response = await axios.get(url_springboot + "/api/utilizacao-ia/listar", {
			params: { page: req.query.page, size: req.query.size }
		});

		res.json(response.data);
	} catch (ex) {
		repassarErro(res, ex);
	}
}));

router.get("/buscar/:id", wrap(async (req, res) => {
	const id = req.params.id;

	try {
		const response = await axios.get(url_springboot + "/api/utilizacao-ia/buscar/" + encodeURIComponent(id));

		res.json(response.data);
	} catch (ex) {
		repassarErro(res, ex);
	}
}));

router.post("/criar", wrap(async (req, res) => {
	const utilizacao = req.body;

	try {
		const response = await axios.post(url_springboot + "/api/utilizacao-ia/criar", utilizacao);

		res.status(response.status).json(response.data);
	} catch (ex) {
		repassarErro(res, ex);
	}
}));

router.delete("/deletar/:id", wrap(async (req, res) => {
	const id = req.params.id;

	try {
		const response = await axios.delete(url_springboot + "/api/utilizacao-ia/deletar/" + encodeURIComponent(id));

		res.status(response.status).json(response.data ?? { sucesso: true });
	} catch (ex) {
		repassarErro(res, ex);
	}
}));

module.exports = router;
