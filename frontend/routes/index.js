const express = require("express");
const wrap = require("express-async-error-wrapper");

const router = express.Router();
const { autenticar } = require("../middleware/autenticacao");

router.get("/", autenticar, wrap(async (req, res) => {
  res.render("index/index", { titulo: "Início" });
}));

const { config, proxy } = require("../middleware/autenticacao");
router.get("/arvore", autenticar, wrap(async (req, res) => {
  const { data } = await require("axios").get(process.env.url_springboot + "/api/arvore", config(req));
  res.set("Cache-Control", "private, no-store").render("index/arvore", { titulo: "Árvore de habilidades", trilha: data });
}));
router.get("/curso/criar", autenticar, wrap(async (req, res) => {
  if (req.usuario.posicao !== "GESTOR" || !req.usuario.departmentId)
    return res.status(403).render("erro", { mensagem: "Cadastro reservado ao gestor da área." });
  const { data } = await require("axios").get(process.env.url_springboot + "/api/arvore", config(req));
  res.set("Cache-Control", "private, no-store").render("index/criarCurso", { titulo: "Cadastrar curso", cursos: data.cursos });
}));
router.post("/api/arvore/cursos", proxy(() => "/api/arvore/cursos"));
router.post("/api/arvore/cursos/:id/concluir", proxy(req => "/api/arvore/cursos/" + encodeURIComponent(req.params.id) + "/concluir"));

router.get("/login", wrap(async (req, res) => {
  res.render("index/login", { titulo: "Login" });
}));

router.get("/perfil", autenticar, wrap(async (req, res) => {
  res.render("index/configurarPerfil", { titulo: "Perfil" });
}));

router.get("/dashboard", autenticar, wrap(async (req, res) => {
  res.render("dashboard/index", {titulo: "Dashboard" });
}));

router.get("/sobre", wrap(async (req, res) => {
  res.render("index/sobre", { titulo: "Sobre Nós" });
}));

module.exports = router;
