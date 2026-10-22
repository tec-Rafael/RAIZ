package br.com.raiz.dashboard.service;

import br.com.raiz.dashboard.dto.DashboardDto.*;
import br.com.raiz.dashboard.repository.DashboardRepository;
import br.com.raiz.dashboard.repository.DashboardRepository.Escopo;
import br.com.raiz.score.service.AiLiteracyService;
import br.com.raiz.score.service.GovernanceService;
import br.com.raiz.score.service.SecurityIncidentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final DashboardRepository repository;
    private final AiLiteracyService literacy;
    private final GovernanceService governance;
    private final SecurityIncidentService security;
    public DashboardService(DashboardRepository repository, AiLiteracyService literacy,
                            GovernanceService governance, SecurityIncidentService security) {
        this.repository = repository; this.literacy = literacy;
        this.governance = governance; this.security = security;
    }
    public List<Pessoa> usuarios() { return repository.pessoas(); }
    public static List<Periodo> periodos(LocalDate inicio) {
        if (inicio == null) return List.of();
        return IntStream.range(0,5).mapToObj(i -> {
            int dias = i == 4 ? 66 : 90;
            LocalDate data = inicio.plusDays(i * 90L);
            return new Periodo(i + 1, data, data.plusDays(dias - 1), dias, i == 4);
        }).toList();
    }
    public Filtros filtros(String perfil, String usuarioId) {
        validarPerfil(perfil);
        List<Pessoa> pessoas = repository.pessoas();
        Pessoa usuario = pessoas.stream().filter(p -> p.id().equals(usuarioId)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Funcionário de referência não encontrado."));
        List<Pessoa> permitidas = pessoas.stream().filter(p -> Objects.equals(p.companyId(),usuario.companyId()))
            .filter(p -> switch (perfil) {
                case "colaborador" -> p.id().equals(usuario.id());
                case "gestor" -> p.id().equals(usuario.id()) || (usuario.departmentId() != null && Objects.equals(p.departmentId(),usuario.departmentId()));
                default -> true;
            }).toList();
        List<Opcao> areas = repository.areas(usuario.companyId()).stream()
            .filter(a -> perfil.equals("administrador") || a.id().equals(usuario.departmentId())).toList();
        return new Filtros(usuario,perfil,periodos(repository.primeiraData()),areas,permitidas,
            repository.ferramentas(usuario.companyId()),repository.ultimaData(usuario.companyId()),true);
    }
    public record Consulta(String perfil, String usuarioId, String escopo, String employeeId,
                            String departmentId, String aiToolId, int periodo) { }
    public Resumo resumo(Consulta q) {
        Filtros filtros = filtros(q.perfil(),q.usuarioId());
        Escopo e = resolverEscopo(q,filtros);
        Periodo periodo = selecionarPeriodo(filtros.periodos(),q.periodo());
        List<Evolucao> evolucao = filtros.periodos().stream().map(p -> new Evolucao(p,repository.metricas(e,p))).toList();
        Map<String,Double> scores = new LinkedHashMap<>();
        // Os serviços atuais definem scores individuais, não um score organizacional.
        if (e.pessoa() != null) {
            var intervalo = new br.com.raiz.score.domain.Periodo(periodo.inicio(),periodo.fim());
            scores.put("literacy",literacy.calcularAiLiteracyScore(e.pessoa(),intervalo));
            scores.put("governance",governance.calcularGovernanceScore(e.pessoa(),intervalo));
            scores.put("security",security.calcularSecurityScore(e.pessoa(),intervalo));
        }
        return new Resumo(periodo,evolucao.get(q.periodo()-1).metricas(),
            q.periodo() > 1 ? evolucao.get(q.periodo()-2).metricas() : null,evolucao,
            repository.distribuicao(e,periodo),scores,e.pessoa() != null,filtros.ultimaData());
    }
    public Pagina integrantes(Consulta q, String agrupamento, String busca, String ordem, int page, int size) {
        if (!Set.of("pessoa","area").contains(agrupamento)) throw erro("Agrupamento inválido.");
        if (!Set.of("nome","requisicoes","issues","prs").contains(ordem)) throw erro("Ordenação inválida.");
        if (page < 0 || size < 1 || size > 100 || busca.length() > 150) throw erro("Paginação ou busca inválida.");
        if (agrupamento.equals("area") && !q.perfil().equals("administrador")) throw proibido();
        Filtros filtros = filtros(q.perfil(),q.usuarioId());
        Escopo e = resolverEscopo(q,filtros);
        return repository.integrantes(e,selecionarPeriodo(filtros.periodos(),q.periodo()),agrupamento.equals("area"),busca,ordem,page,size);
    }
    static Periodo selecionarPeriodo(List<Periodo> periodos, int id) {
        if (id < 1 || id > 5) throw erro("Período inválido. Use 1 a 5.");
        if (periodos.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Não há registros de uso de IA para definir os períodos.");
        return periodos.get(id-1);
    }
    static Escopo resolverEscopo(Consulta q, Filtros f) {
        validarPerfil(q.perfil());
        Pessoa u = f.usuario();
        String pessoa = null, departamento = null;
        switch (q.escopo()) {
            case "pessoal" -> {
                pessoa = q.employeeId() == null || q.employeeId().isBlank() ? u.id() : q.employeeId();
                final String id = pessoa;
                if (f.pessoas().stream().noneMatch(p -> p.id().equals(id))) throw proibido();
            }
            case "equipe" -> {
                if (!q.perfil().equals("gestor") || u.departmentId() == null) throw proibido();
                departamento = u.departmentId();
            }
            case "area" -> {
                if (!q.perfil().equals("administrador")) throw proibido();
                departamento = q.departmentId();
                final String id = departamento;
                if (id == null || f.areas().stream().noneMatch(a -> a.id().equals(id))) throw erro("Área inválida.");
            }
            case "empresa" -> { if (!q.perfil().equals("administrador")) throw proibido(); }
            default -> throw erro("Escopo inválido.");
        }
        String ferramenta = q.aiToolId() == null || q.aiToolId().isBlank() ? null : q.aiToolId();
        if (ferramenta != null && f.ferramentas().stream().noneMatch(t -> t.id().equals(ferramenta))) throw erro("Ferramenta inválida.");
        return new Escopo(u.companyId(),departamento,pessoa,ferramenta);
    }
    private static void validarPerfil(String perfil) {
        if (!Set.of("colaborador","gestor","administrador").contains(perfil)) throw erro("Perfil inválido.");
    }
    private static ResponseStatusException erro(String mensagem) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,mensagem); }
    private static ResponseStatusException proibido() { return new ResponseStatusException(HttpStatus.FORBIDDEN,"Escopo fora da visão demonstrada."); }
}
