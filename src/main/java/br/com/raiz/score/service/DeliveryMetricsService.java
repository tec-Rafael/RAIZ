
package br.com.raiz.score.service;

import br.com.raiz.score.domain.Issue;
import br.com.raiz.score.domain.Periodo;
import br.com.raiz.score.domain.PullRequest;
import br.com.raiz.score.dto.DeliveryMetricsDto;
import br.com.raiz.score.dto.DeliveryScoreDto;
import br.com.raiz.score.repository.IssueRepository;
import br.com.raiz.score.repository.PullRequestRepository;
import br.com.raiz.usuario.domain.Funcionario;
import br.com.raiz.usuario.repository.FuncionarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeliveryMetricsService {

    private final PullRequestRepository pullRequestRepository;
    private final IssueRepository issueRepository;
    private final FuncionarioRepository funcionarioRepository;

    private static final List<String> CARGOS_ELEGIVEIS = List.of(
            "Desenvolvedor(a) Full Stack",
            "Desenvolvedor(a) Frontend",
            "Desenvolvedor(a) Backend",
            "Engenheiro(a) de Software",
            "Engenheiro(a) de Testes",
            "Analista de QA",
            "Engenheiro(a) DevOps",
            "Site Reliability Engineer",
            "Engenheiro(a) de Infraestrutura",
            "Engenheiro(a) de Dados",
            "Cientista de Dados"
    );

    // Percentis históricos fixos
    private static final double ISSUE_P10 = 9.2;
    private static final double ISSUE_P90 = 33.8;

    private static final double MERGE_P10 = 1.2708333333333335;
    private static final double MERGE_P90 = 2.770833333333333;

    private static final double REVIEW_P10 = 68.54483081695457;
    private static final double REVIEW_P90 = 88.55182535769397;

    private static final double PR_P10 = 4.0;
    private static final double PR_P90 = 20.0;

    public DeliveryMetricsService(
            PullRequestRepository pullRequestRepository,
            IssueRepository issueRepository,
            FuncionarioRepository funcionarioRepository
    ) {
        this.pullRequestRepository = pullRequestRepository;
        this.issueRepository = issueRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    public List<DeliveryMetricsDto> calcularMetricasPorCargo(
            Periodo periodo
    ) {
        validarPeriodo(periodo);

        List<Funcionario> funcionarios =
                funcionarioRepository.findByJobRoleIn(CARGOS_ELEGIVEIS);

        List<DeliveryMetricsDto> metricasBrutas = new ArrayList<>();

        // 1. Calcula as métricas brutas de cada funcionário.
        for (Funcionario funcionario : funcionarios) {
            String employeeId = funcionario.getEmployeeId();

            List<PullRequest> prs = pullRequestRepository
                    .findByEmployeeId(employeeId)
                    .stream()
                    .filter(pr -> estaNoPeriodo(pr.getCreatedAt(), periodo))
                    .toList();

            List<Issue> issues = issueRepository
                    .findByEmployeeId(employeeId)
                    .stream()
                    .filter(issue -> estaNoPeriodo(
                            issue.getCreatedAt(), periodo))
                    .toList();

            Double issueTime = calcularMedianaIssues(issues);
            Double mergeTime = calcularMedianaMerge(prs);

            int revisados = (int) prs.stream()
                    .filter(pr -> pr.getReviewCount() != null)
                    .filter(pr -> pr.getReviewCount() >= 1)
                    .count();

            Integer volume = prs.isEmpty() ? null : prs.size();

            Double reviewRate = prs.isEmpty()
                    ? null
                    : 100.0 * revisados / prs.size();

            metricasBrutas.add(criarDto(
                    funcionario.getEmployeeId(),
                    funcionario.getJobRole(),
                    issueTime,
                    mergeTime,
                    reviewRate,
                    volume,
                    prs.isEmpty() ? null : revisados,
                    null,
                    null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null
            ));
        }

        Map<String, List<DeliveryMetricsDto>> porCargo =
                metricasBrutas.stream()
                        .collect(Collectors.groupingBy(
                                DeliveryMetricsDto::getJobRole));

        // 2. Calcula PeerMean e taxa de revisão ajustada.
        List<DeliveryMetricsDto> ajustadas = new ArrayList<>();

        for (DeliveryMetricsDto f : metricasBrutas) {
            List<DeliveryMetricsDto> colegas = porCargo.get(f.getJobRole());

            OptionalDouble media = colegas.stream()
                    .filter(c -> !c.getEmployeeId().equals(f.getEmployeeId()))
                    .filter(c -> c.getReviewRate() != null)
                    .mapToDouble(DeliveryMetricsDto::getReviewRate)
                    .average();

            Double peerMean = media.isPresent() ? media.getAsDouble() : null;
            Double adjustedRate = null;

            if (peerMean != null
                    && f.getPrVolume() != null
                    && f.getReviewedPrCount() != null) {
                adjustedRate = (
                        100.0 * f.getReviewedPrCount() + 5.0 * peerMean
                ) / (f.getPrVolume() + 5.0);
            }

            ajustadas.add(criarDto(
                    f.getEmployeeId(),
                    f.getJobRole(),
                    f.getIssueResolutionTime(),
                    f.getMergeTime(),
                    f.getReviewRate(),
                    f.getPrVolume(),
                    f.getReviewedPrCount(),
                    peerMean,
                    adjustedRate,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null
            ));
        }

        Map<String, List<DeliveryMetricsDto>> grupos =
                ajustadas.stream()
                        .collect(Collectors.groupingBy(
                                DeliveryMetricsDto::getJobRole));

        // 3. Normaliza métricas e calcula notas relativas e consolidadas.
        List<DeliveryMetricsDto> resultados = new ArrayList<>();

        for (DeliveryMetricsDto f : ajustadas) {
            List<DeliveryMetricsDto> grupo = grupos.get(f.getJobRole());

            int totalIssue = valoresDoGrupo(
                    grupo, DeliveryMetricsDto::getIssueResolutionTime
            ).size();

            int totalMerge = valoresDoGrupo(
                    grupo, DeliveryMetricsDto::getMergeTime
            ).size();

            int totalReview = valoresDoGrupo(
                    grupo, DeliveryMetricsDto::getAdjustedReviewRate
            ).size();

            int totalPr = valoresDoGrupo(
                    grupo,
                    x -> x.getPrVolume() == null
                            ? null
                            : x.getPrVolume().doubleValue()
            ).size();

            Double indIssue = normalizar(
                    f.getIssueResolutionTime(), ISSUE_P10, ISSUE_P90, false);

            Double indMerge = normalizar(
                    f.getMergeTime(), MERGE_P10, MERGE_P90, false);

            // Os percentis históricos disponíveis foram calculados
            // usando a taxa de revisão bruta.
            Double indReview = normalizar(
                    f.getReviewRate(), REVIEW_P10, REVIEW_P90, true);

            Double indPr = f.getPrVolume() == null ? null
                    : normalizar(f.getPrVolume().doubleValue(),
                    PR_P10, PR_P90, true);

            Double relIssue = calcularNotaRelativa(
                    f.getIssueResolutionTime(),
                    valoresDoGrupo(grupo,
                            DeliveryMetricsDto::getIssueResolutionTime),
                    false);

            Double relMerge = calcularNotaRelativa(
                    f.getMergeTime(),
                    valoresDoGrupo(grupo,
                            DeliveryMetricsDto::getMergeTime),
                    false);

            Double relReview = calcularNotaRelativa(
                    f.getAdjustedReviewRate(),
                    valoresDoGrupo(grupo,
                            DeliveryMetricsDto::getAdjustedReviewRate),
                    true);

            Double relPr = calcularNotaRelativa(
                    f.getPrVolume() == null ? null
                            : f.getPrVolume().doubleValue(),
                    valoresDoGrupo(grupo, x -> x.getPrVolume() == null
                            ? null : x.getPrVolume().doubleValue()),
                    true);

            Double notaIndividual = calcularMediaPonderada(
                    indIssue, indMerge, indReview, indPr);

            Double notaRelativa = calcularMediaPonderada(
                    relIssue, relMerge, relReview, relPr);

            Double deliveryScore;

            if (notaIndividual != null && notaRelativa != null) {
                deliveryScore = 0.70 * notaIndividual
                        + 0.30 * notaRelativa;
            } else if (notaIndividual != null) {
                deliveryScore = notaIndividual;
            } else {
                // Se não houver nota individual, não inventa uma nota final.
                deliveryScore = notaRelativa;
            }

            resultados.add(new DeliveryMetricsDto(
                    f.getEmployeeId(),
                    f.getJobRole(),
                    f.getIssueResolutionTime(),
                    f.getMergeTime(),
                    f.getReviewRate(),
                    f.getPrVolume(),
                    f.getReviewedPrCount(),
                    f.getPeerMean(),
                    f.getAdjustedReviewRate(),

                    relIssue,
                    relMerge,
                    relReview,
                    relPr,

                    totalIssue,
                    totalMerge,
                    totalReview,
                    totalPr,

                    indIssue,
                    indMerge,
                    indReview,
                    indPr,

                    arredondar(notaIndividual),
                    arredondar(notaRelativa),
                    arredondar(deliveryScore)
            ));
        }

        return resultados;
    }

    private DeliveryMetricsDto criarDto(
            String employeeId,
            String jobRole,
            Double issueTime,
            Double mergeTime,
            Double reviewRate,
            Integer prVolume,
            Integer reviewedPrCount,
            Double peerMean,
            Double adjustedReviewRate,
            Double relIssue,
            Double relMerge,
            Double relReview,
            Double relPr,
            Double indIssue,
            Double indMerge,
            Double indReview,
            Double indPr,
            Double individualScore,
            Double relativeScore,
            Double deliveryScore
    ) {
        return new DeliveryMetricsDto(
                employeeId, jobRole,
                issueTime, mergeTime, reviewRate,
                prVolume, reviewedPrCount,
                peerMean, adjustedReviewRate,
                relIssue, relMerge, relReview, relPr,
                indIssue, indMerge, indReview, indPr,
                individualScore, relativeScore, deliveryScore
        );
    }


    public DeliveryScoreDto calcularDeliveryScoreFuncionario(
            String employeeId,
            Periodo periodo
    ) {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException(
                    "O ID do funcionário é obrigatório."
            );
        }

        List<DeliveryMetricsDto> resultados =
                calcularMetricasPorCargo(periodo);

        DeliveryMetricsDto funcionario = resultados.stream()
                .filter(f -> employeeId.equals(f.getEmployeeId()))
                .findFirst()
                .orElseThrow(() -> new java.util.NoSuchElementException(
                        "Funcionário não encontrado ou não elegível para o Delivery Score."
                ));

        DeliveryScoreDto.ScoreIndividualDto individual =
                new DeliveryScoreDto.ScoreIndividualDto(
                        funcionario.getIndividualIssueScore(),
                        funcionario.getIndividualMergeScore(),
                        funcionario.getIndividualReviewScore(),
                        funcionario.getIndividualPrScore(),
                        funcionario.getIndividualScore()
                );

        DeliveryScoreDto.ScoreRelativoDto relativo =
                new DeliveryScoreDto.ScoreRelativoDto(
                        new DeliveryScoreDto.IndicadorRelativoDto(
                                funcionario.getRelativeIssueScore(),
                                funcionario.getTotalFuncionariosIssue()
                        ),
                        new DeliveryScoreDto.IndicadorRelativoDto(
                                funcionario.getRelativeMergeScore(),
                                funcionario.getTotalFuncionariosMerge()
                        ),
                        new DeliveryScoreDto.IndicadorRelativoDto(
                                funcionario.getRelativeReviewScore(),
                                funcionario.getTotalFuncionariosReview()
                        ),
                        new DeliveryScoreDto.IndicadorRelativoDto(
                                funcionario.getRelativePrScore(),
                                funcionario.getTotalFuncionariosPr()
                        ),
                        funcionario.getRelativeScore()
                );

        return new DeliveryScoreDto(
                funcionario.getEmployeeId(),
                funcionario.getJobRole(),
                periodo.getInicio(),
                periodo.getFim(),
                individual,
                relativo,
                funcionario.getDeliveryScore()
        );
    }


    private Double normalizar(
            Double valor,
            double p10,
            double p90,
            boolean maiorMelhor
    ) {
        if (valor == null || !Double.isFinite(valor)) {
            return null;
        }

        if (p90 <= p10) {
            return null;
        }

        double nota = maiorMelhor
                ? 100.0 * (valor - p10) / (p90 - p10)
                : 100.0 * (p90 - valor) / (p90 - p10);

        return arredondar(Math.max(0.0, Math.min(100.0, nota)));
    }

    private Double calcularMediaPonderada(
            Double issue,
            Double merge,
            Double review,
            Double pr
    ) {
        double soma = 0.0;
        double pesos = 0.0;

        if (issue != null) {
            soma += issue * 0.35;
            pesos += 0.35;
        }
        if (merge != null) {
            soma += merge * 0.25;
            pesos += 0.25;
        }
        if (review != null) {
            soma += review * 0.20;
            pesos += 0.20;
        }
        if (pr != null) {
            soma += pr * 0.20;
            pesos += 0.20;
        }

        return pesos == 0.0 ? null : soma / pesos;
    }

    private Double calcularNotaRelativa(
            Double valorFuncionario,
            List<Double> valoresGrupo,
            boolean maiorMelhor
    ) {
        if (valorFuncionario == null || valoresGrupo == null) {
            return null;
        }

        List<Double> validos = valoresGrupo.stream()
                .filter(v -> v != null && Double.isFinite(v))
                .toList();

        // Funcionário + pelo menos três colegas com dados válidos.
        if (validos.size() < 4) {
            return null;
        }

        long piores = validos.stream()
                .filter(v -> maiorMelhor
                        ? v < valorFuncionario
                        : v > valorFuncionario)
                .count();

        long empatados = validos.stream()
                .filter(v -> Double.compare(v, valorFuncionario) == 0)
                .count();

        double nota = 100.0 * (piores + 0.5 * empatados) / validos.size();
        return arredondar(nota);
    }

    private List<Double> valoresDoGrupo(
            List<DeliveryMetricsDto> grupo,
            Function<DeliveryMetricsDto, Double> extrator
    ) {
        return grupo.stream()
                .map(extrator)
                .filter(v -> v != null && Double.isFinite(v))
                .toList();
    }

    private Double calcularMedianaIssues(List<Issue> issues) {
        List<Double> duracoes = new ArrayList<>();

        for (Issue issue : issues) {
            LocalDateTime inicio = issue.getCreatedAt();
            LocalDateTime fim = issue.getClosedAt();

            if (inicio != null && fim != null && !fim.isBefore(inicio)) {
                duracoes.add(
                        ChronoUnit.SECONDS.between(inicio, fim) / 86400.0);
            }
        }

        return calcularMediana(duracoes);
    }

    private Double calcularMedianaMerge(List<PullRequest> prs) {
        List<Double> duracoes = new ArrayList<>();

        for (PullRequest pr : prs) {
            LocalDateTime inicio = pr.getCreatedAt();
            LocalDateTime fim = pr.getMergedAt();

            if (inicio != null && fim != null && !fim.isBefore(inicio)) {
                duracoes.add(
                        ChronoUnit.SECONDS.between(inicio, fim) / 86400.0);
            }
        }

        return calcularMediana(duracoes);
    }

    private Double calcularMediana(List<Double> valores) {
        if (valores.isEmpty()) {
            return null;
        }

        valores.sort(Comparator.naturalOrder());
        int meio = valores.size() / 2;

        if (valores.size() % 2 == 0) {
            return (valores.get(meio - 1) + valores.get(meio)) / 2.0;
        }

        return valores.get(meio);
    }

    private Double arredondar(Double valor) {
        if (valor == null) {
            return null;
        }
        return Math.round(valor * 100.0) / 100.0;
    }

    private boolean estaNoPeriodo(
            LocalDateTime data,
            Periodo periodo
    ) {
        return data != null
                && !data.toLocalDate().isBefore(periodo.getInicio())
                && !data.toLocalDate().isAfter(periodo.getFim());
    }

    private void validarPeriodo(Periodo periodo) {
        if (periodo == null
                || periodo.getInicio() == null
                || periodo.getFim() == null
                || periodo.getFim().isBefore(periodo.getInicio())) {
            throw new IllegalArgumentException(
                    "O período informado é inválido.");
        }
    }
}
