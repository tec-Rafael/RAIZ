const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ejs = require('ejs');
for (const posicao of ['COLABORADOR','GESTOR','ADMINISTRADOR']) {
    for (const view of ['index/index','index/configurarPerfil','dashboard/index','funcionario/listar','funcionario/criar']) {
        const filename = path.resolve(__dirname,'../views',view + '.ejs');
        const html = ejs.render(fs.readFileSync(filename,'utf8'), {usuario:{employeeId:'C01-E0012',nome:'Pessoa <Teste>',email:'pessoa@example.com',posicao,jobRole:'Cargo'}}, {filename});
        const gestao = html.includes('href="/funcionario/criar"') || html.includes('href="/funcionario/listar"');
        assert.equal(gestao,view === 'index/configurarPerfil' && posicao !== 'COLABORADOR',view + ' / ' + posicao);
        if (view === 'index/configurarPerfil') {
            assert.ok(html.includes('Pessoa &lt;Teste&gt;'));
            assert.ok(html.includes('perfilConsumo') && html.includes('perfilSair'));
        }
    }
}
console.log('Views renderizadas para 3 cargos; gestão aparece somente no perfil autorizado.');
