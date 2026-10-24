package br.com.raiz.usuario.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdministradorInicial implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    public AdministradorInicial(JdbcTemplate jdbc, PasswordEncoder encoder) { this.jdbc=jdbc; this.encoder=encoder; }
    @Override @Transactional public void run(ApplicationArguments args) {
        // Só promove na primeira execução. Reiniciar não troca a senha nem encerra a sessão.
        if (jdbc.queryForObject("SELECT count(*) FROM employees WHERE employee_id='ROOT-E0001' AND lower(posicao)='administrador'",Long.class) == 0) {
            jdbc.update("""
                INSERT INTO employees (employee_id, company_id, department_id, job_role, seniority, hire_date,
                    cost_center, active, nome, email, senha, posicao)
                VALUES ('ROOT-E0001','C01','C01-D01','Administrador do sistema','Senior',CURRENT_DATE,
                    'CC-C01-D01',TRUE,'Administrador RAIZ','raiz@gmail.com',?,'administrador')
                ON CONFLICT (employee_id) DO UPDATE SET nome=EXCLUDED.nome, email=EXCLUDED.email,
                    senha=EXCLUDED.senha, posicao=EXCLUDED.posicao, active=TRUE, token=NULL, token_expira_em=NULL
                """,encoder.encode("12345"));
        }
    }
}
