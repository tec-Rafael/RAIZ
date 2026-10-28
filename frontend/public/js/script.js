
function alternarOlho() {
    const senha = document.getElementById("inputSenhaLogin");
    const icone = document.getElementById("iconeOlho");

    if (!senha || !icone) return;

    senha.type = senha.type === "password" ? "text" : "password";
    icone.className = senha.type === "password" ? "bi bi-eye" : "bi bi-eye-slash";
}


let loginEmAndamento = false;
async function realizarLoginUsuario() {
    const formulario = document.getElementById('formLoginUsuario');
    if (loginEmAndamento || !formulario.reportValidity()) return;
    const dados = new FormData(formulario);
    const email = String(dados.get('email') || '').trim();
    const senha = String(dados.get('senha') || '');
    if (!email || !senha) { alert('Preencha o e-mail e a senha.'); return; }
    const botao = formulario.querySelector('.btnLoginPlataforma');
    loginEmAndamento = true;
    botao.disabled = true;
    try {
        const resposta = await fetch('/api/funcionario/login', {
            method: 'POST', headers: { 'content-type': 'application/json' },
            body: JSON.stringify({ email, senha })
        });
        if (!resposta.ok) {
            alert(resposta.status === 401 ? 'E-mail ou senha inválidos.' : resposta.status === 403 ? 'Acesso não permitido para esta conta.' : 'Não foi possível entrar. Verifique se a API está disponível.');
            return;
        }
        window.location.assign('/dashboard');
    } catch (erro) {
        alert('Não foi possível comunicar com o servidor. Tente novamente.');
    } finally {
        loginEmAndamento = false;
        botao.disabled = false;
    }
}

document.getElementById('formLoginUsuario')?.addEventListener('submit', evento => { evento.preventDefault(); realizarLoginUsuario(); });
