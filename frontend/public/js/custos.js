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

  function roundMoney(amount) {
    return Math.round((amount + Number.EPSILON) * 100) / 100;
  }

  function percentage(amount, percent) {
    return roundMoney(amount * percent / 100);
  }

  function average(first, second) {
    return roundMoney((first + second) / 2);
  }

  function simulate(payload) {
    return {
      scenarios: payload.scenarios.map(scenario => {
        const alternatives = payload.providers.map(provider => {
          const capacity = provider.capacityHoursPerMonth * scenario.deadlineMonths;
          const baseCost = roundMoney(provider.hourlyRate * scenario.estimatedHours);
          const contingencyCost = percentage(baseCost, payload.contingencyPercent);
          return {
            providerName: provider.name,
            hourlyRate: provider.hourlyRate,
            capacityWithinDeadlineHours: capacity,
            feasible: scenario.estimatedHours <= capacity,
            missingCapacityHours: Math.max(0, scenario.estimatedHours - capacity),
            baseCost,
            contingencyCost,
            implementationCost: roundMoney(baseCost + contingencyCost)
          };
        });

        const feasibleAlternatives = alternatives.filter(item => item.feasible);
        const selected = feasibleAlternatives.sort((a, b) => a.implementationCost - b.implementationCost)[0];
        if (!selected) {
          return {
            scenarioName: scenario.name,
            estimatedHours: scenario.estimatedHours,
            deadlineMonths: scenario.deadlineMonths,
            alternatives,
            selectedProvider: null
          };
        }

        const implementation = selected.implementationCost;
        const annualMaintenanceMin = percentage(implementation, payload.annualMaintenancePercentMin);
        const annualMaintenanceMax = percentage(implementation, payload.annualMaintenancePercentMax);
        const monthlyMaintenanceMin = roundMoney(annualMaintenanceMin / 12);
        const monthlyMaintenanceMax = roundMoney(annualMaintenanceMax / 12);
        const monthlyHostingBase = average(payload.monthlyHostingCostMin, payload.monthlyHostingCostMax);

        return {
          scenarioName: scenario.name,
          estimatedHours: scenario.estimatedHours,
          deadlineMonths: scenario.deadlineMonths,
          alternatives,
          selectedProvider: selected.providerName,
          implementationBaseCost: selected.baseCost,
          contingencyCost: selected.contingencyCost,
          implementationCost: implementation,
          annualMaintenanceMin,
          annualMaintenanceMax,
          monthlyMaintenanceMin,
          monthlyMaintenanceMax,
          monthlyOperatingMin: roundMoney(monthlyMaintenanceMin + payload.monthlyHostingCostMin + payload.monthlyAiCostMin),
          monthlyOperatingBase: roundMoney(average(monthlyMaintenanceMin, monthlyMaintenanceMax) + monthlyHostingBase + payload.monthlyAiCostBase),
          monthlyOperatingMax: roundMoney(monthlyMaintenanceMax + payload.monthlyHostingCostMax + payload.monthlyAiCostMax),
          firstYearCostMin: roundMoney(implementation + annualMaintenanceMin + 12 * payload.monthlyHostingCostMin + 12 * payload.monthlyAiCostMin),
          firstYearCostBase: roundMoney(implementation + average(annualMaintenanceMin, annualMaintenanceMax) + 12 * monthlyHostingBase + 12 * payload.monthlyAiCostBase),
          firstYearCostMax: roundMoney(implementation + annualMaintenanceMax + 12 * payload.monthlyHostingCostMax + 12 * payload.monthlyAiCostMax)
        };
      })
    };
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

    if (payload.annualMaintenancePercentMin > payload.annualMaintenancePercentMax
        || payload.monthlyHostingCostMin > payload.monthlyHostingCostMax
        || payload.monthlyAiCostMin > payload.monthlyAiCostBase
        || payload.monthlyAiCostBase > payload.monthlyAiCostMax) {
      errorBox.textContent = "Confira as faixas: mínimo deve ser menor ou igual à base, e a base menor ou igual ao máximo.";
      return;
    }

    render(simulate(payload));
  });
})();
