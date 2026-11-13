package br.com.raiz.score.repository;

import br.com.raiz.dashboard.dto.DashboardDto.Periodo;
import br.com.raiz.score.dto.AmsDto.Pessoa;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/** Uma consulta agregada por período; nunca uma consulta por pessoa/log. */
@Repository
public class AmsSourceRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public AmsSourceRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Fonte(Pessoa pessoa, Double seguranca, Double governanca, Double completion,
                         Double training, Double diversity, Double custoToken, Double custoRequest,
                         Double issueDias, Double mergeDias, Double reviewRate, Double volume) { }
    public Map<String,Long> cursosDaArvore(Periodo periodo) {
        Map<String,Long> total = new HashMap<>();
        jdbc.query("SELECT funcionario,count(*) FROM arvore_conclusao WHERE dia BETWEEN :inicio AND :fim GROUP BY funcionario",
            Map.of("inicio",periodo.inicio(),"fim",periodo.fim()), (org.springframework.jdbc.core.RowCallbackHandler)
            r -> total.put(r.getString(1),r.getLong(2)));
        return total;
    }
    public List<Pessoa> pessoas() {
        return jdbc.query("SELECT e.employee_id,e.nome,e.company_id,c.company_name,e.department_id,d.department_name,e.posicao,e.job_role "
            + "FROM employees e JOIN companies c ON c.company_id=e.company_id LEFT JOIN departments d ON d.department_id=e.department_id "
            + "ORDER BY e.nome,e.employee_id",Map.of(),(r,n)->pessoa(r));
    }
    private static Pessoa pessoa(ResultSet r) throws SQLException {
        return new Pessoa(r.getString("employee_id"),r.getString("nome"),r.getString("company_id"),r.getString("company_name"),
            r.getString("department_id"),r.getString("department_name"),r.getString("posicao"),r.getString("job_role"));
    }
    private static Double numero(ResultSet r,String nome) throws SQLException { return r.getObject(nome)==null?null:r.getDouble(nome); }
    public List<Fonte> carregar(Periodo p) {
        return jdbc.query("""
            WITH uso AS (
              SELECT l.employee_id, SUM(COALESCE(l.request_count,0)) requests,
                SUM(COALESCE(l.input_tokens,0)::bigint+COALESCE(l.output_tokens,0)::bigint) tokens,
                SUM(COALESCE(l.input_tokens,0)/1000.0*m.input_price_usd_per_1k_tokens+
                    COALESCE(l.output_tokens,0)/1000.0*m.output_price_usd_per_1k_tokens) custo,
                COUNT(*) FILTER(WHERE m.model_id IS NULL OR m.input_price_usd_per_1k_tokens IS NULL
                    OR m.output_price_usd_per_1k_tokens IS NULL) precos_ausentes,
                SUM(CASE WHEN lower(a.approval_status)='aprovado' THEN COALESCE(l.request_count,0) ELSE 0 END) aprovados
              FROM ai_usage_logs l JOIN employees e ON e.employee_id=l.employee_id AND e.company_id=l.company_id
              LEFT JOIN ai_models m ON m.model_id=l.model_id
              LEFT JOIN approved_ai_tools a ON a.company_id=l.company_id AND a.ai_tool_id=l.ai_tool_id
              WHERE l.timestamp>=:inicio AND l.timestamp<:fim GROUP BY l.employee_id
            ), custos AS (
              SELECT employee_id, 100.0*aprovados/NULLIF(requests,0) governance,
                CASE WHEN precos_ausentes=0 THEN custo*1000/NULLIF(tokens,0) END custo_token,
                CASE WHEN precos_ausentes=0 THEN custo/NULLIF(requests,0) END custo_request FROM uso
            ), seguranca AS (
              SELECT s.employee_id, GREATEST(0,100-SUM(CASE lower(s.severity)
                  WHEN 'baixa' THEN 5 WHEN 'media' THEN 10 WHEN 'alta' THEN 20 WHEN 'critica' THEN 40 ELSE 0 END)) nota
              FROM security_events s JOIN employees e ON e.employee_id=s.employee_id AND e.company_id=s.company_id
              WHERE s.timestamp>=:inicio AND s.timestamp<:fim GROUP BY s.employee_id
            ), cursos AS (
              SELECT employee_id,
                COUNT(*) FILTER(WHERE enrollment_date<=:diaFim AND (completion_date IS NULL OR completion_date>:diaFim)) matriculados,
                COUNT(*) FILTER(WHERE lower(completion_status)='concluido' AND completion_date BETWEEN :diaInicio AND :diaFim) concluidos,
                AVG(score) FILTER(WHERE lower(completion_status)='concluido' AND completion_date BETWEEN :diaInicio AND :diaFim) training,
                COUNT(DISTINCT course_category) FILTER(WHERE lower(completion_status)='concluido' AND completion_date BETWEEN :diaInicio AND :diaFim) categorias
              FROM training_records GROUP BY employee_id
            ), iss AS (
              SELECT x.employee_id, percentile_cont(0.5) WITHIN GROUP(ORDER BY EXTRACT(EPOCH FROM(x.closed_at-x.created_at))/86400.0)
                  FILTER(WHERE x.closed_at>=x.created_at) issue_dias
              FROM issues x JOIN projects j ON j.project_id=x.project_id
              JOIN employees e ON e.employee_id=x.employee_id AND e.company_id=j.company_id
              WHERE x.created_at>=:inicio AND x.created_at<:fim AND x.closed_at>=:inicio AND x.closed_at<:fim GROUP BY x.employee_id
            ), pr AS (
              SELECT x.employee_id, COUNT(*) volume,
                percentile_cont(0.5) WITHIN GROUP(ORDER BY EXTRACT(EPOCH FROM(x.merged_at-x.created_at))/86400.0)
                    FILTER(WHERE x.merged_at>=x.created_at AND x.merged_at<:fim) merge_dias,
                100.0*COUNT(*) FILTER(WHERE x.review_count>=1)/COUNT(*) review_rate
              FROM pull_requests x JOIN projects j ON j.project_id=x.project_id
              JOIN employees e ON e.employee_id=x.employee_id AND e.company_id=j.company_id
              WHERE x.created_at>=:inicio AND x.created_at<:fim GROUP BY x.employee_id
            )
            SELECT e.employee_id,e.nome,e.company_id,c.company_name,e.department_id,d.department_name,e.posicao,e.job_role,
              COALESCE(s.nota,100) seguranca, k.governance,
              100.0*t.concluidos/NULLIF(t.matriculados+t.concluidos,0) completion, t.training,
              CASE WHEN t.categorias>0 THEN LEAST(t.categorias/4.0,1.0)*100 END diversity,
              k.custo_token,k.custo_request,i.issue_dias,p.merge_dias,p.review_rate,p.volume
            FROM employees e JOIN companies c ON c.company_id=e.company_id LEFT JOIN departments d ON d.department_id=e.department_id
            LEFT JOIN custos k ON k.employee_id=e.employee_id LEFT JOIN seguranca s ON s.employee_id=e.employee_id
            LEFT JOIN cursos t ON t.employee_id=e.employee_id LEFT JOIN iss i ON i.employee_id=e.employee_id LEFT JOIN pr p ON p.employee_id=e.employee_id
            ORDER BY e.employee_id
            """,Map.of("inicio",p.inicio().atStartOfDay(),"fim",p.fim().plusDays(1).atStartOfDay(),"diaInicio",p.inicio(),"diaFim",p.fim()),
            (r,n)->new Fonte(pessoa(r),numero(r,"seguranca"),numero(r,"governance"),numero(r,"completion"),numero(r,"training"),numero(r,"diversity"),
                numero(r,"custo_token"),numero(r,"custo_request"),numero(r,"issue_dias"),numero(r,"merge_dias"),numero(r,"review_rate"),numero(r,"volume")));
    }
}
