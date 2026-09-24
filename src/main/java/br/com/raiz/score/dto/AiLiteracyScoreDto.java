package br.com.raiz.score.dto;

public class AiLiteracyScoreDto {

    private final Double completionScore;
    private final Double trainingScore;
    private final Double diversityScore;
    private final Double aiLiteracyScore;

    public AiLiteracyScoreDto(
            Double completionScore,
            Double trainingScore,
            Double diversityScore,
            Double aiLiteracyScore) {

        this.completionScore = completionScore;
        this.trainingScore = trainingScore;
        this.diversityScore = diversityScore;
        this.aiLiteracyScore = aiLiteracyScore;
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