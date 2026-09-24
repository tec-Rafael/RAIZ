package br.com.raiz.score.dto;

public class SecurityIncidentScoreDto {

    private final Double securityScore;

    public SecurityIncidentScoreDto(Double securityScore) {
        this.securityScore = securityScore;
    }

    public Double getSecurityScore() {
        return securityScore;
    }
}