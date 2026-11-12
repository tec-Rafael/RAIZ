package br.com.raiz.avatar.service;

import br.com.raiz.avatar.dto.AvatarDto.Avatar;
import br.com.raiz.avatar.dto.AvatarDto.Resposta;
import br.com.raiz.avatar.repository.AvatarRepository;
import br.com.raiz.usuario.service.SessaoService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;

@Service
public class AvatarService {
    /** Tentativas de sorteio até achar uma combinação livre (na base atual bastam no máximo 2). */
    static final int TENTATIVAS = 50;
    private final AvatarRepository repo;
    private final SessaoService sessoes;
    public AvatarService(AvatarRepository repo, SessaoService sessoes) { this.repo = repo; this.sessoes = sessoes; }

    /** Avatar do usuário da sessão. Quem ainda não tem recebe agora o avatar sorteado (ou o fixo, no caso do administrador). */
    @Transactional
    public Resposta meu() {
        String id = sessoes.atual().employeeId();
        var atual = repo.buscar(id);
        if (atual.isEmpty()) { atribuirInicial(id, chavesReservadas()); atual = repo.buscar(id); }
        var r = atual.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível carregar o avatar."));
        return new Resposta(r.avatar(), r.origem());
    }

    /** O próprio usuário edita o avatar no perfil. */
    @Transactional
    public Resposta salvar(Avatar pedido) {
        String erro = AvatarCatalogo.validar(pedido);
        if (erro != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, erro);
        var a = new Avatar(2, pedido.genero(), pedido.pele(), pedido.olhos(), pedido.cabelo(), pedido.corCabelo(), pedido.facial(),
            pedido.sardas(), pedido.camisa(), pedido.calca());
        repo.salvar(sessoes.atual().employeeId(), a, "escolhido");
        return new Resposta(a, "escolhido");
    }

    /** Combinações que um sorteio não pode repetir: as já gravadas e a do administrador (mesmo antes de ele ter avatar). */
    public Set<String> chavesReservadas() {
        var emUso = repo.chavesEmUso();
        emUso.add(AvatarCatalogo.chave(AvatarCatalogo.AVATAR_ADMINISTRADOR));
        return emUso;
    }

    /**
     * Dá ao colaborador o avatar inicial: o fixo para o administrador; para os demais, o sorteio determinístico,
     * refeito com a próxima tentativa enquanto a combinação já estiver em uso. Atualiza o conjunto de chaves em uso.
     */
    public boolean atribuirInicial(String employeeId, Set<String> emUso) {
        if (AvatarCatalogo.ADMINISTRADOR.equals(employeeId)) {
            boolean criado = repo.inserirSeAusente(employeeId, AvatarCatalogo.AVATAR_ADMINISTRADOR, "fixo");
            emUso.add(AvatarCatalogo.chave(AvatarCatalogo.AVATAR_ADMINISTRADOR));
            return criado;
        }
        Avatar a = AvatarCatalogo.sortear(employeeId, 0);
        for (int t = 1; t < TENTATIVAS && emUso.contains(AvatarCatalogo.chave(a)); t++) a = AvatarCatalogo.sortear(employeeId, t);
        boolean criado = repo.inserirSeAusente(employeeId, a, "sorteado");
        if (criado) emUso.add(AvatarCatalogo.chave(a));
        return criado;
    }
}
