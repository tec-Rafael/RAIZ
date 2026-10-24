package br.com.raiz.usuario.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
@ConditionalOnProperty(name="raiz.demo.contas",havingValue="true")
public class ContasDemonstracaoInicial implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    public ContasDemonstracaoInicial(JdbcTemplate jdbc,PasswordEncoder encoder) { this.jdbc=jdbc;this.encoder=encoder; }
    @Override @Transactional public void run(ApplicationArguments args) {
        int criada=jdbc.update("INSERT INTO raiz_migracoes (versao) VALUES ('contas-demonstracao-login-v1') ON CONFLICT DO NOTHING");
        if(criada==0)return;
        // Escolhidas pelos dados do P5. O marcador evita redefinir senhas em reinicializações.
        for(String id:List.of("C01-E0354","C12-E0237","C02-E0002","C03-E0004")) {
            int alterada=jdbc.update("UPDATE employees SET senha=?,token=NULL,token_expira_em=NULL WHERE employee_id=? AND active=TRUE AND lower(posicao)<>'administrador'",encoder.encode("123456"),id);
            if(alterada!=1)throw new IllegalStateException("Conta de demonstração não disponível: "+id);
        }
    }
}
