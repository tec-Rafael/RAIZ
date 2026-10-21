package br.com.raiz.score.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.LocalDate;

@JsonPropertyOrder({
        "employeeId",
        "periodo",
        "jobRole",
        "scoreIndividual",
        "scoreRelativo",
        "deliveryScore"
})
public class DeliveryScoreDto {

    private final String employeeId;
    private final String jobRole;
    private final PeriodoResponseDto periodo;
    private final ScoreIndividualDto scoreIndividual;
    private final ScoreRelativoDto scoreRelativo;
    private final Double deliveryScore;

    public DeliveryScoreDto(
            String employeeId,
            String jobRole,
            int numeroPeriodo,
            LocalDate inicio,
            LocalDate fim,
            ScoreIndividualDto scoreIndividual,
            ScoreRelativoDto scoreRelativo,
            Double deliveryScore
    ) {
        this.employeeId = employeeId;
        this.jobRole = jobRole;
        this.periodo = new PeriodoResponseDto(
                numeroPeriodo,
                inicio,
                fim
        );
        this.scoreIndividual = scoreIndividual;
        this.scoreRelativo = scoreRelativo;
        this.deliveryScore = deliveryScore;
    }

    // Mantém compatibilidade com o serviço atual
    public DeliveryScoreDto(
            String employeeId,
            String jobRole,
            LocalDate inicio,
            LocalDate fim,
            ScoreIndividualDto scoreIndividual,
            ScoreRelativoDto scoreRelativo,
            Double deliveryScore
    ) {
        this(
                employeeId,
                jobRole,
                0,
                inicio,
                fim,
                scoreIndividual,
                scoreRelativo,
                deliveryScore
        );
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getJobRole() {
        return jobRole;
    }

    public PeriodoResponseDto getPeriodo() {
        return periodo;
    }

    public DeliveryScoreDto comNumeroPeriodo(int numeroPeriodo) {
        return new DeliveryScoreDto(
                employeeId,
                jobRole,
                numeroPeriodo,
                periodo.inicio(),
                periodo.fim(),
                scoreIndividual,
                scoreRelativo,
                deliveryScore
        );
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