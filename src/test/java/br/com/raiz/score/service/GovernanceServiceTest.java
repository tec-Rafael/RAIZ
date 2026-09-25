package br.com.raiz.score.service;

import br.com.raiz.ia.domain.UtilizacaoIA;
import br.com.raiz.ia.repository.UtilizacaoIARepository;
import br.com.raiz.score.domain.ApprovedAiTool;
import br.com.raiz.score.domain.Periodo;
import br.com.raiz.score.repository.ApprovedAiToolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernanceServiceTest {

    @Mock
    private UtilizacaoIARepository utilizacaoIARepository;

    @Mock
    private ApprovedAiToolRepository approvedAiToolRepository;

    @InjectMocks
    private GovernanceService governanceService;

    private final Periodo periodo = new Periodo(
            LocalDate.of(2025, 8, 30),
            LocalDate.of(2025, 11, 27)
    );

    @Test
    void deveRetornar100QuandoTodasAsSolicitacoesForemAprovadas() {

        UtilizacaoIA usoT01 = new UtilizacaoIA(
                "U001",
                LocalDate.of(2025, 9, 10).atStartOfDay(),
                "C01-E0001",
                "C01",
                "C01-D01",
                "T01",
                "M01",
                "Chat",
                100,
                1000,
                500,
                100.0,
                "success",
                "P01",
                "CC01"
        );

        UtilizacaoIA usoT02 = new UtilizacaoIA(
                "U002",
                LocalDate.of(2025, 10, 10).atStartOfDay(),
                "C01-E0001",
                "C01",
                "C01-D01",
                "T02",
                "M02",
                "Chat",
                50,
                500,
                250,
                100.0,
                "success",
                "P01",
                "CC01"
        );

        ApprovedAiTool aprovadoT01 = new ApprovedAiTool(
                "C01",
                "T01",
                "Aprovado",
                LocalDate.of(2025, 9, 1),
                null
        );

        ApprovedAiTool aprovadoT02 = new ApprovedAiTool(
                "C01",
                "T02",
                "Aprovado",
                LocalDate.of(2025, 9, 1),
                null
        );

        when(utilizacaoIARepository.findByEmployeeId("C01-E0001"))
                .thenReturn(List.of(usoT01, usoT02));

        when(approvedAiToolRepository.findByIdCompanyId("C01"))
                .thenReturn(List.of(aprovadoT01, aprovadoT02));

        Double score = governanceService.calcularGovernanceScore(
                "C01-E0001",
                periodo
        );

        assertEquals(100.0, score);
    }

    @Test
    void deveCalcularScoreConsiderandoQuantidadeDeRequestsAprovados() {

        UtilizacaoIA usoAprovado = new UtilizacaoIA(
                "U003",
                LocalDate.of(2025, 9, 10).atStartOfDay(),
                "C01-E0001",
                "C01",
                "C01-D01",
                "T01",
                "M01",
                "Chat",
                100,
                1000,
                500,
                100.0,
                "success",
                "P01",
                "CC01"
        );

        UtilizacaoIA usoBloqueado = new UtilizacaoIA(
                "U004",
                LocalDate.of(2025, 10, 10).atStartOfDay(),
                "C01-E0001",
                "C01",
                "C01-D01",
                "T02",
                "M02",
                "Chat",
                50,
                500,
                250,
                100.0,
                "success",
                "P01",
                "CC01"
        );

        ApprovedAiTool aprovado = new ApprovedAiTool(
                "C01",
                "T01",
                "Aprovado",
                LocalDate.of(2025, 9, 1),
                null
        );

        ApprovedAiTool bloqueado = new ApprovedAiTool(
                "C01",
                "T02",
                "Bloqueado",
                LocalDate.of(2025, 9, 1),
                null
        );

        when(utilizacaoIARepository.findByEmployeeId("C01-E0001"))
                .thenReturn(List.of(usoAprovado, usoBloqueado));

        when(approvedAiToolRepository.findByIdCompanyId("C01"))
                .thenReturn(List.of(aprovado, bloqueado));

        Double score = governanceService.calcularGovernanceScore(
                "C01-E0001",
                periodo
        );

        assertEquals(66.6666666667, score, 0.0001);
    }

    @Test
    void deveConsiderarEmAvaliacaoComoNaoAprovado() {

        UtilizacaoIA usoAprovado = new UtilizacaoIA(
                "U005",
                LocalDate.of(2025, 9, 10).atStartOfDay(),
                "C01-E0001",
                "C01",
                "C01-D01",
                "T01",
                "M01",
                "Chat",
                80,
                1000,
                500,
                100.0,
                "success",
                "P01",
                "CC01"
        );

        UtilizacaoIA usoEmAvaliacao = new UtilizacaoIA(
                "U006",
                LocalDate.of(2025, 10, 10).atStartOfDay(),
                "C01-E0001",
                "C01",
                "C01-D01",
                "T02",
                "M02",
                "Chat",
                20,
                500,
                250,
                100.0,
                "success",
                "P01",
                "CC01"
        );

        ApprovedAiTool aprovado = new ApprovedAiTool(
                "C01",
                "T01",
                "Aprovado",
                LocalDate.of(2025, 9, 1),
                null
        );

        ApprovedAiTool emAvaliacao = new ApprovedAiTool(
                "C01",
                "T02",
                "Em Avaliacao",
                LocalDate.of(2025, 9, 1),
                null
        );

        when(utilizacaoIARepository.findByEmployeeId("C01-E0001"))
                .thenReturn(List.of(usoAprovado, usoEmAvaliacao));

        when(approvedAiToolRepository.findByIdCompanyId("C01"))
                .thenReturn(List.of(aprovado, emAvaliacao));

        Double score = governanceService.calcularGovernanceScore(
                "C01-E0001",
                periodo
        );

        assertEquals(80.0, score);
    }

    @Test
    void deveRetornarNullQuandoNaoHouverSolicitacoesNoPeriodo() {

        UtilizacaoIA usoForaDoPeriodo = new UtilizacaoIA(
                "U007",
                LocalDate.of(2025, 12, 10).atStartOfDay(),
                "C01-E0001",
                "C01",
                "C01-D01",
                "T01",
                "M01",
                "Chat",
                100,
                1000,
                500,
                100.0,
                "success",
                "P01",
                "CC01"
        );

        when(utilizacaoIARepository.findByEmployeeId("C01-E0001"))
                .thenReturn(List.of(usoForaDoPeriodo));

        Double score = governanceService.calcularGovernanceScore(
                "C01-E0001",
                periodo
        );

        assertNull(score);
    }

    @Test
    void deveIgnorarSolicitacoesForaDoPeriodo() {

        UtilizacaoIA usoAprovadoDentro = new UtilizacaoIA(
                "U008",
                LocalDate.of(2025, 9, 10).atStartOfDay(),
                "C01-E0001",
                "C01",
                "C01-D01",
                "T01",
                "M01",
                "Chat",
                100,
                1000,
                500,
                100.0,
                "success",
                "P01",
                "CC01"
        );

        UtilizacaoIA usoBloqueadoFora = new UtilizacaoIA(
                "U009",
                LocalDate.of(2025, 12, 10).atStartOfDay(),
                "C01-E0001",
                "C01",
                "C01-D01",
                "T02",
                "M02",
                "Chat",
                100,
                500,
                250,
                100.0,
                "success",
                "P01",
                "CC01"
        );

        ApprovedAiTool aprovado = new ApprovedAiTool(
                "C01",
                "T01",
                "Aprovado",
                LocalDate.of(2025, 9, 1),
                null
        );

        ApprovedAiTool bloqueado = new ApprovedAiTool(
                "C01",
                "T02",
                "Bloqueado",
                LocalDate.of(2025, 12, 1),
                null
        );

        when(utilizacaoIARepository.findByEmployeeId("C01-E0001"))
                .thenReturn(List.of(usoAprovadoDentro, usoBloqueadoFora));

        when(approvedAiToolRepository.findByIdCompanyId("C01"))
                .thenReturn(List.of(aprovado, bloqueado));

        Double score = governanceService.calcularGovernanceScore(
                "C01-E0001",
                periodo
        );

        assertEquals(100.0, score);
    }

    @Test
    void deveConsiderarStatusAprovadoMesmoComMaiusculasDiferentes() {

        UtilizacaoIA usoAprovado = new UtilizacaoIA(
                "U010",
                LocalDate.of(2025, 9, 10).atStartOfDay(),
                "C01-E0001",
                "C01",
                "C01-D01",
                "T01",
                "M01",
                "Chat",
                50,
                1000,
                500,
                100.0,
                "success",
                "P01",
                "CC01"
        );

        UtilizacaoIA usoBloqueado = new UtilizacaoIA(
                "U011",
                LocalDate.of(2025, 10, 10).atStartOfDay(),
                "C01-E0001",
                "C01",
                "C01-D01",
                "T02",
                "M02",
                "Chat",
                50,
                500,
                250,
                100.0,
                "success",
                "P01",
                "CC01"
        );

        ApprovedAiTool aprovado = new ApprovedAiTool(
                "C01",
                "T01",
                "APROVADO",
                LocalDate.of(2025, 9, 1),
                null
        );

        ApprovedAiTool bloqueado = new ApprovedAiTool(
                "C01",
                "T02",
                "Bloqueado",
                LocalDate.of(2025, 9, 1),
                null
        );

        when(utilizacaoIARepository.findByEmployeeId("C01-E0001"))
                .thenReturn(List.of(usoAprovado, usoBloqueado));

        when(approvedAiToolRepository.findByIdCompanyId("C01"))
                .thenReturn(List.of(aprovado, bloqueado));

        Double score = governanceService.calcularGovernanceScore(
                "C01-E0001",
                periodo
        );

        assertEquals(50.0, score);
    }

    @Test
    void deveConsiderarComoNaoAprovadaFerramentaSemRegistroDeAprovacao() {

        UtilizacaoIA usoAprovado = new UtilizacaoIA(
                "U012",
                LocalDate.of(2025, 9, 10).atStartOfDay(),
                "C01-E0001",
                "C01",
                "C01-D01",
                "T01",
                "M01",
                "Chat",
                70,
                1000,
                500,
                100.0,
                "success",
                "P01",
                "CC01"
        );

        UtilizacaoIA usoSemRegistro = new UtilizacaoIA(
                "U013",
                LocalDate.of(2025, 10, 10).atStartOfDay(),
                "C01-E0001",
                "C01",
                "C01-D01",
                "T99",
                "M99",
                "Chat",
                30,
                500,
                250,
                100.0,
                "success",
                "P01",
                "CC01"
        );

        ApprovedAiTool aprovado = new ApprovedAiTool(
                "C01",
                "T01",
                "Aprovado",
                LocalDate.of(2025, 9, 1),
                null
        );

        when(utilizacaoIARepository.findByEmployeeId("C01-E0001"))
                .thenReturn(List.of(usoAprovado, usoSemRegistro));

        when(approvedAiToolRepository.findByIdCompanyId("C01"))
                .thenReturn(List.of(aprovado));

        Double score = governanceService.calcularGovernanceScore(
                "C01-E0001",
                periodo
        );

        assertEquals(70.0, score);
    }
}