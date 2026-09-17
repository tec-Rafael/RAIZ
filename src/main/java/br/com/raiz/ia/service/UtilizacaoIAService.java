package br.com.raiz.ia.service;

import br.com.raiz.ia.domain.UtilizacaoIA;
import br.com.raiz.ia.dto.UtilizacaoIADto.CriarRequest;
import br.com.raiz.ia.dto.UtilizacaoIADto.Response;
import br.com.raiz.ia.repository.UtilizacaoIARepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
public class UtilizacaoIAService {

    private final UtilizacaoIARepository repository;

    public UtilizacaoIAService(UtilizacaoIARepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Response> listar() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Response buscarPorId(String id) {
        return toResponse(obter(id));
    }

    @Transactional
    public Response criar(CriarRequest request) {

        if (repository.existsById(request.usageId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe uma utilização de IA com este ID"
            );
        }

        UtilizacaoIA utilizacao = new UtilizacaoIA(
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

        return toResponse(repository.save(utilizacao));
    }

    private UtilizacaoIA obter(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Utilização de IA não encontrada: " + id
                ));
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