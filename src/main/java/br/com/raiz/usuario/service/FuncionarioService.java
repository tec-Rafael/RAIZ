package br.com.raiz.usuario.service;

import br.com.raiz.usuario.domain.Funcionario;
import br.com.raiz.usuario.domain.Posicao;
import br.com.raiz.usuario.dto.FuncionarioDto.CriarRequest;
import br.com.raiz.usuario.dto.FuncionarioDto.LoginRequest;
import br.com.raiz.usuario.dto.FuncionarioDto.LoginResponse;
import br.com.raiz.usuario.dto.FuncionarioDto.Response;
import br.com.raiz.usuario.repository.FuncionarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

/**
 * Regras de negócio do módulo de funcionários.
 *
 * Sem classes de exceção próprias: erros de negócio viram
 * ResponseStatusException com o status HTTP correspondente à situação real
 * (400/403/404/409), que o Spring já converte automaticamente na resposta.
 */
@Service
public class FuncionarioService {

    private final FuncionarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public FuncionarioService(FuncionarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Funcionario funcionario = repository.findByEmailIgnoreCase(normalizar(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos"));

        if (!passwordEncoder.matches(request.senha(), funcionario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
        }
        if (!Boolean.TRUE.equals(funcionario.getActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Funcionário inativo");
        }
        return new LoginResponse(funcionario.getEmployeeId(), funcionario.getNome(),
                funcionario.getEmail(), funcionario.getPosicao());
    }

    @Transactional(readOnly = true)
    public List<Response> listar() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Response buscarPorId(String id) {
        return toResponse(obter(id));
    }

    /**
     * Cria um novo funcionário.
     *
     * Regra de negócio (não confia no que o cliente envia, apenas no que
     * está de fato gravado no banco):
     *   1. gestorId precisa existir na base.
     *   2. o funcionário encontrado com esse id precisa ter posicao = GESTOR.
     *   3. o novo funcionário herda company_id e department_id do gestor —
     *      isso impede que um gestor cadastre alguém fora do próprio setor.
     *   4. e-mail não pode já existir.
     */
    @Transactional
    public Response criar(CriarRequest request) {
        Funcionario gestor = obter(request.gestorId());

        if (gestor.getPosicao() != Posicao.GESTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas um funcionário com posição GESTOR pode cadastrar novos funcionários");
        }

        String email = normalizar(request.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe um funcionário cadastrado com este e-mail");
        }

        String employeeId = gerarProximoEmployeeId(gestor.getCompanyId());
        Posicao posicao = request.posicao() != null ? request.posicao() : Posicao.COLABORADOR;

        Funcionario novo = new Funcionario(
                employeeId,
                gestor.getCompanyId(),
                gestor.getDepartmentId(),
                request.jobRole(),
                request.seniority(),
                request.hireDate(),
                request.costCenter(),
                Boolean.TRUE,
                request.nome(),
                email,
                passwordEncoder.encode(request.senha()),
                posicao
        );

        try {
            return toResponse(repository.save(novo));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não foi possível cadastrar: conflito com um registro ou restrição já existente no banco", ex);
        }
    }

    /**
     * Remove um funcionário existente. Como issues e pull_requests possuem
     * FOREIGN KEY para employees.employee_id, excluir alguém com histórico
     * vinculado gera violação de integridade no Postgres — tratada aqui como
     * 409, nunca como 500.
     */
    @Transactional
    public void deletar(String id) {
        Funcionario funcionario = obter(id);
        try {
            repository.delete(funcionario);
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível excluir: existem registros vinculados a este funcionário (issues, pull requests, etc.)", ex);
        }
    }

    private Funcionario obter(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado: " + id));
    }

    private String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Gera o próximo employee_id seguindo o padrão real já usado no banco
     * ({companyId}-E0001, {companyId}-E0002, ...), verificando unicidade
     * efetiva contra a base — nunca inventando um formato novo de ID.
     */
    private String gerarProximoEmployeeId(String companyId) {
        long proximo = repository.countByCompanyId(companyId) + 1;
        String id;
        do {
            id = "%s-E%04d".formatted(companyId, proximo++);
        } while (repository.existsById(id));
        return id;
    }

    private Response toResponse(Funcionario f) {
        return new Response(f.getEmployeeId(), f.getCompanyId(), f.getDepartmentId(), f.getJobRole(),
                f.getSeniority(), f.getHireDate(), f.getCostCenter(), f.getActive(), f.getNome(),
                f.getEmail(), f.getPosicao());
    }
}
