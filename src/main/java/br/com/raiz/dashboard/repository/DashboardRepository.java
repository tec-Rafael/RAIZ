package br.com.raiz.dashboard.repository;

import br.com.raiz.dashboard.dto.DashboardDto.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Consultas somente de leitura. Agregações executadas no PostgreSQL, sem baixar logs. */
@Repository
public class DashboardRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public DashboardRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Pessoa> pessoas() {
        return jdbc.query("""
            SELECT e.employee_id, e.nome, e.company_id, c.company_name,
                   e.department_id, d.department_name, e.posicao
            FROM employees e JOIN companies c ON c.company_id = e.company_id
            LEFT JOIN departments d ON d.department_id = e.department_id
            ORDER BY e.nome, e.employee_id
            """, Map.of(), (r, n) -> new Pessoa(r.getString(1), r.getString(2), r.getString(3),
                r.getString(4), r.getString(5), r.getString(6), r.getString(7)));
    }

    public LocalDate primeiraData() {
        return jdbc.queryForObject("SELECT MIN(timestamp)::date FROM ai_usage_logs", Map.of(), LocalDate.class);
    }

    public LocalDate ultimaData(String companyId) {
        return jdbc.queryForObject("SELECT MAX(timestamp)::date FROM ai_usage_logs WHERE company_id = :company",
            Map.of("company", companyId), LocalDate.class);
    }

    public List<Opcao> ferramentas(String companyId) {
        return jdbc.query("""
            SELECT t.ai_tool_id, t.tool_name FROM ai_tools t
            WHERE EXISTS (SELECT 1 FROM ai_usage_logs l WHERE l.ai_tool_id = t.ai_tool_id AND l.company_id = :company)
            ORDER BY t.tool_name
            """, Map.of("company", companyId), (r, n) -> new Opcao(r.getString(1), r.getString(2)));
    }

    public List<Opcao> areas(String companyId) {
        return jdbc.query("SELECT department_id, department_name FROM departments WHERE company_id = :company ORDER BY department_name",
            Map.of("company", companyId), (r, n) -> new Opcao(r.getString(1), r.getString(2)));
    }

    public record Escopo(String company, String departamento, String pessoa, String ferramenta) { }

    private MapSqlParameterSource parametros(Escopo e, Periodo p) {
        var args = new MapSqlParameterSource().addValue("company", e.company())
            .addValue("inicio", p.inicio().atStartOfDay()).addValue("fim", p.fim().plusDays(1).atStartOfDay());
        if (e.departamento() != null) args.addValue("departamento", e.departamento());
        if (e.pessoa() != null) args.addValue("pessoa", e.pessoa());
        if (e.ferramenta() != null) args.addValue("ferramenta", e.ferramenta());
        return args;
    }

    // Somente trechos constantes entram no SQL; todos os valores são parâmetros vinculados.
    private String pessoasWhere(Escopo e) {
        return " e.company_id = :company"
            + (e.departamento() == null ? "" : " AND e.department_id = :departamento")
            + (e.pessoa() == null ? "" : " AND e.employee_id = :pessoa");
    }

    private String usoWhere(Escopo e) {
        return pessoasWhere(e) + " AND l.company_id = :company AND l.timestamp >= :inicio AND l.timestamp < :fim"
            + (e.ferramenta() == null ? "" : " AND l.ai_tool_id = :ferramenta");
    }

    public Metricas metricas(Escopo e, Periodo p) {
        var args = parametros(e, p);
        var uso = jdbc.queryForMap("""
            SELECT COALESCE(SUM(l.request_count),0) requisicoes,
                   COALESCE(SUM(COALESCE(l.input_tokens,0)::bigint + COALESCE(l.output_tokens,0)::bigint),0) tokens,
                   COUNT(*) registros, COUNT(l.latency_ms) cobertura,
                   100.0 * COUNT(*) FILTER (WHERE lower(l.request_status) = 'success') / NULLIF(COUNT(l.request_status),0) sucesso,
                   AVG(l.latency_ms) latencia
            FROM ai_usage_logs l JOIN employees e ON e.employee_id = l.employee_id WHERE
            """ + usoWhere(e), args);
        var issues = entregas(e, p, false);
        var prs = entregas(e, p, true);
        return new Metricas(longo(uso,"requisicoes"), longo(uso,"tokens"), longo(uso,"registros"), longo(uso,"cobertura"),
            decimal(uso,"sucesso"), decimal(uso,"latencia"), longo(issues,"total"), longo(prs,"total"),
            decimal(issues,"ciclo"), decimal(prs,"ciclo"), longo(issues,"cobertura"), longo(prs,"cobertura"));
    }

    private Map<String, Object> entregas(Escopo e, Periodo p, boolean pr) {
        String tabela = pr ? "pull_requests" : "issues";
        String fim = pr ? "merged_at" : "closed_at";
        return jdbc.queryForMap("SELECT COUNT(*) total, "
            + "percentile_cont(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (x." + fim + " - x.created_at))/3600.0) "
            + "FILTER (WHERE x." + fim + " >= x.created_at) ciclo, "
            + "COUNT(*) FILTER (WHERE x." + fim + " >= x.created_at) cobertura "
            + "FROM " + tabela + " x JOIN employees e ON e.employee_id = x.employee_id "
            + "JOIN projects j ON j.project_id = x.project_id AND j.company_id = e.company_id WHERE "
            + pessoasWhere(e) + " AND x." + fim + " >= :inicio AND x." + fim + " < :fim", parametros(e,p));
    }

    public List<Distribuicao> distribuicao(Escopo e, Periodo p) {
        return jdbc.query("""
            SELECT l.ai_tool_id, COALESCE(t.tool_name,l.ai_tool_id), COALESCE(SUM(l.request_count),0),
                   COALESCE(SUM(COALESCE(l.input_tokens,0)::bigint + COALESCE(l.output_tokens,0)::bigint),0)
            FROM ai_usage_logs l JOIN employees e ON e.employee_id = l.employee_id
            LEFT JOIN ai_tools t ON t.ai_tool_id = l.ai_tool_id WHERE
            """ + usoWhere(e) + " GROUP BY l.ai_tool_id, t.tool_name ORDER BY 3 DESC, 1", parametros(e,p),
            (r,n) -> new Distribuicao(r.getString(1), r.getString(2), r.getLong(3), r.getLong(4)));
    }

    public Pagina integrantes(Escopo e, Periodo p, boolean areas, String busca, String ordem, int page, int size) {
        var args = parametros(e,p).addValue("busca", "%" + busca.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%")
            .addValue("limit",size).addValue("offset",(long)page * size);
        String cte = """
            WITH selecionados AS (SELECT e.employee_id, e.nome, e.department_id,
                COALESCE(d.department_name, 'Sem departamento') departamento
                FROM employees e LEFT JOIN departments d ON d.department_id = e.department_id WHERE
            """ + pessoasWhere(e) + "), uso AS ("
            + "SELECT l.employee_id, SUM(l.request_count) requisicoes, SUM(COALESCE(l.input_tokens,0)::bigint + COALESCE(l.output_tokens,0)::bigint) tokens "
            + "FROM ai_usage_logs l JOIN selecionados s ON s.employee_id = l.employee_id "
            + "WHERE l.company_id = :company AND l.timestamp >= :inicio AND l.timestamp < :fim "
            + (e.ferramenta() == null ? "" : "AND l.ai_tool_id = :ferramenta ") + "GROUP BY l.employee_id), "
            + "iss AS (SELECT x.employee_id, COUNT(*) total FROM issues x JOIN selecionados s ON s.employee_id = x.employee_id "
            + "JOIN projects j ON j.project_id = x.project_id AND j.company_id = :company "
            + "WHERE x.closed_at >= :inicio AND x.closed_at < :fim GROUP BY x.employee_id), "
            + "pr AS (SELECT x.employee_id, COUNT(*) total FROM pull_requests x JOIN selecionados s ON s.employee_id = x.employee_id "
            + "JOIN projects j ON j.project_id = x.project_id AND j.company_id = :company "
            + "WHERE x.merged_at >= :inicio AND x.merged_at < :fim GROUP BY x.employee_id), linhas AS ("
            + "SELECT " + (areas ? "COALESCE(s.department_id,'') id, s.departamento nome" : "s.employee_id id, s.nome")
            + ", COUNT(*) pessoas, COALESCE(SUM(u.requisicoes),0) requisicoes, COALESCE(SUM(u.tokens),0) tokens, "
            + "COALESCE(SUM(i.total),0) issues, COALESCE(SUM(p.total),0) prs "
            + "FROM selecionados s LEFT JOIN uso u ON u.employee_id = s.employee_id "
            + "LEFT JOIN iss i ON i.employee_id = s.employee_id LEFT JOIN pr p ON p.employee_id = s.employee_id GROUP BY "
            + (areas ? "s.department_id,s.departamento" : "s.employee_id,s.nome") + ") ";
        String filtro = " FROM linhas WHERE nome ILIKE :busca ESCAPE '!'";
        long total = jdbc.queryForObject(cte + "SELECT COUNT(*)" + filtro,args,Long.class);
        String order = switch (ordem) { case "issues" -> "issues DESC"; case "prs" -> "prs DESC";
            case "nome" -> "nome ASC"; default -> "requisicoes DESC"; };
        List<Linha> rows = jdbc.query(cte + "SELECT *" + filtro + " ORDER BY " + order + ", id LIMIT :limit OFFSET :offset",args,
            (r,n) -> new Linha(r.getString("id"),r.getString("nome"),r.getLong("pessoas"),r.getLong("requisicoes"),
                r.getLong("tokens"),r.getLong("issues"),r.getLong("prs")));
        return new Pagina(rows,total,page,size);
    }

    private static long longo(Map<String,Object> row,String key) { return ((Number)row.get(key)).longValue(); }
    private static Double decimal(Map<String,Object> row,String key) { return row.get(key) == null ? null : ((Number)row.get(key)).doubleValue(); }
}
