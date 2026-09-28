package br.com.raiz.score.dto;

public record FinOpsScoreDto(
        Double tokenEfficiency,
        Double requestEfficiency,
        Double finOpsScore
) {
}