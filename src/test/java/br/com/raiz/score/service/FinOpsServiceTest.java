package br.com.raiz.score.service;

import br.com.raiz.ia.domain.UtilizacaoIA;
import br.com.raiz.ia.repository.UtilizacaoIARepository;
import br.com.raiz.score.domain.AiModel;
import br.com.raiz.score.domain.Periodo;
import br.com.raiz.score.dto.FinOpsScoreDto;
import br.com.raiz.score.repository.AiModelRepository;
import br.com.raiz.usuario.domain.Funcionario;
import br.com.raiz.usuario.repository.FuncionarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinOpsServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private UtilizacaoIARepository utilizacaoIARepository;

    @Mock
    private AiModelRepository aiModelRepository;

    private FinOpsService finOpsService;

    @BeforeEach
    void setUp() {
        finOpsService = new FinOpsService(
                funcionarioRepository,
                utilizacaoIARepository,
                aiModelRepository
        );
    }

    @Test
    void deveLancarErroQuandoFuncionarioNaoExistir() {

        String employeeId = "C01-E0025";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        when(funcionarioRepository.existsById(employeeId))
                .thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> finOpsService.calcularFinOpsScore(
                        employeeId,
                        periodo
                )
        );
    }

    @Test
    void deveRetornarNuloQuandoNaoHouverUtilizacaoNoPeriodo() {

        String employeeId = "C01-E0025";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        when(funcionarioRepository.existsById(employeeId))
                .thenReturn(true);

        when(utilizacaoIARepository.findByEmployeeId(employeeId))
                .thenReturn(List.of());

        FinOpsScoreDto resultado =
                finOpsService.calcularFinOpsScore(
                        employeeId,
                        periodo
                );

        assertNull(resultado.tokenEfficiency());
        assertNull(resultado.requestEfficiency());
        assertNull(resultado.finOpsScore());
    }

    @Test
    void deveRetornarNuloQuandoModeloNaoPossuirPreco() {

        String employeeId = "C01-E0025";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        UtilizacaoIA utilizacao = criarUtilizacao(
                "U001",
                employeeId,
                "M001",
                10,
                1000,
                500
        );

        AiModel modelo = criarModelo(
                "M001",
                0.01,
                null
        );

        when(funcionarioRepository.existsById(employeeId))
                .thenReturn(true);

        when(utilizacaoIARepository.findByEmployeeId(employeeId))
                .thenReturn(List.of(utilizacao));

        when(aiModelRepository.findById("M001"))
                .thenReturn(Optional.of(modelo));

        FinOpsScoreDto resultado =
                finOpsService.calcularFinOpsScore(
                        employeeId,
                        periodo
                );

        assertNull(resultado.tokenEfficiency());
        assertNull(resultado.requestEfficiency());
        assertNull(resultado.finOpsScore());
    }

    @Test
    void deveCalcularFinOpsScoreCom50PorCentoDeCadaMetrica() {

        String employeeId = "C01-E0025";
        String employeeReferencia = "C01-E0026";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        UtilizacaoIA utilizacaoFuncionario = criarUtilizacao(
                "U001",
                employeeId,
                "M001",
                10,
                1000,
                0
        );

        UtilizacaoIA utilizacaoReferencia = criarUtilizacao(
                "U002",
                employeeReferencia,
                "M002",
                20,
                1000,
                0
        );

        AiModel modeloFuncionario = criarModelo(
                "M001",
                0.02,
                0.02
        );

        AiModel modeloReferencia = criarModelo(
                "M002",
                0.04,
                0.04
        );

        Funcionario funcionario = criarFuncionario(employeeId);
        Funcionario funcionarioReferencia =
                criarFuncionario(employeeReferencia);

        when(funcionarioRepository.existsById(employeeId))
                .thenReturn(true);

        when(funcionarioRepository.findAll())
                .thenReturn(List.of(
                        funcionario,
                        funcionarioReferencia
                ));

        when(utilizacaoIARepository.findByEmployeeId(employeeId))
                .thenReturn(List.of(utilizacaoFuncionario));

        when(utilizacaoIARepository.findByEmployeeId(employeeReferencia))
                .thenReturn(List.of(utilizacaoReferencia));

        when(aiModelRepository.findById("M001"))
                .thenReturn(Optional.of(modeloFuncionario));

        when(aiModelRepository.findById("M002"))
                .thenReturn(Optional.of(modeloReferencia));

        FinOpsScoreDto resultado =
                finOpsService.calcularFinOpsScore(
                        employeeId,
                        periodo
                );

        assertEquals(
                100.0,
                resultado.tokenEfficiency()
        );

        assertEquals(
                100.0,
                resultado.requestEfficiency()
        );

        assertEquals(
                100.0,
                resultado.finOpsScore()
        );
    }

    @Test
    void deveCalcularTokenEfficiency() {

        String employeeId = "C01-E0025";
        String employeeReferencia = "C01-E0026";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        UtilizacaoIA utilizacaoFuncionario = criarUtilizacao(
                "U001",
                employeeId,
                "M001",
                10,
                1000,
                0
        );

        UtilizacaoIA utilizacaoReferencia = criarUtilizacao(
                "U002",
                employeeReferencia,
                "M002",
                10,
                1000,
                0
        );

        AiModel modeloFuncionario = criarModelo(
                "M001",
                0.04,
                0.04
        );

        AiModel modeloReferencia = criarModelo(
                "M002",
                0.02,
                0.02
        );

        when(funcionarioRepository.existsById(employeeId))
                .thenReturn(true);

        when(funcionarioRepository.findAll())
                .thenReturn(List.of(
                        criarFuncionario(employeeId),
                        criarFuncionario(employeeReferencia)
                ));

        when(utilizacaoIARepository.findByEmployeeId(employeeId))
                .thenReturn(List.of(utilizacaoFuncionario));

        when(utilizacaoIARepository.findByEmployeeId(employeeReferencia))
                .thenReturn(List.of(utilizacaoReferencia));

        when(aiModelRepository.findById("M001"))
                .thenReturn(Optional.of(modeloFuncionario));

        when(aiModelRepository.findById("M002"))
                .thenReturn(Optional.of(modeloReferencia));

        FinOpsScoreDto resultado =
                finOpsService.calcularFinOpsScore(
                        employeeId,
                        periodo
                );

        assertEquals(
                75.0,
                resultado.tokenEfficiency()
        );
    }

    @Test
    void deveCalcularRequestEfficiency() {

        String employeeId = "C01-E0025";
        String employeeReferencia = "C01-E0026";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        UtilizacaoIA utilizacaoFuncionario = criarUtilizacao(
                "U001",
                employeeId,
                "M001",
                10,
                1000,
                0
        );

        UtilizacaoIA utilizacaoReferencia = criarUtilizacao(
                "U002",
                employeeReferencia,
                "M002",
                10,
                1000,
                0
        );

        AiModel modeloFuncionario = criarModelo(
                "M001",
                0.04,
                0.04
        );

        AiModel modeloReferencia = criarModelo(
                "M002",
                0.02,
                0.02
        );

        when(funcionarioRepository.existsById(employeeId))
                .thenReturn(true);

        when(funcionarioRepository.findAll())
                .thenReturn(List.of(
                        criarFuncionario(employeeId),
                        criarFuncionario(employeeReferencia)
                ));

        when(utilizacaoIARepository.findByEmployeeId(employeeId))
                .thenReturn(List.of(utilizacaoFuncionario));

        when(utilizacaoIARepository.findByEmployeeId(employeeReferencia))
                .thenReturn(List.of(utilizacaoReferencia));

        when(aiModelRepository.findById("M001"))
                .thenReturn(Optional.of(modeloFuncionario));

        when(aiModelRepository.findById("M002"))
                .thenReturn(Optional.of(modeloReferencia));

        FinOpsScoreDto resultado =
                finOpsService.calcularFinOpsScore(
                        employeeId,
                        periodo
                );

        assertEquals(
                75.0,
                resultado.requestEfficiency()
        );
    }

    @Test
    void deveLimitarScoreEm100() {

        String employeeId = "C01-E0025";
        String employeeReferencia = "C01-E0026";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        UtilizacaoIA utilizacaoFuncionario = criarUtilizacao(
                "U001",
                employeeId,
                "M001",
                10,
                1000,
                0
        );

        UtilizacaoIA utilizacaoReferencia = criarUtilizacao(
                "U002",
                employeeReferencia,
                "M002",
                10,
                1000,
                0
        );

        AiModel modeloFuncionario = criarModelo(
                "M001",
                0.01,
                0.01
        );

        AiModel modeloReferencia = criarModelo(
                "M002",
                0.10,
                0.10
        );

        when(funcionarioRepository.existsById(employeeId))
                .thenReturn(true);

        when(funcionarioRepository.findAll())
                .thenReturn(List.of(
                        criarFuncionario(employeeId),
                        criarFuncionario(employeeReferencia)
                ));

        when(utilizacaoIARepository.findByEmployeeId(employeeId))
                .thenReturn(List.of(utilizacaoFuncionario));

        when(utilizacaoIARepository.findByEmployeeId(employeeReferencia))
                .thenReturn(List.of(utilizacaoReferencia));

        when(aiModelRepository.findById("M001"))
                .thenReturn(Optional.of(modeloFuncionario));

        when(aiModelRepository.findById("M002"))
                .thenReturn(Optional.of(modeloReferencia));

        FinOpsScoreDto resultado =
                finOpsService.calcularFinOpsScore(
                        employeeId,
                        periodo
                );

        assertEquals(
                100.0,
                resultado.tokenEfficiency()
        );

        assertEquals(
                100.0,
                resultado.requestEfficiency()
        );

        assertEquals(
                100.0,
                resultado.finOpsScore()
        );
    }

    private UtilizacaoIA criarUtilizacao(
            String usageId,
            String employeeId,
            String modelId,
            Integer requestCount,
            Integer inputTokens,
            Integer outputTokens) {

        return new UtilizacaoIA(
                usageId,
                LocalDateTime.of(2025, 7, 20, 10, 0),
                employeeId,
                "C01",
                "D01",
                "T01",
                modelId,
                "Aplicacao teste",
                requestCount,
                inputTokens,
                outputTokens,
                100.0,
                "SUCCESS",
                "P01",
                "CC01"
        );
    }

    private AiModel criarModelo(
            String modelId,
            Double inputPrice,
            Double outputPrice) {

        return new AiModel(
                modelId,
                "T01",
                "Modelo teste",
                "Familia teste",
                inputPrice,
                outputPrice
        );
    }

    private Funcionario criarFuncionario(String employeeId) {

        return new Funcionario(
                employeeId,
                "C01",
                "D01",
                "Desenvolvedor",
                "Junior",
                LocalDate.of(2025, 1, 1),
                "CC01",
                true,
                "Funcionario Teste",
                employeeId + "@teste.com",
                "123456",
                null
        );
    }
}