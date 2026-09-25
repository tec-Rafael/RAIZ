package br.com.raiz.score.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ApprovedAiToolId implements Serializable {

    @Column(name = "company_id", length = 64)
    private String companyId;

    @Column(name = "ai_tool_id", length = 64)
    private String aiToolId;

    protected ApprovedAiToolId() {
    }

    public ApprovedAiToolId(String companyId, String aiToolId) {
        this.companyId = companyId;
        this.aiToolId = aiToolId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getAiToolId() {
        return aiToolId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApprovedAiToolId)) return false;

        ApprovedAiToolId that = (ApprovedAiToolId) o;

        return Objects.equals(companyId, that.companyId)
                && Objects.equals(aiToolId, that.aiToolId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, aiToolId);
    }
}