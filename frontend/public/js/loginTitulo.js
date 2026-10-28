/*
 * Animação do título da tela de login ("Analisando a IA na raiz da sua empresa.").
 * Usa GSAP 3.13 com SplitText e ScrambleText (carregados via CDN no login.ejs):
 *   - entrada: as letras sobem de trás de uma máscara, uma a uma;
 *   - "IA": embaralha como código e se resolve (repete de tempos em tempos e ao passar o mouse).
 * Sem GSAP (CDN fora do ar) ou com "reduzir movimento" ativo, o título aparece parado.
 */
(function () {
	"use strict";

	const titulo = document.getElementById("tituloLogin");
	if (!titulo) return;

	function mostrarParado() { titulo.classList.remove("anim-pendente"); }

	const reduzirMovimento = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
	if (reduzirMovimento || typeof gsap === "undefined" || typeof SplitText === "undefined") { mostrarParado(); return; }

	gsap.registerPlugin(SplitText);
	const temScramble = typeof ScrambleTextPlugin !== "undefined";
	if (temScramble) gsap.registerPlugin(ScrambleTextPlugin);

	const ia = titulo.querySelector(".tituloLogin__ia");

	// o "IA" fica fora do SplitText para o ScrambleText poder trocar o texto dele
	const partes = Array.prototype.filter.call(titulo.querySelectorAll(".tituloLogin__parte"), function (el) { return el !== ia; });
	const split = SplitText.create(partes, { type: "words,chars", mask: "chars" });

	// largura fixa no "IA" para o texto ao redor não "pular" enquanto ele embaralha
	ia.style.width = ia.getBoundingClientRect().width + "px";
	titulo.classList.remove("anim-pendente");

	function embaralharIA(duracao) {
		if (!temScramble) return gsap.fromTo(ia, { opacity: 0.4 }, { opacity: 1, duration: 0.4 });
		return gsap.to(ia, { duration: duracao || 1.1, scrambleText: { text: "IA", chars: "01<>/{}AI#", revealDelay: 0.35, speed: 0.6 }, ease: "none" });
	}

	const tl = gsap.timeline({ delay: 0.25 });
	tl.from(split.chars, { yPercent: 115, rotate: 10, opacity: 0, duration: 0.7, ease: "back.out(1.7)", stagger: 0.022 })
		.from(ia, { yPercent: 60, opacity: 0, scale: 0.6, duration: 0.55, ease: "back.out(2.2)" }, 0.18)
		.add(embaralharIA(1.3), 0.3);

	// vida contínua: o "IA" se reembaralha de tempos em tempos
	let ocupado = false;
	function pulsar() {
		if (ocupado) return;
		ocupado = true;
		embaralharIA(0.9).eventCallback("onComplete", function () { ocupado = false; });
	}
	tl.eventCallback("onComplete", function () { setInterval(pulsar, 7000); });
	titulo.addEventListener("mouseenter", pulsar);
})();
