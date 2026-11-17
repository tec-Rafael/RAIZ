const router = require('express').Router();
const axios = require('axios');
const { config } = require('../../middleware/autenticacao');

// Pergunta ao assistente do dashboard. O Java valida a sessão, a origem, os filtros e as permissões e
// repassa ao agente orquestrador (Python). A LLM roda localmente e pode levar mais de um minuto: o
// tempo-limite aqui é maior que o do proxy padrão (60 s).
router.post('/perguntar', async (req, res) => {
    try {
        const resposta = await axios({ ...config(req), timeout: 190000, method: 'POST',
            url: process.env.url_springboot + '/api/assistente/perguntar', data: req.body });
        res.status(resposta.status).json(resposta.data);
    } catch (erro) {
        if (erro.response) return res.status(erro.response.status).json(erro.response.data);
        res.status(erro.code === 'ECONNABORTED' ? 504 : 502)
            .json({ detail: erro.code === 'ECONNABORTED' ? 'O assistente demorou demais para responder. Tente uma pergunta mais curta.' : 'Não foi possível consultar a API RAIZ.' });
    }
});

module.exports = router;
