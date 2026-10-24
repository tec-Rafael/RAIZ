package br.com.raiz.dashboard.controller;

import br.com.raiz.dashboard.dto.DashboardDto.*;
import br.com.raiz.dashboard.service.DashboardService;
import br.com.raiz.dashboard.service.DashboardService.Consulta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import br.com.raiz.usuario.service.SessaoService;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dados reais para demonstração de perfis. O seletor não autentica usuários.")
public class DashboardController {
    private final DashboardService service;
    private final SessaoService sessoes;
    public DashboardController(DashboardService service,SessaoService sessoes) { this.service = service; this.sessoes=sessoes; }
    private String referencia(String companyId) {
        var u=sessoes.atual();
        if (!u.admin() || companyId==null || companyId.isBlank()) return u.employeeId();
        return service.usuarios().stream().filter(p->p.companyId().equals(companyId)).findFirst()
            .orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Empresa inválida")).id();
    }
    @GetMapping("/usuarios")
    @Operation(summary = "Lista pessoas de referência para demonstração, sem credenciais")
    public List<Pessoa> usuarios() { return filtros(null).pessoas(); }
    @GetMapping("/filtros")
    @Operation(summary = "Opções por visão e cinco períodos a partir do primeiro log do banco")
    public Filtros filtros(@RequestParam(required=false) String companyId) {
        return service.filtros(sessoes.atual().perfil(),referencia(companyId));
    }
    @GetMapping("/resumo")
    @Operation(summary = "KPIs, evolução dos cinco períodos e distribuição por ferramenta")
    public Resumo resumo(@RequestParam(required=false) String companyId,
            @RequestParam(defaultValue = "pessoal") String escopo,
            @RequestParam(required = false) String employeeId, @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String aiToolId, @RequestParam(defaultValue = "5") int periodo) {
        return service.resumo(new Consulta(sessoes.atual().perfil(),referencia(companyId),escopo,employeeId,departmentId,aiToolId,periodo));
    }
    @GetMapping("/integrantes")
    @Operation(summary = "Tabela por funcionário ou área, com busca e paginação")
    public Pagina integrantes(@RequestParam(required=false) String companyId,
            @RequestParam(defaultValue = "pessoal") String escopo,
            @RequestParam(required = false) String employeeId, @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String aiToolId, @RequestParam(defaultValue = "5") int periodo,
            @RequestParam(defaultValue = "pessoa") String agrupamento, @RequestParam(defaultValue = "") String busca,
            @RequestParam(defaultValue = "requisicoes") String ordem,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return service.integrantes(new Consulta(sessoes.atual().perfil(),referencia(companyId),escopo,employeeId,departmentId,aiToolId,periodo),
            agrupamento,busca,ordem,page,size);
    }
}
