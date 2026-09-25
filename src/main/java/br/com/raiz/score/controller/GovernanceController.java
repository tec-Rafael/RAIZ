package br.com.raiz.score.controller;

import br.com.raiz.score.domain.Periodo;
import br.com.raiz.score.dto.GovernanceScoreDto;
import br.com.raiz.score.service.GovernanceService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/score/governance")
public class GovernanceController {

    private final GovernanceService governanceService;

    public GovernanceController(GovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @GetMapping("/{employeeId}")
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
}