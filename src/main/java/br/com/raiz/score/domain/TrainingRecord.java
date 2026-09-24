package br.com.raiz.score.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Mapeamento da tabela training_records (ver database/ai_usage_mysql_updated.sql)
 * Nenhuma coluna foi inventada — todas já existiam no banco fornecido;
 * Os dados vão ser utilizados para calcular o AI Literacy Score, um dos componentes do AMS
 */

@Entity
@Table(name = "training_records")
public class TrainingRecord {

    @Id
    @Column(name = "training_id", length = 64)
    private String trainingId;

    @Column(name = "employee_id", length = 64)
    private String employeeId;

    @Column(name = "course_name", length = 255)
    private String courseName;

    // Course Category vai ser utilizado para o Diversity Score, um dos componentes do AMS
    @Column(name = "course_category", length = 100)
    private String courseCategory;

    // Completion Date vai ser usada na separação da nota a cada 90 dias
    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    @Column(name = "completion_status", length = 100)
    private String completionStatus;

    // Nota tirada no curso
    private Double score;

    @Column(name = "duration_hours")
    private Double durationHours;

    protected TrainingRecord() {

    }

    public TrainingRecord(String trainingId, String employeeId, String courseName,
                          String courseCategory, LocalDate completionDate, LocalDate enrollmentDate,
                          String completionStatus, Double score, Double durationHours) {
        this.trainingId = trainingId;
        this.employeeId = employeeId;
        this.courseName = courseName;
        this.courseCategory = courseCategory;
        this.completionDate = completionDate;
        this.enrollmentDate = enrollmentDate;
        this.completionStatus = completionStatus;
        this.score = score;
        this.durationHours = durationHours;
    }

    public String getTrainingId() {
        return trainingId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getCourseCategory() {
        return courseCategory;
    }

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public LocalDate getEnrollmentDate() { return enrollmentDate; }

    public String getCompletionStatus() {
        return completionStatus;
    }

    public Double getScore() {
        return score;
    }

    public Double getDurationHours() {
        return durationHours;
    }
}