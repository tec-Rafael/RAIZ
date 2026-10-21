package br.com.raiz.score.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "employeeId",
        "periodo",
        "totalIncidentes",
        "penalidadeAcumulada",
        "securityScore"
})
public class SecurityIncidentScoreDto {

    private final String employeeId;
    private final PeriodoResponseDto periodo;
    private final Integer totalIncidentes;
    private final Double penalidadeAcumulada;
    private final Double securityScore;

    public SecurityIncidentScoreDto(
            String employeeId,
            PeriodoResponseDto periodo,
            Integer totalIncidentes,
            Double penalidadeAcumulada,
            Double securityScore
    ) {
        this.employeeId = employeeId;
        this.periodo = periodo;
        this.totalIncidentes = totalIncidentes;
        this.penalidadeAcumulada = penalidadeAcumulada;
        this.securityScore = securityScore;
    }

    // Mantém compatibilidade com chamadas anteriores
    public SecurityIncidentScoreDto(Double securityScore) {
        this(null, null, null, null, securityScore);
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public PeriodoResponseDto getPeriodo() {
        return periodo;
    }

    public Integer getTotalIncidentes() {
        return totalIncidentes;
    }

    public Double getPenalidadeAcumulada() {
        return penalidadeAcumulada;
    }

    public Double getSecurityScore() {
        return securityScore;
    }
}