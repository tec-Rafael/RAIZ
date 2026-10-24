const assert=require('node:assert/strict');
const {origin,login,logout}=require('./sessao-helper.cjs');
const contas=require('./contas-demo.json');
(async()=>{
    const visitante=await fetch(origin+'/',{redirect:'manual'});
    assert.equal(visitante.status,302);assert.equal(visitante.headers.get('location'),'/login');
    for(const conta of [{email:'raiz@gmail.com',senha:'12345'},contas.gestorMaior,contas.desenvolvedor]){
        const sessao=await login(conta.email,conta.senha);
        try{
            const gestor=['GESTOR','ADMINISTRADOR'].includes(sessao.usuario.posicao);
            for(const rota of ['/','/perfil','/dashboard']){
                const resposta=await fetch(origin+rota,{headers:{cookie:sessao.cookie}});
                assert.equal(resposta.status,200,rota);
                const html=await resposta.text();
                assert.equal(html.includes('href="/funcionario/criar"'),rota==='/perfil'&&gestor);
                assert.equal(html.includes('href="/funcionario/listar"'),rota==='/perfil'&&gestor);
                if(rota==='/')assert.ok(html.includes('menuLateralHomePage'));
                else assert.ok(html.includes('rdSidebar'));
            }
        }finally{await logout(sessao.cookie);}
    }
    console.log('Home protegida e navegação por cargo conferidas na API em execução.');
})().catch(e=>{console.error(e);process.exitCode=1;});
