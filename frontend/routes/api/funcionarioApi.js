const express = require("express");
const wrap = require("express-async-error-wrapper");
const axios = require("axios");

const router = express.Router();

const url_springboot = process.env.url_springboot;

// Repassa status HTTP e corpo reais do backend Java (400/401/403/404/409...)
// em vez de esconder tudo atrás de um 500 genérico.
function repassarErro(res, ex) {
	if (ex.response) {
		res.status(ex.response.status).json(ex.response.data);
	} else {
		res.status(500).json("Exceção na comunicação com o servidor remoto: " + ex.message);
	}
}

router.post("/login", wrap(async (req, res) => {
	try {
		const response = await axios.post(url_springboot + "/api/funcionario/login", req.body);

		res.json(response.data);
	} catch (ex) {
		repassarErro(res, ex);
	}
}));

router.get("/listar", wrap(async (req, res) => {
	try {
		const response = await axios.get(url_springboot + "/api/funcionario/listar");

		res.json(response.data);
	} catch (ex) {
		repassarErro(res, ex);
	}
}));

router.get("/buscar/:id", wrap(async (req, res) => {
	const id = req.params.id;

	try {
		const response = await axios.get(url_springboot + "/api/funcionario/buscar/" + encodeURIComponent(id));

		res.json(response.data);
	} catch (ex) {
		repassarErro(res, ex);
	}
}));

router.post("/criar", wrap(async (req, res) => {
	const funcionario = req.body;

	try {
		const response = await axios.post(url_springboot + "/api/funcionario/criar", funcionario);

		res.status(response.status).json(response.data);
	} catch (ex) {
		repassarErro(res, ex);
	}
}));

router.delete("/deletar/:id", wrap(async (req, res) => {
	const id = req.params.id;

	try {
		const response = await axios.delete(url_springboot + "/api/funcionario/deletar/" + encodeURIComponent(id));

		res.status(response.status).json(response.data ?? { sucesso: true });
	} catch (ex) {
		repassarErro(res, ex);
	}
}));

module.exports = router;
