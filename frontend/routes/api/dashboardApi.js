const router = require('express').Router();
const { proxy } = require('../../middleware/autenticacao');
for (const rota of ['usuarios', 'filtros', 'resumo', 'integrantes', 'ams', 'ams/usuarios', 'ams/filtros'])
    router.get('/' + rota, proxy(() => '/api/dashboard/' + rota));
module.exports = router;
