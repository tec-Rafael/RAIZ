const router = require('express').Router();
const { proxy } = require('../../middleware/autenticacao');
for (const [method, rota] of [['get','/listar'],['get','/destaques'],['get','/buscar/:id'],['post','/criar'],['delete','/deletar/:id']])
    router[method](rota, proxy(req => '/api/utilizacao-ia' + req.path));
module.exports = router;
