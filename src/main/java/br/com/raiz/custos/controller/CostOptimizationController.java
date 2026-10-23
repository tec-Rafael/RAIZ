package br.com.raiz.custos.controller;

import br.com.raiz.custos.dto.CostOptimizationRequest;
import br.com.raiz.custos.service.CostOptimizationService;
import br.com.raiz.custos.service.CostOptimizationService.OptimizationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/custos")
@Tag(name = "Custos e otimização", description = "Simulação de custos e seleção da alternativa viável de menor custo.")
public class CostOptimizationController {

    private final CostOptimizationService service;

    public CostOptimizationController(CostOptimizationService service) {
        this.service = service;
    }

    @PostMapping("/otimizar")
    @Operation(
            summary = "Otimiza o custo de implantação e simula cenários",
            description = "Compara fornecedores por porte, verifica capacidade dentro do prazo e retorna "
                    + "a alternativa de menor custo, manutenção e orçamento estimado do primeiro ano. "
                    + "Todos os custos são informados em reais."
    )
    public OptimizationResponse optimize(@Valid @RequestBody CostOptimizationRequest request) {
        return service.optimize(request);
    }
}