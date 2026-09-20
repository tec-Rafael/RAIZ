const API_URL = "http://localhost:8080/api/funcionario";

function alternarOlho() {
    const senha = document.getElementById("inputSenhaLogin");
    const icone = document.getElementById("iconeOlho");

    if (!senha || !icone) return;

    senha.type = senha.type === "password" ? "text" : "password";
    icone.className = senha.type === "password" ? "bi bi-eye" : "bi bi-eye-slash";
}


async function realizarLoginUsuario() {

    let formLoginUsuario = document.getElementById("formLoginUsuario");
    let formData = new FormData(formLoginUsuario)

    let email = formData.get("email")
    let senha = formData.get("senha")


    if (!email) {
        alert("email invalido");
        return;
    } if (!senha) {
        alert("senha invalido")
        return;
    }

    let pessoaLogin = {
        email: email,
        senha: senha
    }

    try {

        let response = await fetch("/api/funcionario/login", {
            method: "POST",
            body: JSON.stringify(pessoaLogin),
            headers: {
                "content-type": "application/json"
            }
        });

        if (!response.ok) {
            alert("Ocorreu um erro ao fazer login(");
            return;
        }

        window.location.href = "/";

    } catch (ex) {
        alert("Ocorreu um erro de rede ao fazer login " + ex.message)
    }


}

