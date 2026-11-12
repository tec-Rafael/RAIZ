package br.com.raiz.avatar.config;

import br.com.raiz.avatar.repository.AvatarRepository;
import br.com.raiz.avatar.service.AvatarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dá um avatar sorteado e diferente dos outros a cada colaborador da base que ainda não tem avatar,
 * e o avatar fixo ao administrador ROOT-E0001. Idempotente: só age sobre quem está sem avatar, então
 * reiniciar a API não troca avatares sorteados nem escolhidos. Roda depois dos inicializadores de contas.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AvataresIniciais implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AvataresIniciais.class);
    private final AvatarRepository repo;
    private final AvatarService avatares;
    public AvataresIniciais(AvatarRepository repo, AvatarService avatares) { this.repo = repo; this.avatares = avatares; }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        var pendentes = repo.funcionariosSemAvatar();
        if (pendentes.isEmpty()) return;
        var emUso = avatares.chavesReservadas();
        int criados = 0;
        for (String id : pendentes) if (avatares.atribuirInicial(id, emUso)) criados++;
        log.info("Avatares iniciais atribuídos: {}", criados);
    }
}
