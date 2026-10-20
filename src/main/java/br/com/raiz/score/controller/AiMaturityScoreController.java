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
            DeliveryMetricsService deliveryMetricsService) {

        this.aiLiteracyService = aiLiteracyService;
        this.governanceService = governanceService;
        this.securityIncidentService = securityIncidentService;
        this.finOpsService = finOpsService;
        this.deliveryMetricsService = deliveryMetricsService;
    }

    @GetMapping("/ai-literacy/{employeeId}")
    @Operation(
            summary = "Calcula o AI Literacy Score",
            description = "Calcula o indicador de AI Literacy de um funcionário para o período informado."
    )
    public AiLiteracyScoreDto calcularAiLiteracy(
            @PathVariable String employeeId,
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fim) {

        Periodo periodo = new Periodo(inicio, fim);

        return aiLiteracyService.calcularAiLiteracy(
                employeeId,
                periodo
        );
    }

    @GetMapping("/governance/{employeeId}")
    @Operation(
            summary = "Calcula o Governance Score",
            description = "Calcula o indicador de governança do uso de IA de um funcionário para o período informado."
    )
    public GovernanceScoreDto calcularGovernanceScore(
            @PathVariable String employeeId,
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fim) {

        Periodo periodo = new Periodo(inicio, fim);

        Double score = governanceService.calcularGovernanceScore(
                employeeId,
                periodo
        );

        return new GovernanceScoreDto(score);
    }

    @GetMapping("/security-incidents/{employeeId}")
    @Operation(
            summary = "Calcula o Security Score",
            description = "Calcula o indicador de segurança do uso de IA de um funcionário para o período informado."
    )
    public SecurityIncidentScoreDto calcularSecurityScore(
            @PathVariable String employeeId,
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fim) {

        Periodo periodo = new Periodo(inicio, fim);

        Double score = securityIncidentService.calcularSecurityScore(
                employeeId,
                periodo
        );

        return new SecurityIncidentScoreDto(score);
    }

    @GetMapping("/finops/{employeeId}")
    @Operation(
            summary = "Calcula o FinOps Score",
            description = "Calcula o indicador de eficiência econômica do uso de IA de um funcionário para o período informado."
    )
    public FinOpsScoreDto calcularFinOpsScore(
            @PathVariable String employeeId,
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fim) {

        Periodo periodo = new Periodo(inicio, fim);

        return finOpsService.calcularFinOpsScore(
                employeeId,
                periodo
        );
    }

    @GetMapping("/delivery/{employeeId}")
    @Operation(
            summary = "Calcula o Delivery Score",
            description = "Calcula os indicadores de entrega de software de um funcionário para o período informado."
    )
    public DeliveryScoreDto calcularDeliveryScore(
            @PathVariable String employeeId,
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fim) {

        Periodo periodo = new Periodo(inicio, fim);

        return deliveryMetricsService.calcularDeliveryScoreFuncionario(
                employeeId,
                periodo
        );
    }



}