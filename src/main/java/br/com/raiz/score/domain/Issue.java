package br.com.raiz.score.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "issues")
public class Issue {

    @Id
    @Column(name = "issue_id", length = 64)
    private String issueId;

    @Column(name = "project_id", length = 64)
    private String projectId;

    @Column(name = "employee_id", length = 64)
    private String employeeId;

    @Column(name = "issue_type", length = 100)
    private String issueType;

    @Column(name = "priority", length = 100)
    private String priority;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "status", length = 100)
    private String status;

    protected Issue() {
        // Construtor exigido pelo JPA
    }

    public String getIssueId() {
        return issueId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getIssueType() {
        return issueType;
    }

    public String getPriority() {
        return priority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public String getStatus() {
        return status;
    }
}