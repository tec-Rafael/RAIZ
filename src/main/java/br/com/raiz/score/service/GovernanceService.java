package br.com.raiz.score.service;

import br.com.raiz.ia.domain.UtilizacaoIA;
import br.com.raiz.ia.repository.UtilizacaoIARepository;
import br.com.raiz.score.domain.ApprovedAiTool;
import br.com.raiz.score.domain.Periodo;
import br.com.raiz.score.repository.ApprovedAiToolRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GovernanceService {

    private final UtilizacaoIARepository utilizacaoIARepository;
    private final ApprovedAiToolRepository approvedAiToolRepository;

    public GovernanceService(
            UtilizacaoIARepository utilizacaoIARepository,
            ApprovedAiToolRepository approvedAiToolRepository) {

        this.utilizacaoIARepository = utilizacaoIARepository;
        this.approvedAiToolRepository = approvedAiToolRepository;
    }

    public Double calcularGovernanceScore(String employeeId, Periodo periodo) {

        List<UtilizacaoIA> utilizacoes =
                utilizacaoIARepository.findByEmployeeId(employeeId);

        List<UtilizacaoIA> utilizacoesNoPeriodo = utilizacoes.stream()
                .filter(utilizacao -> utilizacao.getTimestamp() != null)
                .filter(utilizacao ->
                        !utilizacao.getTimestamp()
                                .toLocalDate()
                                .isBefore(periodo.getInicio()))
                .filter(utilizacao ->
                        !utilizacao.getTimestamp()
                                .toLocalDate()
                                .isAfter(periodo.getFim()))
                .toList();

        long requestsTotais = utilizacoesNoPeriodo.stream()
                .mapToLong(utilizacao ->
                        utilizacao.getRequestCount() == null
                                ? 0L
                                : utilizacao.getRequestCount())
                .sum();

        if (requestsTotais == 0) {
            return null;
        }

        List<String> companyIds = utilizacoesNoPeriodo.stream()
                .map(UtilizacaoIA::getCompanyId)
                .filter(companyId -> companyId != null)
                .distinct()
                .toList();

        Map<String, ApprovedAiTool> ferramentasAprovadas =
                companyIds.stream()
                        .flatMap(companyId ->
                                approvedAiToolRepository
                                        .findByIdCompanyId(companyId)
                                        .stream())
                        .collect(Collectors.toMap(
                                tool -> tool.getCompanyId() + "|" + tool.getAiToolId(),
                                Function.identity()
                        ));

        long requestsAprovados = utilizacoesNoPeriodo.stream()
                .filter(utilizacao -> {
                    String chave = utilizacao.getCompanyId()
                            + "|" + utilizacao.getAiToolId();

                    ApprovedAiTool ferramenta =
                            ferramentasAprovadas.get(chave);

                    return ferramenta != null
                            && ferramenta.getApprovalStatus() != null
                            && ferramenta.getApprovalStatus()
                            .equalsIgnoreCase("Aprovado");
                })
                .mapToLong(utilizacao ->
                        utilizacao.getRequestCount() == null
                                ? 0L
                                : utilizacao.getRequestCount())
                .sum();

        return (requestsAprovados / (double) requestsTotais) * 100;
    }
}