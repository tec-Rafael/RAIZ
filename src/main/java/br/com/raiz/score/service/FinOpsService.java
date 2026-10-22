package br.com.raiz.score.service;

import br.com.raiz.score.domain.AiModel;
import br.com.raiz.score.domain.Periodo;
import br.com.raiz.ia.domain.UtilizacaoIA;
import br.com.raiz.score.repository.AiModelRepository;
import br.com.raiz.usuario.repository.FuncionarioRepository;
import br.com.raiz.ia.repository.UtilizacaoIARepository;
import br.com.raiz.score.dto.FinOpsScoreDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
public class FinOpsService {

    private final FuncionarioRepository funcionarioRepository;
    private final UtilizacaoIARepository utilizacaoIARepository;
    private final AiModelRepository aiModelRepository;

    public FinOpsService(
            FuncionarioRepository funcionarioRepository,
            UtilizacaoIARepository utilizacaoIARepository,
            AiModelRepository aiModelRepository) {

        this.funcionarioRepository = funcionarioRepository;
        this.utilizacaoIARepository = utilizacaoIARepository;
        this.aiModelRepository = aiModelRepository;
    }

    public FinOpsScoreDto calcularFinOpsScore(
            String employeeId,
            Periodo periodo) {

        if (!funcionarioRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Funcionário não encontrado");
        }

        List<UtilizacaoIA> utilizacoes =
                buscarUtilizacoesNoPeriodo(employeeId, periodo);

        if (utilizacoes.isEmpty()) {
            return new FinOpsScoreDto(null, null, null);
        }

        // Se algum modelo não existir ou não possuir preço,
        // o FinOps do funcionário fica como N/A.
        Map<String, AiModel> modelosFuncionario =
                carregarModelos(utilizacoes);
        if (possuiPrecoInvalido(utilizacoes, modelosFuncionario)) {
            return new FinOpsScoreDto(null, null, null);
        }

        Double custoTokenFuncionario =
                calcularCustoPorMilTokens(
                        utilizacoes,
                        modelosFuncionario
                );

        Double custoRequestFuncionario =
                calcularCustoPorRequest(
                        utilizacoes,
                        modelosFuncionario
                );

        Double custoTokenReferencia =
                calcularMedianaCustoPorMilTokens(periodo);

        Double custoRequestReferencia =
                calcularMedianaCustoPorRequest(periodo);

        Double tokenEfficiency = null;
        Double requestEfficiency = null;

        if (custoTokenFuncionario != null
                && custoTokenReferencia != null
                && custoTokenFuncionario > 0) {

            tokenEfficiency = Math.min(
                    100.0,
                    (custoTokenReferencia / custoTokenFuncionario) * 100.0
            );
        }

        if (custoRequestFuncionario != null
                && custoRequestReferencia != null
                && custoRequestFuncionario > 0) {

            requestEfficiency = Math.min(
                    100.0,
                    (custoRequestReferencia / custoRequestFuncionario) * 100.0
            );
        }

        Double finOpsScore = null;

        if (tokenEfficiency == null && requestEfficiency == null) {
            finOpsScore = null;

        } else if (tokenEfficiency == null) {
            finOpsScore = requestEfficiency;

        } else if (requestEfficiency == null) {
            finOpsScore = tokenEfficiency;

        } else {
            finOpsScore =
                    (tokenEfficiency * 0.50)
                            + (requestEfficiency * 0.50);
        }

        return new FinOpsScoreDto(
                tokenEfficiency,
                requestEfficiency,
                finOpsScore
        );
    }

    private List<UtilizacaoIA> buscarUtilizacoesNoPeriodo(
            String employeeId,
            Periodo periodo) {

        return utilizacaoIARepository
                .findByEmployeeId(employeeId)
                .stream()
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
    }

    private Map<String, AiModel> carregarModelos(
            List<UtilizacaoIA> utilizacoes) {

        List<String> modelIds = utilizacoes.stream()
                .map(UtilizacaoIA::getModelId)
                .distinct()
                .toList();

        Map<String, AiModel> modelosPorId = new HashMap<>();
        for (String id : modelIds) {
            if (id != null) aiModelRepository.findById(id)
                    .ifPresent(modelo -> modelosPorId.put(id, modelo));
        }

        return modelosPorId;
    }

    private Double calcularCustoPorMilTokens(
            List<UtilizacaoIA> utilizacoes,
            Map<String, AiModel> modelos) {

        double custoTotal = 0.0;
        long tokensTotais = 0L;

        for (UtilizacaoIA utilizacao : utilizacoes) {

            AiModel modelo = modelos.get(utilizacao.getModelId());

            if (modelo == null) {
                continue;
            }

            long inputTokens = utilizacao.getInputTokens() == null
                    ? 0L
                    : utilizacao.getInputTokens();

            long outputTokens = utilizacao.getOutputTokens() == null
                    ? 0L
                    : utilizacao.getOutputTokens();

            double custo = calcularCustoRegistro(
                    inputTokens,
                    outputTokens,
                    modelo
            );

            custoTotal += custo;
            tokensTotais += inputTokens + outputTokens;
        }

        if (tokensTotais == 0) {
            return null;
        }

        return (custoTotal / tokensTotais) * 1000.0;
    }

    private Double calcularCustoPorRequest(
            List<UtilizacaoIA> utilizacoes,
            Map<String, AiModel> modelos) {

        double custoTotal = 0.0;
        long requestsTotais = 0L;

        for (UtilizacaoIA utilizacao : utilizacoes) {

            AiModel modelo = modelos.get(utilizacao.getModelId());

            if (modelo == null) {
                continue;
            }

            long inputTokens = utilizacao.getInputTokens() == null
                    ? 0L
                    : utilizacao.getInputTokens();

            long outputTokens = utilizacao.getOutputTokens() == null
                    ? 0L
                    : utilizacao.getOutputTokens();

            double custo = calcularCustoRegistro(
                    inputTokens,
                    outputTokens,
                    modelo
            );

            custoTotal += custo;

            requestsTotais += utilizacao.getRequestCount() == null
                    ? 0L
                    : utilizacao.getRequestCount();
        }

        if (requestsTotais == 0) {
            return null;
        }

        return custoTotal / requestsTotais;
    }

    private double calcularCustoRegistro(
            long inputTokens,
            long outputTokens,
            AiModel modelo) {

        double custoEntrada =
                (inputTokens / 1000.0)
                        * modelo.getInputPriceUsdPer1kTokens();

        double custoSaida =
                (outputTokens / 1000.0)
                        * modelo.getOutputPriceUsdPer1kTokens();

        return custoEntrada + custoSaida;
    }

    private Double calcularMedianaCustoPorMilTokens(
            Periodo periodo) {

        List<Double> valores = new ArrayList<>();

        funcionarioRepository.findAll()
                .forEach(funcionario -> {

                    List<UtilizacaoIA> utilizacoes =
                            buscarUtilizacoesNoPeriodo(
                                    funcionario.getEmployeeId(),
                                    periodo
                            );

                    if (utilizacoes.isEmpty()) {
                        return;
                    }

                    Map<String, AiModel> modelos =
                            carregarModelos(utilizacoes);

                    boolean possuiPrecoInvalido =
                            possuiPrecoInvalido(
                                    utilizacoes,
                                    modelos
                            );

                    if (possuiPrecoInvalido) {
                        return;
                    }

                    Double custo =
                            calcularCustoPorMilTokens(
                                    utilizacoes,
                                    modelos
                            );

                    if (custo != null) {
                        valores.add(custo);
                    }
                });

        return calcularMediana(valores);
    }

    private Double calcularMedianaCustoPorRequest(
            Periodo periodo) {

        List<Double> valores = new ArrayList<>();

        funcionarioRepository.findAll()
                .forEach(funcionario -> {

                    List<UtilizacaoIA> utilizacoes =
                            buscarUtilizacoesNoPeriodo(
                                    funcionario.getEmployeeId(),
                                    periodo
                            );

                    if (utilizacoes.isEmpty()) {
                        return;
                    }

                    Map<String, AiModel> modelos =
                            carregarModelos(utilizacoes);

                    boolean possuiPrecoInvalido =
                            possuiPrecoInvalido(
                                    utilizacoes,
                                    modelos
                            );

                    if (possuiPrecoInvalido) {
                        return;
                    }

                    Double custo =
                            calcularCustoPorRequest(
                                    utilizacoes,
                                    modelos
                            );

                    if (custo != null) {
                        valores.add(custo);
                    }
                });

        return calcularMediana(valores);
    }

    private boolean possuiPrecoInvalido(
            List<UtilizacaoIA> utilizacoes,
            Map<String, AiModel> modelos) {

        for (UtilizacaoIA utilizacao : utilizacoes) {

            AiModel modelo =
                    modelos.get(utilizacao.getModelId());

            if (modelo == null
                    || modelo.getInputPriceUsdPer1kTokens() == null
                    || modelo.getOutputPriceUsdPer1kTokens() == null) {

                return true;
            }
        }

        return false;
    }

    private Double calcularMediana(List<Double> valores) {

        if (valores.isEmpty()) {
            return null;
        }

        Collections.sort(valores);

        int tamanho = valores.size();

        if (tamanho % 2 == 1) {
            return valores.get(tamanho / 2);
        }

        double valor1 = valores.get((tamanho / 2) - 1);
        double valor2 = valores.get(tamanho / 2);

        return (valor1 + valor2) / 2.0;
    }
}
