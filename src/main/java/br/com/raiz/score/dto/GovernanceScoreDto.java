package br.com.raiz.score.dto;

public class GovernanceScoreDto {

    private final Double governanceScore;

    public GovernanceScoreDto(Double governanceScore) {
        this.governanceScore = governanceScore;
    }

    public Double getGovernanceScore() {
        return governanceScore;
    }
}