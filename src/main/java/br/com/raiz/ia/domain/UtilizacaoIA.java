package br.com.raiz.ia.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_usage_logs")
public class UtilizacaoIA {

    @Id
    @Column(name = "usage_id", length = 64)
    private String usageId;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "employee_id", length = 64)
    private String employeeId;

    @Column(name = "company_id", length = 64)
    private String companyId;

    @Column(name = "department_id", length = 64)
    private String departmentId;

    @Column(name = "ai_tool_id", length = 64)
    private String aiToolId;

    @Column(name = "model_id", length = 64)
    private String modelId;

    @Column(name = "application", length = 255)
    private String application;

    @Column(name = "request_count")
    private Integer requestCount;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "latency_ms")
    private Double latencyMs;

    @Column(name = "request_status", length = 100)
    private String requestStatus;

    @Column(name = "project_id", length = 64)
    private String projectId;

    @Column(name = "cost_center", length = 100)
    private String costCenter;

    protected UtilizacaoIA() {
        // exigido pelo JPA
    }

    public UtilizacaoIA(String usageId, LocalDateTime timestamp, String employeeId,
                        String companyId, String departmentId, String aiToolId,
                        String modelId, String application, Integer requestCount,
                        Integer inputTokens, Integer outputTokens, Double latencyMs,
                        String requestStatus, String projectId, String costCenter) {
        this.usageId = usageId;
        this.timestamp = timestamp;
        this.employeeId = employeeId;
        this.companyId = companyId;
        this.departmentId = departmentId;
        this.aiToolId = aiToolId;
        this.modelId = modelId;
        this.application = application;
        this.requestCount = requestCount;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.latencyMs = latencyMs;
        this.requestStatus = requestStatus;
        this.projectId = projectId;
        this.costCenter = costCenter;
    }

    public String getUsageId() {
        return usageId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getAiToolId() {
        return aiToolId;
    }

    public String getModelId() {
        return modelId;
    }

    public String getApplication() {
        return application;
    }

    public Integer getRequestCount() {
        return requestCount;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public Double getLatencyMs() {
        return latencyMs;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getCostCenter() {
        return costCenter;
    }
}