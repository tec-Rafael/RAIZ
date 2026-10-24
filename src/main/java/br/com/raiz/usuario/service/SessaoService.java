package br.com.raiz.usuario.service;

import br.com.raiz.usuario.domain.Funcionario;
import br.com.raiz.usuario.domain.Posicao;
import br.com.raiz.usuario.dto.FuncionarioDto.LoginRequest;
import br.com.raiz.usuario.repository.FuncionarioRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

@Service
public class SessaoService {
    public static final Duration DURACAO = Duration.ofHours(8);
    private final FuncionarioRepository repository;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();
    private final String senhaInexistente;

    public SessaoService(FuncionarioRepository repository, PasswordEncoder encoder, JdbcTemplate jdbc) {
        this.repository = repository; this.encoder = encoder; this.jdbc = jdbc;
        this.senhaInexistente = encoder.encode("conta-inexistente");
    }
    public record Usuario(String employeeId, String nome, String email, Posicao posicao,
                          String companyId, String departmentId, String jobRole, Instant expiraEm) {
        public String perfil() { return posicao.name().toLowerCase(Locale.ROOT); }
        public boolean admin() { return posicao == Posicao.ADMINISTRADOR; }
        public boolean gestor() { return posicao == Posicao.GESTOR; }
    }
    public record Login(Usuario usuario, String cookie) { }

    @Transactional
    public Login login(LoginRequest credenciais) {
        var f = repository.findByEmailIgnoreCase(credenciais.email().trim().toLowerCase(Locale.ROOT)).orElse(null);
        boolean correta = encoder.matches(credenciais.senha(), f == null ? senhaInexistente : f.getSenha());
        if (f == null || !correta) throw naoAutorizado();
        if (!Boolean.TRUE.equals(f.getActive())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Funcionário inativo");
        byte[] bytes = new byte[64]; random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        Instant expira = Instant.now().plus(DURACAO);
        jdbc.update("UPDATE employees SET token = ?, token_expira_em = ? WHERE employee_id = ?",
            token, Timestamp.from(expira), f.getEmployeeId());
        return new Login(usuario(f, expira), token + f.getEmployeeId());
    }
    public Usuario validar(String cookie) {
        if (cookie == null || !cookie.matches("[a-f0-9]{128}[A-Za-z0-9_-]{1,64}")) throw naoAutorizado();
        String token = cookie.substring(0,128), id = cookie.substring(128);
        var sessoes = jdbc.query("SELECT token, token_expira_em FROM employees WHERE employee_id = ?",
            (rs, n) -> new Token(rs.getString(1), rs.getTimestamp(2) == null ? null : rs.getTimestamp(2).toInstant()), id);
        if (sessoes.isEmpty()) throw naoAutorizado();
        var sessao = sessoes.get(0);
        if (sessao.token() == null || sessao.expira() == null || !sessao.expira().isAfter(Instant.now()) ||
            !MessageDigest.isEqual(token.getBytes(StandardCharsets.US_ASCII), sessao.token().getBytes(StandardCharsets.US_ASCII))) throw naoAutorizado();
        var f = repository.findById(id).orElseThrow(SessaoService::naoAutorizado);
        if (!Boolean.TRUE.equals(f.getActive()) || f.getPosicao() == null) throw naoAutorizado();
        return usuario(f, sessao.expira());
    }
    private record Token(String token, Instant expira) { }
    @Transactional
    public void logout(String cookie) {
        if (cookie != null && cookie.matches("[a-f0-9]{128}[A-Za-z0-9_-]{1,64}"))
            jdbc.update("UPDATE employees SET token = NULL, token_expira_em = NULL WHERE employee_id = ? AND token = ?",
                cookie.substring(128), cookie.substring(0,128));
    }
    public Usuario atual() {
        var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null || !(attributes.getRequest().getAttribute("usuarioAutenticado") instanceof Usuario u)) throw naoAutorizado();
        return u;
    }
    public boolean permite(Funcionario f) {
        var u = atual();
        return u.admin() || u.employeeId().equals(f.getEmployeeId()) || (u.gestor() && u.departmentId() != null &&
            Objects.equals(u.companyId(),f.getCompanyId()) && Objects.equals(u.departmentId(),f.getDepartmentId()));
    }
    public void exigirPessoa(String id) {
        var f = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Funcionário não encontrado"));
        if (!permite(f)) throw proibido();
    }
    public void exigirGestao() { if (!atual().admin() && !atual().gestor()) throw proibido(); }
    public void exigirAdmin() { if (!atual().admin()) throw proibido(); }
    public static String cookie(HttpServletRequest request) {
        if (request.getCookies() != null) for (Cookie c : request.getCookies()) if (c.getName().equals("usuario")) return c.getValue();
        return null;
    }
    private static Usuario usuario(Funcionario f, Instant expira) {
        return new Usuario(f.getEmployeeId(),f.getNome(),f.getEmail(),f.getPosicao(),f.getCompanyId(),f.getDepartmentId(),f.getJobRole(),expira);
    }
    public static ResponseStatusException naoAutorizado() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sessão ausente, inválida ou expirada. Entre novamente."); }
    public static ResponseStatusException proibido() { return new ResponseStatusException(HttpStatus.FORBIDDEN,"Você não tem acesso a este recurso."); }
}
