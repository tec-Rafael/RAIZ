const express = require("express");
const wrap = require("express-async-error-wrapper");

const router = express.Router();
const { autenticar } = require("../middleware/autenticacao");

router.get("/", autenticar, wrap(async (req, res) => {
  res.render("index/index", { titulo: "Início" });
}));

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
