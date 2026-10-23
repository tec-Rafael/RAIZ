package br.com.raiz.custos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Dados ajustáveis usados na otimização e simulação do orçamento. */
public record CostOptimizationRequest(
        @NotEmpty @Size(max = 20) List<@Valid ScenarioInput> scenarios,
        @NotEmpty @Size(max = 10) List<@Valid ProviderInput> providers,
        @NotNull @DecimalMin("0.00") BigDecimal contingencyPercent,
        @NotNull @DecimalMin("0.00") BigDecimal annualMaintenancePercentMin,
        @NotNull @DecimalMin("0.00") BigDecimal annualMaintenancePercentMax,
        @NotNull @DecimalMin("0.00") BigDecimal monthlyHostingCostMin,
        @NotNull @DecimalMin("0.00") BigDecimal monthlyHostingCostMax,
        @NotNull @DecimalMin("0.00") BigDecimal monthlyAiCostMin,
        @NotNull @DecimalMin("0.00") BigDecimal monthlyAiCostBase,
        @NotNull @DecimalMin("0.00") BigDecimal monthlyAiCostMax
) {
    public record ScenarioInput(
            @NotNull @Size(min = 1, max = 80) String name,
            @NotNull @Positive Integer estimatedHours,
            @NotNull @Positive Integer deadlineMonths
    ) { }

    public record ProviderInput(
            @NotNull @Size(min = 1, max = 80) String name,
            @NotNull @DecimalMin(value = "0.01") BigDecimal hourlyRate,
            @NotNull @Positive Integer capacityHoursPerMonth
    ) { }
}