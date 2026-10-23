package br.com.raiz.custos.service;

import br.com.raiz.custos.dto.CostOptimizationRequest;
import br.com.raiz.custos.dto.CostOptimizationRequest.ProviderInput;
import br.com.raiz.custos.dto.CostOptimizationRequest.ScenarioInput;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class CostOptimizationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    public OptimizationResponse optimize(CostOptimizationRequest request) {
        validateRanges(request);

        List<ScenarioResult> results = request.scenarios().stream()
                .map(scenario -> calculateScenario(scenario, request))
                .toList();

        return new OptimizationResponse(
                "Minimizar o custo total de implantação entre as alternativas que atendem ao prazo.",
                "Custo = horas estimadas x valor-hora x (1 + contingência/100). "
                        + "Uma alternativa é viável quando horas estimadas <= capacidade mensal x prazo em meses.",
                results
        );
    }

    private ScenarioResult calculateScenario(ScenarioInput scenario, CostOptimizationRequest request) {
        List<ProviderResult> providers = request.providers().stream()
                .map(provider -> calculateProvider(provider, scenario, request.contingencyPercent()))
                .toList();

        ProviderResult selected = providers.stream()
                .filter(ProviderResult::feasible)
                .min(Comparator.comparing(ProviderResult::implementationCost))
                .orElse(null);

        if (selected == null) {
            return new ScenarioResult(
                    scenario.name(), scenario.estimatedHours(), scenario.deadlineMonths(),
                    providers, null, null, null, null, null, null, null, null, null, null, null, null, null, null
            );
        }

        BigDecimal implementation = selected.implementationCost();
        BigDecimal maintenanceAnnualMin = percent(implementation, request.annualMaintenancePercentMin());
        BigDecimal maintenanceAnnualMax = percent(implementation, request.annualMaintenancePercentMax());
        BigDecimal maintenanceMonthlyMin = divide(maintenanceAnnualMin, TWELVE);
        BigDecimal maintenanceMonthlyMax = divide(maintenanceAnnualMax, TWELVE);

        BigDecimal monthlyOperatingMin = money(maintenanceMonthlyMin
                .add(request.monthlyHostingCostMin()).add(request.monthlyAiCostMin()));
        BigDecimal monthlyOperatingBase = money(average(maintenanceMonthlyMin, maintenanceMonthlyMax)
                .add(average(request.monthlyHostingCostMin(), request.monthlyHostingCostMax()))
                .add(request.monthlyAiCostBase()));
        BigDecimal monthlyOperatingMax = money(maintenanceMonthlyMax
                .add(request.monthlyHostingCostMax()).add(request.monthlyAiCostMax()));

        BigDecimal yearOneMin = money(implementation.add(maintenanceAnnualMin)
                .add(request.monthlyHostingCostMin().multiply(TWELVE))
                .add(request.monthlyAiCostMin().multiply(TWELVE)));
        BigDecimal yearOneBase = money(implementation.add(average(maintenanceAnnualMin, maintenanceAnnualMax))
                .add(average(request.monthlyHostingCostMin(), request.monthlyHostingCostMax()).multiply(TWELVE))
                .add(request.monthlyAiCostBase().multiply(TWELVE)));
        BigDecimal yearOneMax = money(implementation.add(maintenanceAnnualMax)
                .add(request.monthlyHostingCostMax().multiply(TWELVE))
                .add(request.monthlyAiCostMax().multiply(TWELVE)));

        return new ScenarioResult(
                scenario.name(), scenario.estimatedHours(), scenario.deadlineMonths(),
                providers, selected.providerName(), selected.baseCost(), selected.contingencyCost(),
                implementation, maintenanceAnnualMin, maintenanceAnnualMax,
                maintenanceMonthlyMin, maintenanceMonthlyMax,
                monthlyOperatingMin, monthlyOperatingBase, monthlyOperatingMax,
                yearOneMin, yearOneBase, yearOneMax
        );
    }

    private ProviderResult calculateProvider(ProviderInput provider, ScenarioInput scenario,
                                             BigDecimal contingencyPercent) {
        long capacity = (long) provider.capacityHoursPerMonth() * scenario.deadlineMonths();
        boolean feasible = scenario.estimatedHours() <= capacity;
        long shortfall = Math.max(0L, scenario.estimatedHours() - capacity);

        BigDecimal baseCost = money(provider.hourlyRate()
                .multiply(BigDecimal.valueOf(scenario.estimatedHours())));
        BigDecimal contingencyCost = percent(baseCost, contingencyPercent);

        return new ProviderResult(
                provider.name(), provider.hourlyRate(), capacity, feasible, shortfall,
                baseCost, contingencyCost, money(baseCost.add(contingencyCost))
        );
    }

    private void validateRanges(CostOptimizationRequest request) {
        if (request.annualMaintenancePercentMin().compareTo(request.annualMaintenancePercentMax()) > 0
                || request.monthlyHostingCostMin().compareTo(request.monthlyHostingCostMax()) > 0
                || request.monthlyAiCostMin().compareTo(request.monthlyAiCostBase()) > 0
                || request.monthlyAiCostBase().compareTo(request.monthlyAiCostMax()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe faixas crescentes para manutenção, hospedagem e consumo de IA (mínimo <= base <= máximo). ");
        }
    }

    private BigDecimal percent(BigDecimal value, BigDecimal percent) {
        return money(value.multiply(percent).divide(HUNDRED, 8, RoundingMode.HALF_UP));
    }

    private BigDecimal average(BigDecimal first, BigDecimal second) {
        return divide(first.add(second), new BigDecimal("2"));
    }

    private BigDecimal divide(BigDecimal value, BigDecimal divisor) {
        return money(value.divide(divisor, 8, RoundingMode.HALF_UP));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public record OptimizationResponse(String objective, String rule, List<ScenarioResult> scenarios) { }

    public record ProviderResult(
            String providerName,
            BigDecimal hourlyRate,
            long capacityWithinDeadlineHours,
            boolean feasible,
            long missingCapacityHours,
            BigDecimal baseCost,
            BigDecimal contingencyCost,
            BigDecimal implementationCost
    ) { }

    public record ScenarioResult(
            String scenarioName,
            int estimatedHours,
            int deadlineMonths,
            List<ProviderResult> alternatives,
            String selectedProvider,
            BigDecimal implementationBaseCost,
            BigDecimal contingencyCost,
            BigDecimal implementationCost,
            BigDecimal annualMaintenanceMin,
            BigDecimal annualMaintenanceMax,
            BigDecimal monthlyMaintenanceMin,
            BigDecimal monthlyMaintenanceMax,
            BigDecimal monthlyOperatingMin,
            BigDecimal monthlyOperatingBase,
            BigDecimal monthlyOperatingMax,
            BigDecimal firstYearCostMin,
            BigDecimal firstYearCostBase,
            BigDecimal firstYearCostMax
    ) { }
}