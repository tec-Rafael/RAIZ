package br.com.raiz.ia.service;

import br.com.raiz.ia.domain.UtilizacaoIA;
import br.com.raiz.ia.dto.UtilizacaoIADto.CriarRequest;
import br.com.raiz.ia.dto.UtilizacaoIADto.Destaques;
import br.com.raiz.ia.dto.UtilizacaoIADto.Response;
import br.com.raiz.ia.repository.UtilizacaoIARepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;

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
    public Slice<Response> listar(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::toResponse);
    }


    /**
     * Devolve só o que importa para o dashboard: as IAs mais/menos usadas e os
     * funcionários que mais/menos usam IA (top N de cada), mais quantos
     * funcionários ativos nunca usaram. Tudo agregado no banco.
     *
     * @param companyId opcional; vazio/nulo = todas as empresas
     * @param limite    quantos itens por ranking
     */
    @Transactional(readOnly = true)
    public Destaques destaques(String companyId, int limite) {
        String empresa = (companyId == null || companyId.isBlank()) ? null : companyId.trim();

        return new Destaques(
                repository.ferramentasMaisUsadas(empresa, limite),
                repository.ferramentasMenosUsadas(empresa, limite),
                repository.funcionariosQueMaisUsam(empresa, limite),
                repository.funcionariosQueMenosUsam(empresa, limite),
                repository.contarFuncionariosSemUso(empresa)
        );
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
                request.latencyMs() == null
                        ? null
                        : request.latencyMs().doubleValue(),
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