package br.com.raiz.score.controller;

import br.com.raiz.score.domain.Periodo;
import br.com.raiz.score.dto.SecurityIncidentScoreDto;
import br.com.raiz.score.service.SecurityIncidentService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/score/security-incidents")
public class SecurityIncidentController {

    private final SecurityIncidentService securityIncidentService;

    public SecurityIncidentController(SecurityIncidentService securityIncidentService) {
        this.securityIncidentService = securityIncidentService;
    }

    @GetMapping("/{employeeId}")
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
}