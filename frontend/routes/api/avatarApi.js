const router = require('express').Router();
const { proxy } = require('../../middleware/autenticacao');
// Avatar do usuário da sessão: o Java valida a sessão, a origem e os valores enviados.
router.get('/me', proxy(() => '/api/avatar/me'));
router.put('/me', proxy(() => '/api/avatar/me'));
module.exports = router;
