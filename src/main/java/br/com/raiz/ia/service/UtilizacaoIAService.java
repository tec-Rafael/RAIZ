package br.com.raiz.ia.service;

import br.com.raiz.ia.domain.UtilizacaoIA;
import br.com.raiz.ia.dto.UtilizacaoIADto.CriarRequest;
import br.com.raiz.ia.dto.UtilizacaoIADto.Response;
import br.com.raiz.ia.repository.UtilizacaoIARepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Regras de negócio do módulo de utilização de IA.
 *
 * Sem classes de exceção próprias: erros de negócio viram
 * ResponseStatusException com o status HTTP correspondente à situação real
 * (404/409), que o Spring já converte automaticamente na resposta.
 */
@Service
public class UtilizacaoIAService {

    private final UtilizacaoIARepository repository;

    public UtilizacaoIAService(UtilizacaoIARepository repository) {
        this.repository = repository;
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
     * Cria um novo registro de utilização de IA.
     *
     * Regra de negócio:
     *   1. usageId não pode já existir na base.
     *   2. os dados recebidos são utilizados para criar o registro.
     */
    @Transactional
    public Response criar(CriarRequest request) {

        if (repository.existsById(request.usageId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma utilização de IA cadastrada com este ID");
        }

        UtilizacaoIA nova = new UtilizacaoIA(
                request.usageId(),
                request.timestamp(),
                request.employeeId(),
                request.companyId(),
                request.departmentId(),
                request.aiToolId(),
                request.modelId(),
                request.application(),
                request.requestCount(),
                request.inputTokens(),
                request.outputTokens(),
                request.latencyMs(),
                request.requestStatus(),
                request.projectId(),
                request.costCenter()
        );

        try {
            return toResponse(repository.save(nova));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não foi possível cadastrar: conflito com um registro ou restrição já existente no banco", ex);
        }
    }

    /**
     * Remove um registro de utilização de IA existente.
     */
    @Transactional
    public void deletar(String id) {
        UtilizacaoIA utilizacao = obter(id);

        try {
            repository.delete(utilizacao);
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível excluir: existem registros vinculados a esta utilização de IA", ex);
        }
    }

    private UtilizacaoIA obter(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Utilização de IA não encontrada: " + id));
    }

    private Response toResponse(UtilizacaoIA utilizacao) {
        return new Response(
                utilizacao.getUsageId(),
                utilizacao.getTimestamp(),
                utilizacao.getEmployeeId(),
                utilizacao.getCompanyId(),
                utilizacao.getDepartmentId(),
                utilizacao.getAiToolId(),
                utilizacao.getModelId(),
                utilizacao.getApplication(),
                utilizacao.getRequestCount(),
                utilizacao.getInputTokens(),
                utilizacao.getOutputTokens(),
                utilizacao.getLatencyMs(),
                utilizacao.getRequestStatus(),
                utilizacao.getProjectId(),
                utilizacao.getCostCenter()
        );
    }
}