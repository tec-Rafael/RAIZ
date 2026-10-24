const axios = require('axios');

function config(req) {
    const headers = {};
    for (const nome of ['cookie', 'origin', 'sec-fetch-site']) {
        if (req.headers[nome]) headers[nome] = req.headers[nome];
    }
    return { headers, timeout: 60000 };
}

async function autenticar(req, res, next) {
    try {
        const resposta = await axios.get(process.env.url_springboot + '/api/funcionario/sessao', config(req));
        req.usuario = resposta.data;
        res.locals.usuario = resposta.data;
        next();
    } catch (erro) {
        if (erro.response?.status === 401) {
            res.clearCookie('usuario', { path: '/', httpOnly: true, sameSite: 'lax' });
            return res.redirect('/login');
        }
        res.status(503).render('erro', { mensagem: 'Não foi possível validar a sessão. Verifique se a API RAIZ está disponível.' });
    }
}

function gestao(req, res, next) {
    if (['GESTOR', 'ADMINISTRADOR'].includes(req.usuario.posicao)) return next();
    res.status(403).render('erro', { mensagem: 'Acesso reservado ao gestor e ao administrador.' });
}

// O Java valida novamente a origem e as permissões, inclusive em acesso direto.
function mesmaOrigem(req, res, next) {
    if (!['GET', 'HEAD', 'OPTIONS'].includes(req.method)) {
        const origem = req.get('origin');
        if (req.get('sec-fetch-site') === 'cross-site' || (origem && origem !== req.protocol + '://' + req.get('host'))) {
            return res.status(403).json({ message: 'Origem da solicitação não permitida.' });
        }
    }
    next();
}

function proxy(caminho) {
    return async (req, res) => {
        try {
            const resposta = await axios({ ...config(req), method: req.method,
                url: process.env.url_springboot + caminho(req), params: req.query,
                data: ['GET', 'HEAD'].includes(req.method) ? undefined : req.body });
            if (resposta.headers['set-cookie']) res.setHeader('Set-Cookie', resposta.headers['set-cookie']);
            if (resposta.status === 204) return res.sendStatus(204);
            res.status(resposta.status).json(resposta.data);
        } catch (erro) {
            if (erro.response) res.status(erro.response.status).json(erro.response.data);
            else res.status(502).json({ message: 'Não foi possível consultar a API RAIZ.' });
        }
    };
}

module.exports = { autenticar, gestao, mesmaOrigem, config, proxy };
