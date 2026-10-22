const express = require("express");
const axios = require("axios");
const router = express.Router();
// Mesmo proxy das demais APIs. Nenhum fallback para dados simulados.
for (const rota of ["usuarios", "filtros", "resumo", "integrantes", "ams", "ams/usuarios", "ams/filtros"]) {
    router.get("/" + rota, async (req, res) => {
        try {
            const response = await axios.get(process.env.url_springboot + "/api/dashboard/" + rota, {
                params: req.query, timeout: 60000
            });
            res.json(response.data);
        } catch (ex) {
            if (ex.response) res.status(ex.response.status).json(ex.response.data);
            else res.status(502).json({ message: "Não foi possível consultar a API RAIZ. Verifique se o backend está disponível." });
        }
    });
}
module.exports = router;
