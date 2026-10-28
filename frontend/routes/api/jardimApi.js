const router = require('express').Router();
const { proxy } = require('../../middleware/autenticacao');
for (const rota of ['estado','ranking']) router.get('/' + rota, proxy(() => '/api/jardim/' + rota));
for (const rota of ['avancar','comprar','adubar','equipar','encerrar','zerar']) router.post('/' + rota, proxy(() => '/api/jardim/' + rota));
module.exports = router;
