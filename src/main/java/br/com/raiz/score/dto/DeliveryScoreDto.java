package br.com.raiz.score.dto;

import java.time.LocalDate;

public class DeliveryScoreDto {

    private final String employeeId;
    private final String jobRole;
    private final PeriodoDto periodo;
    private final ScoreIndividualDto scoreIndividual;
    private final ScoreRelativoDto scoreRelativo;
    private final Double deliveryScore;

    public DeliveryScoreDto(
            String employeeId,
            String jobRole,
            LocalDate inicio,
            LocalDate fim,
            ScoreIndividualDto scoreIndividual,
            ScoreRelativoDto scoreRelativo,
            Double deliveryScore
    ) {
        this.employeeId = employeeId;
        this.jobRole = jobRole;
        this.periodo = new PeriodoDto(inicio, fim);
        this.scoreIndividual = scoreIndividual;
        this.scoreRelativo = scoreRelativo;
        this.deliveryScore = deliveryScore;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getJobRole() {
        return jobRole;
    }

    public PeriodoDto getPeriodo() {
        return periodo;
    }

    public ScoreIndividualDto getScoreIndividual() {
        return scoreIndividual;
    }

    public ScoreRelativoDto getScoreRelativo() {
        return scoreRelativo;
    }

    public Double getDeliveryScore() {
        return deliveryScore;
    }

    public record PeriodoDto(LocalDate inicio, LocalDate fim) {}

    public record ScoreIndividualDto(
            Double issueResolutionScore,
            Double mergeTimeScore,
            Double reviewRateScore,
            Double prVolumeScore,
            Double total
    ) {}

    public record IndicadorRelativoDto(
            Double score,
            Integer totalFuncionariosNaComparacao
    ) {}

    public record ScoreRelativoDto(
            IndicadorRelativoDto issueResolutionScore,
            IndicadorRelativoDto mergeTimeScore,
            IndicadorRelativoDto reviewRateScore,
            IndicadorRelativoDto prVolumeScore,
            Double total
    ) {}
}