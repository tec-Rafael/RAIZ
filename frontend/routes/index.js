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

module.exports = router;
