document.querySelectorAll('.tr-no:not(:disabled)').forEach(botao => {
    botao.addEventListener('click', () => document.getElementById(botao.dataset.dialog).showModal());
});
document.querySelectorAll('.tr-fechar').forEach(botao => {
    botao.addEventListener('click', () => botao.closest('dialog').close());
});
document.querySelectorAll('form[data-rota]').forEach(form => {
    form.addEventListener('submit', async event => {
        event.preventDefault();
        const dados = Object.fromEntries(new FormData(form));
        if (dados.nivel) dados.nivel = Number(dados.nivel);
        const botao = form.querySelector('button');
        botao.disabled = true;
        try {
            const r = await fetch(form.dataset.rota, {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(dados)});
            if (r.status === 401) return location.assign('/login');
            if (r.ok) return location.assign('/arvore');
            const erro = await r.json().catch(() => ({}));
            form.querySelector('output').textContent = r.status === 422 ? 'Releia a aula e tente novamente.' : erro.message || 'Confira as informações e tente novamente.';
        } catch {
            form.querySelector('output').textContent = 'Falha de conexão. Tente novamente.';
        } finally { botao.disabled = false; }
    });
});
const painel = document.querySelector('.tr-pagina');
if (painel) {
    const ns = 'http://www.w3.org/2000/svg';
    const linhas = document.createElementNS(ns, 'svg');
    linhas.classList.add('tr-links');
    linhas.setAttribute('aria-hidden','true');
    painel.prepend(linhas);
    function conectar() {
        linhas.replaceChildren();
        const origem = painel.getBoundingClientRect();
        let anterior;
        for (const nivel of painel.querySelectorAll('.tr-nivel')) {
            const filas = new Map();
            for (const img of nivel.querySelectorAll('.tr-no img')) {
                const r = img.getBoundingClientRect();
                const y = Math.round(r.top - origem.top + r.height / 2);
                if (!filas.has(y)) filas.set(y, []);
                filas.get(y).push(Math.round(r.left - origem.left + r.width / 2));
            }
            for (const [y, xs] of filas) {
                const centro = origem.width / 2;
                const d = ['M', Math.min(centro,...xs), y, 'H', Math.max(centro,...xs)];
                if (anterior !== undefined) d.push('M',centro,anterior,'V',y);
                const ramo = document.createElementNS(ns,'path');
                ramo.setAttribute('d',d.join(' '));
                ramo.setAttribute('stroke','#dcaa4e');
                ramo.setAttribute('stroke-width','3');
                ramo.setAttribute('fill','none');
                linhas.append(ramo);
                anterior = y;
            }
        }
    }
    new ResizeObserver(conectar).observe(painel);
    window.addEventListener('load',conectar);
}
