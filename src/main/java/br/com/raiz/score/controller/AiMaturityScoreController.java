package br.com.raiz.score.controller;

import br.com.raiz.score.domain.Periodo;
import br.com.raiz.score.dto.AiLiteracyScoreDto;
import br.com.raiz.score.dto.GovernanceScoreDto;
import br.com.raiz.score.dto.SecurityIncidentScoreDto;
import br.com.raiz.score.dto.FinOpsScoreDto;
import br.com.raiz.score.service.AiLiteracyService;
import br.com.raiz.score.service.GovernanceService;
import br.com.raiz.score.service.SecurityIncidentService;
import br.com.raiz.score.service.FinOpsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import br.com.raiz.score.dto.DeliveryScoreDto;
import br.com.raiz.score.dto.PeriodoResponseDto;
import br.com.raiz.score.service.DeliveryMetricsService;
import java.util.List;

import java.time.LocalDate;

@RestController
@RequestMapping("/score")
@Tag(
        name = "AI Maturity Score",
        description = "Indicadores de maturidade no uso de Inteligência Artificial."
)
public class AiMaturityScoreController {

    private final br.com.raiz.score.service.AiMaturityScoreService amsService;
    private final AiLiteracyService aiLiteracyService;
    private final GovernanceService governanceService;
    private final SecurityIncidentService securityIncidentService;
    private final FinOpsService finOpsService;
    private final DeliveryMetricsService deliveryMetricsService;

    public AiMaturityScoreController(
            AiLiteracyService aiLiteracyService,
            GovernanceService governanceService,
            SecurityIncidentService securityIncidentService,
            FinOpsService finOpsService,
            DeliveryMetricsService deliveryMetricsService,
            br.com.raiz.score.service.AiMaturityScoreService amsService) {

        this.amsService = amsService;
        this.aiLiteracyService = aiLiteracyService;
        this.governanceService = governanceService;
        this.securityIncidentService = securityIncidentService;
        this.finOpsService = finOpsService;
        this.deliveryMetricsService = deliveryMetricsService;
    }

    @GetMapping("/ams/{employeeId}")
    @Operation(summary = "AMS individual com dimensões, pesos e tratamento de notas ausentes")
    public br.com.raiz.score.dto.AmsDto.Individual calcularAMS(@PathVariable String employeeId, @RequestParam int periodo) {
        return amsService.calcularAMS(employeeId, periodo);
    }

    @GetMapping("/ai-literacy/{employeeId}")
    @Operation(
            summary = "Calcula o AI Literacy Score",
            description = "Calcula o AI Literacy Score de um funcionário no período AMS informado."
    )
    public AiLiteracyScoreDto calcularAiLiteracy(
            @PathVariable String employeeId,
            @RequestParam int periodo) {

        Periodo periodoCalculado = amsService.obterPeriodo(periodo);

        AiLiteracyScoreDto resultado =
                aiLiteracyService.calcularAiLiteracy(
                        employeeId,
                        periodoCalculado
                );

        return new AiLiteracyScoreDto(
                employeeId,
                new PeriodoResponseDto(periodo, periodoCalculado),
                resultado.getCompletionScore(),
                resultado.getTrainingScore(),
                resultado.getDiversityScore(),
                resultado.getAiLiteracyScore()
        );
    }

    @GetMapping("/governance/{employeeId}")
    @Operation(
            summary = "Calcula o Governance Score",
            description = "Calcula o indicador de governança de um funcionário no período AMS informado."
    )
    public GovernanceScoreDto calcularGovernanceScore(
            @PathVariable String employeeId,
            @RequestParam int periodo) {

        Periodo periodoCalculado = amsService.obterPeriodo(periodo);

        GovernanceScoreDto resultado =
                governanceService.calcularGovernanceDetalhado(
                        employeeId,
                        periodoCalculado,
                        periodo
                );

        return new GovernanceScoreDto(
                employeeId,
                resultado.getPeriodo(),
                resultado.getRequestsTotais(),
                resultado.getRequestsAprovados(),
                resultado.getGovernanceScore()
        );
    }

    @GetMapping("/security-incidents/{employeeId}")
    @Operation(
            summary = "Calcula o Security Score",
            description = "Calcula o indicador de segurança de um funcionário no período AMS informado."
    )
    public SecurityIncidentScoreDto calcularSecurityScore(
            @PathVariable String employeeId,
            @RequestParam int periodo) {

        Periodo periodoCalculado = amsService.obterPeriodo(periodo);

        return securityIncidentService.calcularSecurityDetalhado(
                employeeId,
                periodoCalculado,
                periodo
        );
    }

    @GetMapping("/finops/{employeeId}")
    @Operation(
            summary = "Calcula o FinOps Score",
            description = "Calcula o indicador de eficiência econômica do uso de IA de um funcionário no período AMS informado."
    )
    public FinOpsScoreDto calcularFinOpsScore(
            @PathVariable String employeeId,
            @RequestParam int periodo) {

        Periodo periodoCalculado = amsService.obterPeriodo(periodo);

        FinOpsScoreDto resultado =
                finOpsService.calcularFinOpsScore(
                        employeeId,
                        periodoCalculado
                );

        return new FinOpsScoreDto(
                employeeId,
                new PeriodoResponseDto(periodo, periodoCalculado),
                resultado.tokenEfficiency(),
                resultado.requestEfficiency(),
                resultado.finOpsScore()
        );
    }

    @GetMapping("/delivery/{employeeId}")
    @Operation(
            summary = "Calcula o Delivery Score",
            description = "Calcula o Delivery Score de um funcionário no período AMS informado."
    )
    public DeliveryScoreDto calcularDeliveryScore(
            @PathVariable String employeeId,
            @RequestParam int periodo) {

        Periodo periodoCalculado = amsService.obterPeriodo(periodo);

        return deliveryMetricsService
                .calcularDeliveryScoreFuncionario(employeeId, periodoCalculado)
                .comNumeroPeriodo(periodo);
    }



}
