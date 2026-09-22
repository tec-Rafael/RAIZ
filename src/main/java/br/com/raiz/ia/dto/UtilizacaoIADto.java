package br.com.raiz.ia.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class UtilizacaoIADto {

    private UtilizacaoIADto() {
    }

    public record CriarRequest(

            String usageId,

            LocalDateTime timestamp,

            String employeeId,

            String companyId,

            String departmentId,

            String aiToolId,

            String modelId,

            String application,

            Integer requestCount,

            Integer inputTokens,

            Integer outputTokens,

            Long latencyMs,

            String requestStatus,

            String projectId,

            String costCenter
    ) {
    }

    public record Response(
            String usageId,
            LocalDateTime timestamp,
            String employeeId,
            String companyId,
            String departmentId,
            String aiToolId,
            String modelId,
            String application,
            Integer requestCount,
            Integer inputTokens,
            Integer outputTokens,
            Double latencyMs,
            String requestStatus,
            String projectId,
            String costCenter
    ) {
    }

    /**
     * Uma linha de ranking (serve tanto para ferramenta de IA quanto para funcionário).
     * totalRequisicoes = soma de request_count.
     */
    public record Ranking(
            String id,
            String nome,
            Long totalRequisicoes
    ) {
    }

    /**
     * Resumo pronto para o dashboard: só os N primeiros/últimos de cada ranking,
     * já calculados no banco (nunca a lista inteira de registros).
     */
    public record Destaques(
            List<Ranking> ferramentasMaisUsadas,
            List<Ranking> ferramentasMenosUsadas,
            List<Ranking> funcionariosQueMaisUsam,
            List<Ranking> funcionariosQueMenosUsam,
            long funcionariosSemUso
    ) {
    }
}