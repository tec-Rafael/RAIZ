INSERT INTO employees (employee_id, company_id, department_id, job_role, seniority, hire_date, cost_center, active,
                       nome, email, senha, posicao)
VALUES ('ROOT-E0001', 'C01', 'C01-D01', 'Gerente de Tecnologia', 'Senior', CURRENT_DATE, 'CC-C01-D01', TRUE,
        'Administrador RAIZ', 'root@raiz.local', '$2b$10$zmJoEk.nHcVZULV6KJoFMuhIhaMSCrNiPZiMndUMjrquYuHaEdLMm',
        'gestor') ON CONFLICT (employee_id) DO NOTHING;
