package br.com.raiz.ia.dto;

import java.time.LocalDateTime;

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
}