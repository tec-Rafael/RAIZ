package br.com.raiz.usuario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Mapeamento 1:1 da tabela employees (ver database/ai_usage_mysql_updated.sql
 * e database/CriacaoPosicao.sql). Nenhuma coluna foi inventada — todas já
 * existiam no banco fornecido; posicao foi adicionada por migração real
 * (ALTER TABLE) e é a única tratada como Enum no lado Java (PosicaoConverter).
 */
@Entity
@Table(name = "employees")
public class Funcionario {

    @Id
    @Column(name = "employee_id", length = 64)
    private String employeeId;

    @Column(name = "company_id", length = 64)
    private String companyId;

    @Column(name = "department_id", length = 64)
    private String departmentId;

    @Column(name = "job_role", length = 255)
    private String jobRole;

    @Column(length = 100)
    private String seniority;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "cost_center", length = 100)
    private String costCenter;

    private Boolean active;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String senha;

    @Column(length = 100)
    private Posicao posicao;

    protected Funcionario() {
        // exigido pelo JPA
    }

    public Funcionario(String employeeId, String companyId, String departmentId, String jobRole,
                        String seniority, LocalDate hireDate, String costCenter, Boolean active,
                        String nome, String email, String senha, Posicao posicao) {
        this.employeeId = employeeId;
        this.companyId = companyId;
        this.departmentId = departmentId;
        this.jobRole = jobRole;
        this.seniority = seniority;
        this.hireDate = hireDate;
        this.costCenter = costCenter;
        this.active = active;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.posicao = posicao;
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

    public String getJobRole() {
        return jobRole;
    }

    public String getSeniority() {
        return seniority;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public String getCostCenter() {
        return costCenter;
    }

    public Boolean getActive() {
        return active;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenha() {
        return senha;
    }

    public Posicao getPosicao() {
        return posicao;
    }
}
