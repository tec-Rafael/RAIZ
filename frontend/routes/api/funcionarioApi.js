const router = require('express').Router();
const { proxy } = require('../../middleware/autenticacao');
for (const [method, rota] of [
    ['post', '/login'], ['post', '/logout'], ['get', '/sessao'], ['get', '/me/consumo-ia'],
    ['get', '/listar'], ['get', '/buscar/:id'], ['post', '/criar'], ['delete', '/deletar/:id']
]) router[method](rota, proxy(req => '/api/funcionario' + req.path));
module.exports = router;
