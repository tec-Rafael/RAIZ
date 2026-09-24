package br.com.raiz.score.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "approved_ai_tools")
public class ApprovedAiTool {

    @EmbeddedId
    private ApprovedAiToolId id;

    @Column(name = "approval_status", length = 50)
    private String approvalStatus;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Column(name = "allowed_departments")
    private String allowedDepartments;

    protected ApprovedAiTool() {
    }

    public ApprovedAiTool(
            String companyId,
            String aiToolId,
            String approvalStatus,
            LocalDate approvalDate,
            String allowedDepartments) {

        this.id = new ApprovedAiToolId(companyId, aiToolId);
        this.approvalStatus = approvalStatus;
        this.approvalDate = approvalDate;
        this.allowedDepartments = allowedDepartments;
    }

    public String getCompanyId() {
        return id.getCompanyId();
    }

    public String getAiToolId() {
        return id.getAiToolId();
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public LocalDate getApprovalDate() {
        return approvalDate;
    }

    public String getAllowedDepartments() {
        return allowedDepartments;
    }
}