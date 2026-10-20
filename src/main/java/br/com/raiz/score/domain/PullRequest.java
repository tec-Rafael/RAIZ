package br.com.raiz.score.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "pull_requests")
public class PullRequest {

    @Id
    @Column(name = "pull_request_id", length = 64)
    private String pullRequestId;

    @Column(name = "project_id", length = 64)
    private String projectId;

    @Column(name = "employee_id", length = 64)
    private String employeeId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "merged_at")
    private LocalDateTime mergedAt;

    @Column(name = "status", length = 100)
    private String status;

    @Column(name = "files_changed")
    private Integer filesChanged;

    @Column(name = "additions")
    private Integer additions;

    @Column(name = "deletions")
    private Integer deletions;

    @Column(name = "review_count")
    private Double reviewCount;

    protected PullRequest() {
        // Construtor exigido pelo JPA
    }

    public String getPullRequestId() {
        return pullRequestId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getMergedAt() {
        return mergedAt;
    }

    public String getStatus() {
        return status;
    }

    public Integer getFilesChanged() {
        return filesChanged;
    }

    public Integer getAdditions() {
        return additions;
    }

    public Integer getDeletions() {
        return deletions;
    }

    public Double getReviewCount() {
        return reviewCount;
    }
}