package br.com.raiz.score.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "employeeId",
        "periodo",
        "requestsTotais",
        "requestsAprovados",
        "governanceScore"
})
public class GovernanceScoreDto {

    private final String employeeId;
    private final PeriodoResponseDto periodo;
    private final Long requestsTotais;
    private final Long requestsAprovados;
    private final Double governanceScore;

    public GovernanceScoreDto(
            String employeeId,
            PeriodoResponseDto periodo,
            Long requestsTotais,
            Long requestsAprovados,
            Double governanceScore
    ) {
        this.employeeId = employeeId;
        this.periodo = periodo;
        this.requestsTotais = requestsTotais;
        this.requestsAprovados = requestsAprovados;
        this.governanceScore = governanceScore;
    }

    // Mantém compatibilidade com o serviço atual
    public GovernanceScoreDto(
            Long requestsTotais,
            Long requestsAprovados,
            Double governanceScore,
            PeriodoResponseDto periodo
    ) {
        this(
                null,
                periodo,
                requestsTotais,
                requestsAprovados,
                governanceScore
        );
    }

    public GovernanceScoreDto(Double governanceScore) {
        this(null, null, null, null, governanceScore);
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public PeriodoResponseDto getPeriodo() {
        return periodo;
    }

    public Long getRequestsTotais() {
        return requestsTotais;
    }

    public Long getRequestsAprovados() {
        return requestsAprovados;
    }

    public Double getGovernanceScore() {
        return governanceScore;
    }
}