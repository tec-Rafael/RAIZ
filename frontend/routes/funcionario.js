const express = require("express");
const wrap = require("express-async-error-wrapper");

const router = express.Router();
const { autenticar, gestao } = require("../middleware/autenticacao");
router.use(autenticar, gestao);

router.get("/", wrap(async (req, res) => {
	res.redirect("/funcionario/listar");
}));

router.get("/criar", wrap(async (req, res) => {
	res.render("funcionario/criar", { titulo: "Cadastrar funcionário" });
}));

router.get("/listar", wrap(async (req, res) => {
	res.render("funcionario/listar", { titulo: "Funcionários" });
}));



// Não há rota "/editar": o backend não expõe um endpoint de atualização
// (apenas criar, listar, buscar/{id} e deletar/{id}). Se um endpoint de
// update for adicionado no Spring Boot, essa página pode voltar aqui.

module.exports = router;
