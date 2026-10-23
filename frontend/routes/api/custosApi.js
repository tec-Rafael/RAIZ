const express = require("express");
const wrap = require("express-async-error-wrapper");
const axios = require("axios");

const router = express.Router();
const urlSpring = process.env.url_springboot;

router.post("/otimizar", wrap(async (req, res) => {
  try {
    const response = await axios.post(urlSpring + "/api/custos/otimizar", req.body);
    res.status(response.status).json(response.data);
  } catch (error) {
    if (error.response) {
      res.status(error.response.status).json(error.response.data);
      return;
    }
    res.status(502).json({ message: "Não foi possível conectar à API de custos." });
  }
}));

module.exports = router;