package br.com.raiz.score.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "employeeId",
        "periodo",
        "tokenEfficiency",
        "requestEfficiency",
        "finOpsScore"
})
public record FinOpsScoreDto(
        String employeeId,
        PeriodoResponseDto periodo,
        Double tokenEfficiency,
        Double requestEfficiency,
        Double finOpsScore
) {

    // Mantém compatibilidade com o FinOpsService atual
    public FinOpsScoreDto(
            Double tokenEfficiency,
            Double requestEfficiency,
            Double finOpsScore
    ) {
        this(
                null,
                null,
                tokenEfficiency,
                requestEfficiency,
                finOpsScore
        );
    }
}