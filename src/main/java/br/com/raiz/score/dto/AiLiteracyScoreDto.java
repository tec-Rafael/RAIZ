package br.com.raiz.score.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "employeeId",
        "periodo",
        "completionScore",
        "trainingScore",
        "diversityScore",
        "aiLiteracyScore"
})
public class AiLiteracyScoreDto {

    private final String employeeId;
    private final PeriodoResponseDto periodo;

    private final Double completionScore;
    private final Double trainingScore;
    private final Double diversityScore;
    private final Double aiLiteracyScore;

    // Mantém o construtor utilizado pelo serviço atual
    public AiLiteracyScoreDto(
            Double completionScore,
            Double trainingScore,
            Double diversityScore,
            Double aiLiteracyScore
    ) {
        this(
                null,
                null,
                completionScore,
                trainingScore,
                diversityScore,
                aiLiteracyScore
        );
    }

    public AiLiteracyScoreDto(
            String employeeId,
            PeriodoResponseDto periodo,
            Double completionScore,
            Double trainingScore,
            Double diversityScore,
            Double aiLiteracyScore
    ) {
        this.employeeId = employeeId;
        this.periodo = periodo;
        this.completionScore = completionScore;
        this.trainingScore = trainingScore;
        this.diversityScore = diversityScore;
        this.aiLiteracyScore = aiLiteracyScore;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public PeriodoResponseDto getPeriodo() {
        return periodo;
    }

    public Double getCompletionScore() {
        return completionScore;
    }

    public Double getTrainingScore() {
        return trainingScore;
    }

    public Double getDiversityScore() {
        return diversityScore;
    }

    public Double getAiLiteracyScore() {
        return aiLiteracyScore;
    }
}