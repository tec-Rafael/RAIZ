(() => {
  const form = document.getElementById("costForm");
  if (!form) return;

  const value = id => Number(document.getElementById(id).value);
  const money = amount => new Intl.NumberFormat("pt-BR", {
    style: "currency", currency: "BRL"
  }).format(amount);
  const results = document.getElementById("costResults");
  const errorBox = document.getElementById("costError");

  function addLine(parent, label, valueText) {
    const p = document.createElement("p");
    const strong = document.createElement("strong");
    strong.textContent = label + ": ";
    p.append(strong, document.createTextNode(valueText));
    parent.appendChild(p);
  }

  function render(data) {
    results.replaceChildren();
    const heading = document.createElement("h2");
    heading.textContent = "Resultados";
    results.appendChild(heading);

    data.scenarios.forEach(scenario => {
      const card = document.createElement("article");
      card.className = "costCard";
      const title = document.createElement("h3");
      title.textContent = scenario.scenarioName;
      card.appendChild(title);

      if (!scenario.selectedProvider) {
        addLine(card, "Resultado", "Nenhuma alternativa atende ao prazo informado.");
      } else {
        addLine(card, "Alternativa escolhida", scenario.selectedProvider);
        addLine(card, "Custo de implantação", money(scenario.implementationCost));
        addLine(card, "Manutenção mensal", `${money(scenario.monthlyMaintenanceMin)} a ${money(scenario.monthlyMaintenanceMax)}`);
        addLine(card, "Operação mensal baixa / base / alta", `${money(scenario.monthlyOperatingMin)} / ${money(scenario.monthlyOperatingBase)} / ${money(scenario.monthlyOperatingMax)}`);
        addLine(card, "Custo do primeiro ano baixo / base / alto", `${money(scenario.firstYearCostMin)} / ${money(scenario.firstYearCostBase)} / ${money(scenario.firstYearCostMax)}`);
      }

      const subtitle = document.createElement("p");
      subtitle.textContent = "Alternativas analisadas";
      card.appendChild(subtitle);
      const list = document.createElement("ul");
      list.className = "costAlternatives";
      scenario.alternatives.forEach(alternative => {
        const item = document.createElement("li");
        item.textContent = `${alternative.providerName}: ${alternative.feasible ? "viável" : `inviável, faltam ${alternative.missingCapacityHours} h`} - ${money(alternative.implementationCost)}`;
        list.appendChild(item);
      });
      card.appendChild(list);
      results.appendChild(card);
    });
  }

  form.addEventListener("submit", async event => {
    event.preventDefault();
    errorBox.textContent = "";
    const payload = {
      contingencyPercent: value("contingencyPercent"),
      annualMaintenancePercentMin: value("maintenanceMin"),
      annualMaintenancePercentMax: value("maintenanceMax"),
      monthlyHostingCostMin: value("hostingMin"),
      monthlyHostingCostMax: value("hostingMax"),
      monthlyAiCostMin: value("aiMin"),
      monthlyAiCostBase: value("aiBase"),
      monthlyAiCostMax: value("aiMax"),
      providers: [
        { name: "Freelancer pleno", hourlyRate: value("freelancerRate"), capacityHoursPerMonth: value("freelancerCapacity") },
        { name: "Software house", hourlyRate: value("houseRate"), capacityHoursPerMonth: value("houseCapacity") }
      ],
      scenarios: [
        { name: "Pequeno", estimatedHours: value("smallHours"), deadlineMonths: value("smallMonths") },
        { name: "Médio", estimatedHours: value("mediumHours"), deadlineMonths: value("mediumMonths") },
        { name: "Grande", estimatedHours: value("largeHours"), deadlineMonths: value("largeMonths") }
      ]
    };

    try {
      const response = await fetch("/api/custos/otimizar", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || data.detail || "Confira as faixas e os valores informados.");
      render(data);
    } catch (error) {
      errorBox.textContent = error.message || "Não foi possível calcular os cenários.";
    }
  });
})();