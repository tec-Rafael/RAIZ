package br.com.raiz.dashboard.controller;

import br.com.raiz.dashboard.dto.DashboardDto.*;
import br.com.raiz.dashboard.service.DashboardService;
import br.com.raiz.dashboard.service.DashboardService.Consulta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dados reais para demonstração de perfis. O seletor não autentica usuários.")
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }
    @GetMapping("/usuarios")
    @Operation(summary = "Lista pessoas de referência para demonstração, sem credenciais")
    public List<Pessoa> usuarios() { return service.usuarios(); }
    @GetMapping("/filtros")
    @Operation(summary = "Opções por visão e cinco períodos a partir do primeiro log do banco")
    public Filtros filtros(@RequestParam String perfil, @RequestParam String usuarioId) {
        return service.filtros(perfil,usuarioId);
    }
    @GetMapping("/resumo")
    @Operation(summary = "KPIs, evolução dos cinco períodos e distribuição por ferramenta")
    public Resumo resumo(@RequestParam String perfil, @RequestParam String usuarioId,
            @RequestParam(defaultValue = "pessoal") String escopo,
            @RequestParam(required = false) String employeeId, @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String aiToolId, @RequestParam(defaultValue = "5") int periodo) {
        return service.resumo(new Consulta(perfil,usuarioId,escopo,employeeId,departmentId,aiToolId,periodo));
    }
    @GetMapping("/integrantes")
    @Operation(summary = "Tabela por funcionário ou área, com busca e paginação")
    public Pagina integrantes(@RequestParam String perfil, @RequestParam String usuarioId,
            @RequestParam(defaultValue = "pessoal") String escopo,
            @RequestParam(required = false) String employeeId, @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String aiToolId, @RequestParam(defaultValue = "5") int periodo,
            @RequestParam(defaultValue = "pessoa") String agrupamento, @RequestParam(defaultValue = "") String busca,
            @RequestParam(defaultValue = "requisicoes") String ordem,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return service.integrantes(new Consulta(perfil,usuarioId,escopo,employeeId,departmentId,aiToolId,periodo),
            agrupamento,busca,ordem,page,size);
    }
}
