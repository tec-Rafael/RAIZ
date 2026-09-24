package br.com.raiz.ia.repository;

import br.com.raiz.ia.domain.UtilizacaoIA;
import br.com.raiz.ia.dto.UtilizacaoIADto.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Os rankings são calculados NO BANCO (GROUP BY + ORDER BY + LIMIT): a API só
 * recebe as poucas linhas que interessam, em vez de carregar os ~270 mil
 * registros e somar em Java/JavaScript.
 *
 * Regras (valem para as 4 queries de ranking):
 *  - "uso" = SUM(request_count). Uma linha de log pode representar várias
 *    requisições (1 a 300), então contar linhas daria outro resultado.
 *  - só entram no ranking ferramentas/funcionários com pelo menos 1 registro de uso.
 *    Quem nunca usou não aparece nos logs; ele é contado à parte em
 *    contarFuncionariosSemUso().
 *  - companyId é opcional: nulo = todas as empresas. O CAST é necessário para o
 *    PostgreSQL descobrir o tipo do parâmetro quando ele vem nulo.
 *  - empates são desempatados por nome, para o resultado ser sempre o mesmo.
 */
public interface UtilizacaoIARepository
        extends JpaRepository<UtilizacaoIA, String> {

    List<UtilizacaoIA> findByEmployeeId(String employeeId);

    // ------------------------------------------------------------ IAs (ferramentas)

    @Query(nativeQuery = true, value = """
            SELECT t.ai_tool_id AS id, t.tool_name AS nome, SUM(l.request_count) AS "totalRequisicoes"
              FROM ai_usage_logs l
              JOIN ai_tools t ON t.ai_tool_id = l.ai_tool_id
             WHERE (CAST(:companyId AS varchar) IS NULL OR l.company_id = :companyId)
             GROUP BY t.ai_tool_id, t.tool_name
             ORDER BY SUM(l.request_count) DESC, t.tool_name
             LIMIT :limite
            """)
    List<Ranking> ferramentasMaisUsadas(@Param("companyId") String companyId,
                                        @Param("limite") int limite);

    @Query(nativeQuery = true, value = """
            SELECT t.ai_tool_id AS id, t.tool_name AS nome, SUM(l.request_count) AS "totalRequisicoes"
              FROM ai_usage_logs l
              JOIN ai_tools t ON t.ai_tool_id = l.ai_tool_id
             WHERE (CAST(:companyId AS varchar) IS NULL OR l.company_id = :companyId)
             GROUP BY t.ai_tool_id, t.tool_name
             ORDER BY SUM(l.request_count) ASC, t.tool_name
             LIMIT :limite
            """)
    List<Ranking> ferramentasMenosUsadas(@Param("companyId") String companyId,
                                         @Param("limite") int limite);

    // ------------------------------------------------------------ funcionários (só ativos)

    @Query(nativeQuery = true, value = """
            SELECT e.employee_id AS id, e.nome AS nome, SUM(l.request_count) AS "totalRequisicoes"
              FROM ai_usage_logs l
              JOIN employees e ON e.employee_id = l.employee_id
             WHERE e.active = TRUE
               AND (CAST(:companyId AS varchar) IS NULL OR l.company_id = :companyId)
             GROUP BY e.employee_id, e.nome
             ORDER BY SUM(l.request_count) DESC, e.nome, e.employee_id
             LIMIT :limite
            """)
    List<Ranking> funcionariosQueMaisUsam(@Param("companyId") String companyId,
                                          @Param("limite") int limite);

    @Query(nativeQuery = true, value = """
            SELECT e.employee_id AS id, e.nome AS nome, SUM(l.request_count) AS "totalRequisicoes"
              FROM ai_usage_logs l
              JOIN employees e ON e.employee_id = l.employee_id
             WHERE e.active = TRUE
               AND (CAST(:companyId AS varchar) IS NULL OR l.company_id = :companyId)
             GROUP BY e.employee_id, e.nome
             ORDER BY SUM(l.request_count) ASC, e.nome, e.employee_id
             LIMIT :limite
            """)
    List<Ranking> funcionariosQueMenosUsam(@Param("companyId") String companyId,
                                           @Param("limite") int limite);

    /** Funcionários ativos que não têm nenhum registro de uso (não aparecem nos rankings acima). */
    @Query(nativeQuery = true, value = """
            SELECT COUNT(*)
              FROM employees e
             WHERE e.active = TRUE
               AND (CAST(:companyId AS varchar) IS NULL OR e.company_id = :companyId)
               AND NOT EXISTS (SELECT 1 FROM ai_usage_logs l WHERE l.employee_id = e.employee_id)
            """)
    long contarFuncionariosSemUso(@Param("companyId") String companyId);
}