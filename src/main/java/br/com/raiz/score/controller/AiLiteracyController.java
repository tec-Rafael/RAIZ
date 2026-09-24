package br.com.raiz.score.controller;

import br.com.raiz.score.domain.Periodo;
import br.com.raiz.score.dto.AiLiteracyScoreDto;
import br.com.raiz.score.service.AiLiteracyService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/score/ai-literacy")
public class AiLiteracyController {

    private final AiLiteracyService aiLiteracyService;

    public AiLiteracyController(AiLiteracyService aiLiteracyService) {
        this.aiLiteracyService = aiLiteracyService;
    }

    @GetMapping("/{employeeId}")
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
}