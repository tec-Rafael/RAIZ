package br.com.raiz.score.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_events")
public class SecurityEvent {

    @Id
    @Column(name = "event_id", length = 64)
    private String eventId;

    @Column(name = "company_id", length = 64)
    private String companyId;

    @Column(name = "employee_id", length = 64)
    private String employeeId;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "ai_tool_id", length = 64)
    private String aiToolId;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "severity", length = 50)
    private String severity;

    @Column(name = "policy_id", length = 64)
    private String policyId;

    @Column(name = "action_taken", length = 255)
    private String actionTaken;

    protected SecurityEvent() {

    }

    public SecurityEvent(
            String eventId,
            String companyId,
            String employeeId,
            LocalDateTime timestamp,
            String aiToolId,
            String eventType,
            String severity,
            String policyId,
            String actionTaken) {

        this.eventId = eventId;
        this.companyId = companyId;
        this.employeeId = employeeId;
        this.timestamp = timestamp;
        this.aiToolId = aiToolId;
        this.eventType = eventType;
        this.severity = severity;
        this.policyId = policyId;
        this.actionTaken = actionTaken;
    }

    public String getEventId() {
        return eventId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getAiToolId() {
        return aiToolId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSeverity() {
        return severity;
    }

    public String getPolicyId() {
        return policyId;
    }

    public String getActionTaken() {
        return actionTaken;
    }
}