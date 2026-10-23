const express = require("express");
const wrap = require("express-async-error-wrapper");

const router = express.Router();

router.get("/", wrap(async (req, res) => {
  res.render("index/index", { titulo: "Início" });
}));

router.get("/login", wrap(async (req, res) => {
  res.render("index/login", { titulo: "Login" });
}));

router.get("/perfil", wrap(async (req, res) => {
  res.render("index/configurarPerfil", { titulo: "Perfil" });
}));

router.get("/dashboard", wrap(async (req, res) => {
  res.render("dashboard/index", {titulo: "Dashboard" });
}));

router.get("/custos", wrap(async (req, res) => {
  res.render("custos/index", { titulo: "Simulação de custos" });
}));

router.get("/sobre-nos", wrap(async (req, res) => {
  res.render("index/sobre", { titulo: "Sobre Nós" });
}));




  module.exports = router;
