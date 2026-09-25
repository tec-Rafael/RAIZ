/* =============================================================================
   Dashboard rAIz — página /dashboard
   -----------------------------------------------------------------------------
   A página só conversa com as rotas /api/dashboard/... (routes/api/dashboardApi.js).
   Hoje essas rotas devolvem dados simulados; quando o backend Java tiver os
   endpoints, a troca acontece lá, sem mudar nada neste arquivo.

   Fluxo: perfil simulado → /api/dashboard/filtros (o que o perfil pode ver)
          → filtros escolhidos → /api/dashboard/{pessoal|equipe|area|empresa}
          → cards e gráficos (Chart.js).
   ============================================================================= */
(function () {
	"use strict";

	const API = "/api/dashboard";
	const PERIODO_PADRAO = 6;

	const elSimularComo = document.getElementById("dashSimularComo");
	const elUsuarioSimulado = document.getElementById("dashUsuarioSimulado");
	const elFiltros = document.getElementById("dashFiltros");
	const elContexto = document.getElementById("dashContexto");
	const elConteudo = document.getElementById("dashConteudo");

	// ---------------------------------------------------------------------------
	// Utilitários
	// ---------------------------------------------------------------------------

	const fmtNumero = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 0 });
	const fmtCompacto = new Intl.NumberFormat("pt-BR", { notation: "compact", maximumFractionDigits: 1 });
	const fmtDecimal = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1, minimumFractionDigits: 1 });

	const fmtInteiro = function (v) { return fmtNumero.format(v); };
	const fmtPontos = function (v) { return fmtDecimal.format(v); };
	const fmtTokens = function (v) { return fmtCompacto.format(v); };

	function esc(texto) {
		return String(texto)
			.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
			.replace(/"/g, "&quot;").replace(/'/g, "&#39;");
	}

	function corToken(nome, padrao) {
		const v = getComputedStyle(document.documentElement).getPropertyValue(nome).trim();
		return v || padrao;
	}

	const COR_PRIMARIA = corToken("--color-amazon", "#1E7349");
	const COR_TEXTO_SECUNDARIO = "#5c5c55";
	const COR_GRADE = "#ebe9df";

	function alerta(mensagem) {
		return '<div class="dashAlerta" role="alert"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><span>' + esc(mensagem) + "</span></div>";
	}

	async function buscarJson(url) {
		const resposta = await fetch(url);
		const corpo = await resposta.json().catch(function () { return null; });
		if (!resposta.ok) {
			throw new Error(typeof corpo === "string" ? corpo : (corpo && corpo.message) || "Não foi possível carregar os dados do dashboard.");
		}
		return corpo;
	}

	function textoPeriodo(rotulos) {
		const p = estado.filtros.periodos.find(function (x) { return x.meses === estado.periodo; });
		const intervalo = rotulos.length > 1 ? rotulos[0] + " – " + rotulos[rotulos.length - 1] : rotulos[0];
		return (p ? p.texto : estado.periodo + " meses") + " (" + intervalo + ")";
	}

	function textoVariacao(valor, sufixo) {
		if (valor === null || valor === undefined || !isFinite(valor)) return "";
		const sobe = valor >= 0;
		return '<span class="dashKpi__variacao"><i class="bi ' + (sobe ? "bi-arrow-up-right" : "bi-arrow-down-right") + '" aria-hidden="true"></i>' +
			(sobe ? "+" : "") + fmtDecimal.format(valor) + sufixo + " vs. período anterior</span>";
	}

	// ---------------------------------------------------------------------------
	// Componentes de HTML
	// ---------------------------------------------------------------------------

	function kpi(icone, titulo, valor, detalhe, extraHtml) {
		return '<div class="col-12 col-sm-6 col-xl-3"><div class="dashCard dashKpi">' +
			'<div class="dashKpi__topo"><i class="bi ' + icone + '" aria-hidden="true"></i><span>' + esc(titulo) + "</span></div>" +
			'<div class="dashKpi__valor">' + valor + "</div>" +
			(detalhe ? '<div class="dashKpi__detalhe">' + detalhe + "</div>" : "") +
			(extraHtml || "") +
			"</div></div>";
	}

	function cardGrafico(colunas, id, titulo, subtitulo, descricao, alturaClasse) {
		return '<div class="' + colunas + '"><div class="dashCard">' +
			'<h2 class="dashCard__titulo">' + esc(titulo) + "</h2>" +
			'<p class="dashCard__subtitulo">' + esc(subtitulo) + "</p>" +
			'<div class="dashGrafico ' + (alturaClasse || "") + '"><canvas id="' + id + '" role="img" aria-label="' + esc(descricao) + '"></canvas></div>' +
			"</div></div>";
	}

	function seloProvisorio(texto) {
		return '<span class="dashSelo" title="' + esc(texto) + '"><i class="bi bi-info-circle" aria-hidden="true"></i>Provisório</span>';
	}

	// ---------------------------------------------------------------------------
	// Gráficos (Chart.js): uma série por gráfico, cor primária da paleta,
	// barras finas com ponta arredondada, linhas de 2px, grade discreta.
	// ---------------------------------------------------------------------------

	const graficosAtivos = [];

	function destruirGraficos() {
		while (graficosAtivos.length) graficosAtivos.pop().destroy();
	}

	function opcoesBase(formatar, horizontal, eixoMax) {
		const eixoValor = {
			beginAtZero: true,
			max: eixoMax,
			grid: { color: COR_GRADE, drawTicks: false },
			border: { display: false },
			ticks: { color: COR_TEXTO_SECUNDARIO, padding: 6, maxTicksLimit: 6, maxRotation: 0, callback: function (v) { return formatar(v); } }
		};
		const eixoCategoria = {
			grid: { display: false },
			border: { color: COR_GRADE },
			ticks: { color: COR_TEXTO_SECUNDARIO, maxRotation: 0, autoSkip: true }
		};

		return {
			responsive: true,
			maintainAspectRatio: false,
			indexAxis: horizontal ? "y" : "x",
			animation: { duration: 300 },
			interaction: { mode: "index", intersect: false },
			plugins: {
				legend: { display: false },
				tooltip: {
					backgroundColor: "#1e1e1e",
					padding: 10,
					displayColors: false,
					callbacks: { label: function (ctx) { return formatar(horizontal ? ctx.parsed.x : ctx.parsed.y); } }
				}
			},
			scales: horizontal ? { x: eixoValor, y: eixoCategoria } : { x: eixoCategoria, y: eixoValor }
		};
	}

	function criarGrafico(id, config) {
		const canvas = document.getElementById(id);
		if (!canvas) return;
		if (typeof window.Chart === "undefined") {
			canvas.parentElement.innerHTML = '<p class="dashGrafico__erro">Não foi possível carregar o gráfico. Verifique a conexão e recarregue a página.</p>';
			return;
		}
		graficosAtivos.push(new window.Chart(canvas, config));
	}

	function graficoBarras(id, rotulos, valores, opcoes) {
		const horizontal = !!opcoes.horizontal;
		criarGrafico(id, {
			type: "bar",
			data: {
				labels: rotulos,
				datasets: [{
					label: opcoes.nomeSerie,
					data: valores,
					backgroundColor: COR_PRIMARIA,
					borderRadius: horizontal ? { topRight: 4, bottomRight: 4 } : { topLeft: 4, topRight: 4 },
					borderSkipped: false,
					maxBarThickness: horizontal ? 18 : 28,
					categoryPercentage: 0.7
				}]
			},
			options: opcoesBase(opcoes.formatar, horizontal, opcoes.max)
		});
	}

	function graficoLinha(id, rotulos, valores, opcoes) {
		criarGrafico(id, {
			type: "line",
			data: {
				labels: rotulos,
				datasets: [{
					label: opcoes.nomeSerie,
					data: valores,
					borderColor: COR_PRIMARIA,
					backgroundColor: COR_PRIMARIA,
					borderWidth: 2,
					pointRadius: 4,
					pointHoverRadius: 6,
					pointBorderColor: "#ffffff",
					pointBorderWidth: 2,
					tension: 0.25
				}]
			},
			options: opcoesBase(opcoes.formatar, false, opcoes.max)
		});
	}

	// ---------------------------------------------------------------------------
	// Estado
	// ---------------------------------------------------------------------------

	let estado = null;
	let requisicaoAtual = 0;   // descarta respostas antigas quando o usuário troca filtros rápido

	function estadoInicial(filtros) {
		const primeiraArea = filtros.areas ? filtros.areas[0] : null;
		return {
			perfil: filtros.perfil,
			filtros: filtros,
			periodo: PERIODO_PADRAO,
			visao: filtros.perfil === "gestor" ? "geral" : "pessoal",   // líder e gestor
			pessoaId: filtros.usuario.id,                                // líder
			escopo: "empresa",                                           // administrador
			areaId: primeiraArea ? primeiraArea.id : null,
			adminVisaoArea: "geral",
			adminPessoaId: primeiraArea ? primeiraArea.pessoas[0].id : null
		};
	}

	// Qual dashboard buscar, conforme perfil e filtros.
	function resolverAlvo() {
		const f = estado.filtros;
		switch (estado.perfil) {
			case "colaborador":
				return { tipo: "pessoal", id: f.usuario.id };
			case "lider":
				return estado.visao === "geral" ? { tipo: "equipe", id: f.equipe.id } : { tipo: "pessoal", id: estado.pessoaId };
			case "gestor":
				return estado.visao === "geral" ? { tipo: "area", id: f.area.id } : { tipo: "pessoal", id: f.usuario.id };
			case "administrador":
				if (estado.escopo === "empresa") return { tipo: "empresa" };
				return estado.adminVisaoArea === "geral" ? { tipo: "area", id: estado.areaId } : { tipo: "pessoal", id: estado.adminPessoaId };
			default:
				return null;
		}
	}

	function urlDoAlvo(alvo) {
		const params = "?perfil=" + encodeURIComponent(estado.perfil) + "&meses=" + estado.periodo;
		return API + "/" + alvo.tipo + (alvo.id ? "/" + encodeURIComponent(alvo.id) : "") + params;
	}

	// ---------------------------------------------------------------------------
	// Filtros
	// ---------------------------------------------------------------------------

	function definicaoFiltros() {
		const f = estado.filtros;
		const lista = [];

		if (estado.perfil === "lider") {
			lista.push({ chave: "visao", rotulo: "Visão", valor: estado.visao,
				opcoes: [{ valor: "pessoal", texto: "Pessoal" }, { valor: "geral", texto: "Geral da equipe" }] });
			if (estado.visao === "pessoal") {
				lista.push({ chave: "pessoaId", rotulo: "Pessoa", valor: estado.pessoaId,
					opcoes: f.pessoas.map(function (p) { return { valor: p.id, texto: p.nome }; }) });
			}
		}

		if (estado.perfil === "gestor") {
			lista.push({ chave: "visao", rotulo: "Visão", valor: estado.visao,
				opcoes: [{ valor: "geral", texto: "Geral da área" }, { valor: "pessoal", texto: "Meu dashboard pessoal" }] });
		}

		if (estado.perfil === "administrador") {
			lista.push({ chave: "escopo", rotulo: "Escopo", valor: estado.escopo,
				opcoes: [{ valor: "empresa", texto: "Geral da empresa" }, { valor: "area", texto: "Por área" }] });
			if (estado.escopo === "area") {
				lista.push({ chave: "areaId", rotulo: "Área", valor: estado.areaId,
					opcoes: f.areas.map(function (a) { return { valor: a.id, texto: a.nome }; }) });
				lista.push({ chave: "adminVisaoArea", rotulo: "Visão", valor: estado.adminVisaoArea,
					opcoes: [{ valor: "geral", texto: "Geral da área" }, { valor: "pessoal", texto: "Pessoal" }] });
				if (estado.adminVisaoArea === "pessoal") {
					const area = f.areas.find(function (a) { return a.id === estado.areaId; });
					lista.push({ chave: "adminPessoaId", rotulo: "Pessoa", valor: estado.adminPessoaId,
						opcoes: area.pessoas.map(function (p) { return { valor: p.id, texto: p.nome, grupo: p.grupo }; }) });
				}
			}
		}

		lista.push({ chave: "periodo", rotulo: "Período", valor: String(estado.periodo),
			opcoes: f.periodos.map(function (p) { return { valor: String(p.meses), texto: p.texto }; }) });

		return lista;
	}

	function renderizarFiltros() {
		elFiltros.innerHTML = "";

		definicaoFiltros().forEach(function (f) {
			const id = "dashFiltro_" + f.chave;
			const grupo = document.createElement("div");
			grupo.className = "dashFiltros__item";

			const rotulo = document.createElement("label");
			rotulo.className = "form-label";
			rotulo.setAttribute("for", id);
			rotulo.textContent = f.rotulo;

			const select = document.createElement("select");
			select.className = "form-select form-select-sm";
			select.id = id;

			let optgroupAtual = null;
			f.opcoes.forEach(function (o) {
				const opt = document.createElement("option");
				opt.value = o.valor;
				opt.textContent = o.texto;
				if (o.valor === f.valor) opt.selected = true;
				if (o.grupo) {
					if (!optgroupAtual || optgroupAtual.label !== o.grupo) {
						optgroupAtual = document.createElement("optgroup");
						optgroupAtual.label = o.grupo;
						select.appendChild(optgroupAtual);
					}
					optgroupAtual.appendChild(opt);
				} else {
					select.appendChild(opt);
				}
			});

			select.addEventListener("change", function () { aoMudarFiltro(f.chave, select.value); });

			grupo.appendChild(rotulo);
			grupo.appendChild(select);
			elFiltros.appendChild(grupo);
		});
	}

	function aoMudarFiltro(chave, valor) {
		if (chave === "periodo") {
			estado.periodo = parseInt(valor, 10);
		} else if (chave === "areaId") {
			estado.areaId = valor;
			const area = estado.filtros.areas.find(function (a) { return a.id === valor; });
			estado.adminPessoaId = area.pessoas[0].id;
		} else if (chave === "visao" && estado.perfil === "lider") {
			estado.visao = valor;
			estado.pessoaId = estado.filtros.usuario.id;
		} else {
			estado[chave] = valor;
		}
		atualizar();
	}

	// ---------------------------------------------------------------------------
	// Dashboards
	// ---------------------------------------------------------------------------

	function renderizarPessoal(d) {
		elContexto.innerHTML = "<strong>Dashboard pessoal</strong> · " + esc(d.pessoa.nome) + " · " + esc(d.pessoa.descricao) + " · " + esc(textoPeriodo(d.rotulos));

		const arvore = d.arvore;
		const textoArvore = arvore.ultimoEstagio
			? "Estágio máximo alcançado"
			: "Faltam " + fmtInteiro(arvore.faltamParaProximo) + " AI Credits para o nível " + (arvore.estagio + 1);

		const metasHtml = d.metas.map(function (m) {
			const pct = Math.min(100, Math.round((m.atual / m.alvo) * 100));
			return '<li class="dashMeta">' +
				'<div class="dashMeta__topo"><span>' + esc(m.titulo) + '</span><span class="dashMeta__valor">' + fmtInteiro(m.atual) + "/" + fmtInteiro(m.alvo) + "</span></div>" +
				'<div class="progress" role="progressbar" aria-label="' + esc(m.titulo) + '" aria-valuenow="' + pct + '" aria-valuemin="0" aria-valuemax="100">' +
				'<div class="progress-bar dashMeta__barra" style="width:' + pct + '%"></div></div>' +
				'<div class="dashMeta__unidade">' + esc(m.unidade) + " · " + pct + "%</div></li>";
		}).join("");

		const conquistasHtml = d.conquistas.map(function (c) {
			return '<li class="dashConquista ' + (c.desbloqueada ? "" : "dashConquista--bloqueada") + '" title="' + esc(c.descricao) + '">' +
				'<span class="dashConquista__icone"><i class="bi ' + (c.desbloqueada ? esc(c.icone) : "bi-lock") + '" aria-hidden="true"></i></span>' +
				'<span class="dashConquista__titulo">' + esc(c.titulo) + "</span>" +
				'<span class="dashConquista__estado">' + (c.desbloqueada ? "Desbloqueada" : "Bloqueada") + "</span></li>";
		}).join("");

		elConteudo.innerHTML =
			'<div class="row g-3">' +
				kpi("bi-coin", "AI Credits no período", fmtInteiro(d.creditos), textoVariacao(d.variacaoCreditos, "%")) +
				kpi("bi-mortarboard", "Competência em IA", fmtPontos(d.competencia) + "<small>/100</small>", esc(d.nivel) + " " + seloProvisorio("Faixas de nível ainda em definição")) +
				kpi("bi-cpu", "Tokens consumidos", fmtTokens(d.tokens), textoVariacao(d.variacaoTokens, "%")) +
				kpi("bi-chat-dots", "Interações com IA", fmtInteiro(d.interacoes), "no período selecionado") +
			"</div>" +
			'<div class="row g-3 mt-0">' +
				'<div class="col-12 col-lg-4"><div class="dashCard dashArvore">' +
					'<h2 class="dashCard__titulo">Sua árvore</h2>' +
					// TODO: trocar o ícone pela arte da árvore de cada estágio quando ela estiver na main.
					'<div class="dashArvore__imagem" aria-hidden="true"><i class="bi bi-tree-fill"></i></div>' +
					'<div class="dashArvore__nivel">Nível ' + arvore.estagio + " de " + arvore.totalEstagios + "</div>" +
					'<div class="dashArvore__detalhe">' + fmtInteiro(arvore.creditosAcumulados) + " AI Credits acumulados · " + esc(textoArvore) + "</div>" +
				"</div></div>" +
				cardGrafico("col-12 col-lg-8", "gCreditos", "Evolução de AI Credits", "AI Credits ganhos por mês", "Gráfico de barras com os AI Credits ganhos por mês no período selecionado") +
			"</div>" +
			'<div class="row g-3 mt-0">' +
				cardGrafico("col-12 col-lg-6", "gCompetencias", "Perfil de competência em IA", "Pontuação média no período (0 a 100)", "Gráfico de barras horizontais com a pontuação de cada competência em IA") +
				cardGrafico("col-12 col-lg-6", "gEvolucaoCompetencia", "Evolução da competência", "Média mensal das competências (0 a 100)", "Gráfico de linha com a evolução mensal da competência em IA") +
			"</div>" +
			'<div class="row g-3 mt-0">' +
				cardGrafico("col-12 col-lg-4", "gModelos", "Uso por modelo de IA", "Tokens consumidos no período", "Gráfico de barras horizontais com os tokens consumidos por modelo de IA") +
				'<div class="col-12 col-lg-4"><div class="dashCard"><h2 class="dashCard__titulo">Metas</h2><p class="dashCard__subtitulo">Exemplos simulados</p><ul class="dashMetas">' + metasHtml + "</ul></div></div>" +
				'<div class="col-12 col-lg-4"><div class="dashCard"><h2 class="dashCard__titulo">Conquistas</h2><p class="dashCard__subtitulo">Exemplos simulados</p><ul class="dashConquistas">' + conquistasHtml + "</ul></div></div>" +
			"</div>";

		graficoBarras("gCreditos", d.rotulos, d.serieCreditos, { nomeSerie: "AI Credits", formatar: fmtInteiro });
		graficoBarras("gCompetencias", d.competencias.map(function (c) { return c.nome; }), d.competencias.map(function (c) { return c.valor; }), { nomeSerie: "Pontuação", formatar: fmtPontos, horizontal: true, max: 100 });
		graficoLinha("gEvolucaoCompetencia", d.rotulos, d.serieCompetencia, { nomeSerie: "Competência", formatar: fmtPontos, max: 100 });
		graficoBarras("gModelos", d.modelos.map(function (m) { return m.nome; }), d.modelos.map(function (m) { return m.tokens; }), { nomeSerie: "Tokens", formatar: fmtTokens, horizontal: true });
	}

	function renderizarEquipe(d) {
		elContexto.innerHTML = "<strong>Dashboard geral da equipe</strong> · Equipe " + esc(d.equipe.nome) + " · Área " + esc(d.equipe.area) + " · " + esc(textoPeriodo(d.rotulos));

		const linhasHtml = d.integrantes.map(function (p) {
			const ehUsuario = p.id === estado.filtros.usuario.id;
			return "<tr" + (ehUsuario ? ' class="dashTabela__eu"' : "") + ">" +
				"<td>" + esc(p.nome) + (ehUsuario ? ' <span class="dashTag">Você</span>' : "") + "</td>" +
				"<td>" + (p.perfil === "lider" ? "Líder" : "Colaborador(a)") + "</td>" +
				'<td class="text-end">' + fmtInteiro(p.creditos) + "</td>" +
				'<td class="text-end">' + fmtPontos(p.competencia) + "</td>" +
				'<td class="text-end">' + fmtTokens(p.tokens) + "</td>" +
				'<td class="text-end"><button type="button" class="btn btn-sm dashBtnSecundario" data-ver-pessoa="' + esc(p.id) + '">Ver dashboard</button></td>' +
				"</tr>";
		}).join("");

		elConteudo.innerHTML =
			'<div class="row g-3">' +
				kpi("bi-coin", "AI Credits da equipe", fmtInteiro(d.creditos), textoVariacao(d.variacaoCreditos, "%")) +
				kpi("bi-mortarboard", "Competência média", fmtPontos(d.competencia) + "<small>/100</small>", "média dos integrantes") +
				kpi("bi-cpu", "Tokens consumidos", fmtTokens(d.tokens), textoVariacao(d.variacaoTokens, "%")) +
				kpi("bi-people", "Integrantes", fmtInteiro(d.pessoas), "incluindo o líder") +
			"</div>" +
			'<div class="row g-3 mt-0">' +
				cardGrafico("col-12 col-lg-6", "gCreditos", "Evolução de AI Credits da equipe", "Total de AI Credits por mês", "Gráfico de barras com o total de AI Credits da equipe por mês") +
				cardGrafico("col-12 col-lg-6", "gEvolucaoCompetencia", "Evolução da competência média", "Média mensal dos integrantes (0 a 100)", "Gráfico de linha com a competência média da equipe por mês") +
			"</div>" +
			'<div class="row g-3 mt-0">' +
				'<div class="col-12 col-lg-8"><div class="dashCard"><h2 class="dashCard__titulo">Integrantes</h2><p class="dashCard__subtitulo">Ordenados por AI Credits no período</p>' +
					'<div class="table-responsive"><table class="table dashTabela align-middle"><thead><tr><th scope="col">Nome</th><th scope="col">Perfil</th><th scope="col" class="text-end">AI Credits</th><th scope="col" class="text-end">Competência</th><th scope="col" class="text-end">Tokens</th><th scope="col"><span class="visually-hidden">Ações</span></th></tr></thead><tbody>' + linhasHtml + "</tbody></table></div>" +
				"</div></div>" +
				cardGrafico("col-12 col-lg-4", "gModelos", "Uso por modelo de IA", "Tokens consumidos no período", "Gráfico de barras horizontais com os tokens consumidos pela equipe por modelo de IA") +
			"</div>";

		elConteudo.querySelectorAll("[data-ver-pessoa]").forEach(function (btn) {
			btn.addEventListener("click", function () {
				estado.visao = "pessoal";
				estado.pessoaId = btn.getAttribute("data-ver-pessoa");
				atualizar();
			});
		});

		graficoBarras("gCreditos", d.rotulos, d.serieCreditos, { nomeSerie: "AI Credits", formatar: fmtInteiro });
		graficoLinha("gEvolucaoCompetencia", d.rotulos, d.serieCompetencia, { nomeSerie: "Competência média", formatar: fmtPontos, max: 100 });
		graficoBarras("gModelos", d.modelos.map(function (m) { return m.nome; }), d.modelos.map(function (m) { return m.tokens; }), { nomeSerie: "Tokens", formatar: fmtTokens, horizontal: true });
	}

	// Bloco comum aos dashboards agregados de área e empresa (RF08, RF13–RF16).
	function blocoAgregado(d, rotuloGrupo) {
		const avisoScore = "Cálculo provisório: média simples dos 8 indicadores. A metodologia oficial ainda será definida (RB09).";
		return '<div class="row g-3">' +
				kpi("bi-speedometer2", "AI Maturity Score", fmtPontos(d.score) + "<small>/100</small>", textoVariacao(d.variacaoScore, " pts"), '<div class="mt-2">' + seloProvisorio(avisoScore) + "</div>") +
				kpi("bi-coin", "AI Credits " + rotuloGrupo, fmtInteiro(d.creditos), textoVariacao(d.variacaoCreditos, "%")) +
				kpi("bi-mortarboard", "Competência média", fmtPontos(d.competencia) + "<small>/100</small>", fmtInteiro(d.pessoas) + " pessoas") +
				kpi("bi-cpu", "Tokens consumidos", fmtTokens(d.tokens), textoVariacao(d.variacaoTokens, "%")) +
			"</div>" +
			'<div class="row g-3 mt-0">' +
				cardGrafico("col-12 col-lg-6", "gComposicao", "Composição do AI Maturity Score", "Média de cada indicador no período (0 a 100)", "Gráfico de barras horizontais com a pontuação de cada um dos 8 indicadores de maturidade", "dashGrafico--alto") +
				cardGrafico("col-12 col-lg-6", "gEvolucaoScore", "Evolução do AI Maturity Score", "Score mensal (0 a 100)", "Gráfico de linha com a evolução mensal do AI Maturity Score", "dashGrafico--alto") +
			"</div>";
	}

	function desenharAgregado(d) {
		graficoBarras("gComposicao", d.indicadores.map(function (i) { return i.nome; }), d.indicadores.map(function (i) { return i.valor; }), { nomeSerie: "Pontuação", formatar: fmtPontos, horizontal: true, max: 100 });
		graficoLinha("gEvolucaoScore", d.rotulos, d.serieScore, { nomeSerie: "AI Maturity Score", formatar: fmtPontos, max: 100 });
		graficoBarras("gDistribuicao", d.distribuicao.map(function (x) { return x.nivel; }), d.distribuicao.map(function (x) { return x.quantidade; }), { nomeSerie: "Pessoas", formatar: fmtInteiro });
		graficoBarras("gModelos", d.modelos.map(function (m) { return m.nome; }), d.modelos.map(function (m) { return m.tokens; }), { nomeSerie: "Tokens", formatar: fmtTokens, horizontal: true });
	}

	function linhaDistribuicaoEModelos(rotuloGrupo) {
		return '<div class="row g-3 mt-0">' +
			cardGrafico("col-12 col-lg-6", "gDistribuicao", "Distribuição por nível de competência", "Quantidade de pessoas em cada nível", "Gráfico de barras com a quantidade de pessoas " + rotuloGrupo + " em cada nível de competência") +
			cardGrafico("col-12 col-lg-6", "gModelos", "Uso por modelo de IA", "Tokens consumidos no período", "Gráfico de barras horizontais com os tokens consumidos " + rotuloGrupo + " por modelo de IA") +
			"</div>";
	}

	function renderizarArea(d) {
		elContexto.innerHTML = "<strong>Dashboard geral da área</strong> · " + esc(d.area.nome) + " · " + esc(textoPeriodo(d.rotulos));
		// RF08: visão agregada — nenhuma pessoa é identificada neste dashboard.
		elConteudo.innerHTML = blocoAgregado(d, "da área") + linhaDistribuicaoEModelos("da área");
		desenharAgregado(d);
	}

	function renderizarEmpresa(d) {
		elContexto.innerHTML = "<strong>Dashboard geral da empresa</strong> · " + esc(d.empresa.nome) + " · " + esc(textoPeriodo(d.rotulos));

		const linhasAreas = d.areas.map(function (a) {
			return "<tr><td>" + esc(a.nome) + "</td>" +
				'<td class="text-end">' + fmtPontos(a.score) + "</td>" +
				'<td class="text-end">' + fmtInteiro(a.creditos) + "</td>" +
				'<td class="text-end">' + fmtPontos(a.competencia) + "</td>" +
				'<td class="text-end">' + fmtInteiro(a.pessoas) + "</td></tr>";
		}).join("");

		elConteudo.innerHTML = blocoAgregado(d, "da empresa") +
			'<div class="row g-3 mt-0">' +
				cardGrafico("col-12 col-lg-6", "gComparativoAreas", "AI Maturity Score por área", "Score médio no período (0 a 100)", "Gráfico de barras comparando o AI Maturity Score de cada área") +
				'<div class="col-12 col-lg-6"><div class="dashCard"><h2 class="dashCard__titulo">Resumo por área</h2><p class="dashCard__subtitulo">Valores do período selecionado</p>' +
					'<div class="table-responsive"><table class="table dashTabela align-middle"><thead><tr><th scope="col">Área</th><th scope="col" class="text-end">Score</th><th scope="col" class="text-end">AI Credits</th><th scope="col" class="text-end">Competência</th><th scope="col" class="text-end">Pessoas</th></tr></thead><tbody>' + linhasAreas + "</tbody></table></div>" +
				"</div></div>" +
			"</div>" +
			linhaDistribuicaoEModelos("da empresa");

		desenharAgregado(d);
		graficoBarras("gComparativoAreas", d.areas.map(function (a) { return a.nome; }), d.areas.map(function (a) { return a.score; }), { nomeSerie: "AI Maturity Score", formatar: fmtPontos, max: 100 });
	}

	// ---------------------------------------------------------------------------
	// Orquestração
	// ---------------------------------------------------------------------------

	async function atualizar() {
		const minhaRequisicao = ++requisicaoAtual;

		renderizarFiltros();
		destruirGraficos();
		elConteudo.innerHTML = '<p class="dashCarregando">Carregando…</p>';

		const alvo = resolverAlvo();

		try {
			const dados = await buscarJson(urlDoAlvo(alvo));
			if (minhaRequisicao !== requisicaoAtual) return;

			switch (alvo.tipo) {
				case "pessoal": renderizarPessoal(dados); break;
				case "equipe": renderizarEquipe(dados); break;
				case "area": renderizarArea(dados); break;
				case "empresa": renderizarEmpresa(dados); break;
			}
		} catch (ex) {
			if (minhaRequisicao !== requisicaoAtual) return;
			elContexto.textContent = "";
			elConteudo.innerHTML = alerta(ex.message);
		}
	}

	async function selecionarPerfil(perfil) {
		try {
			const filtros = await buscarJson(API + "/filtros?perfil=" + encodeURIComponent(perfil));
			estado = estadoInicial(filtros);
			elUsuarioSimulado.textContent = filtros.usuario.nome + " · " + filtros.perfilNome;
			await atualizar();
		} catch (ex) {
			elConteudo.innerHTML = alerta(ex.message);
		}
	}

	async function iniciar() {
		try {
			const usuarios = await buscarJson(API + "/simulacao/usuarios");
			usuarios.forEach(function (u) {
				const opt = document.createElement("option");
				opt.value = u.perfil;
				opt.textContent = u.perfilNome;
				elSimularComo.appendChild(opt);
			});
			elSimularComo.addEventListener("change", function () { selecionarPerfil(elSimularComo.value); });
			await selecionarPerfil(usuarios[0].perfil);
		} catch (ex) {
			elConteudo.innerHTML = alerta(ex.message);
		}
	}

	iniciar();
})();
