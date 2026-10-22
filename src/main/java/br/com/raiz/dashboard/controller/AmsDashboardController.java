package br.com.raiz.dashboard.controller;

import br.com.raiz.dashboard.service.AmsDashboardService;
import br.com.raiz.dashboard.service.AmsDashboardService.Consulta;
import br.com.raiz.score.dto.AmsDto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard/ams")
@Tag(name="Dashboard AMS",description="Demonstração por perfil, com validação dos recortes no servidor")
public class AmsDashboardController {
    private final AmsDashboardService service;
    public AmsDashboardController(AmsDashboardService service){this.service=service;}
    @GetMapping("/usuarios")
    @Operation(summary="Pessoas de referência para demonstração")
    public List<Pessoa> usuarios(){return service.usuarios();}
    @GetMapping("/filtros")
    @Operation(summary="Pessoas e períodos disponíveis para a visão selecionada")
    public Filtros filtros(@RequestParam String perfil,@RequestParam String usuarioId){return service.filtros(perfil,usuarioId);}
    @GetMapping
    @Operation(summary="AMS, evolução, dimensões, melhores notas e comparação por empresa/área")
    public Painel painel(@RequestParam String perfil,@RequestParam String usuarioId,
        @RequestParam(required=false) String companyId,@RequestParam(required=false) String gestorId,
        @RequestParam(required=false) String departmentId,@RequestParam(required=false) String employeeId,
        @RequestParam(defaultValue="5") int periodo,@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="10") int size,@RequestParam(defaultValue="") String busca,
        @RequestParam(defaultValue="ams") String ordem){
        return service.painel(new Consulta(perfil,usuarioId,companyId,gestorId,departmentId,employeeId,periodo,page,size,busca,ordem));
    }
}
