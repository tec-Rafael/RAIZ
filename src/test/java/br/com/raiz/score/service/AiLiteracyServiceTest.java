package br.com.raiz.score.service;

import br.com.raiz.score.domain.Periodo;
import br.com.raiz.score.domain.TrainingRecord;
import br.com.raiz.score.repository.TrainingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiLiteracyServiceTest {

    @Mock
    private TrainingRecordRepository trainingRecordRepository;

    private AiLiteracyService aiLiteracyService;

    @BeforeEach
    void setUp() {
        aiLiteracyService = new AiLiteracyService(trainingRecordRepository);
    }

    @Test
    void deveCalcularCompletionScore() {

        String employeeId = "C01-E0025";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        TrainingRecord treinamento = new TrainingRecord(
                "T001",
                employeeId,
                "Curso de IA",
                "Fundamentos",
                LocalDate.of(2025, 7, 20),
                LocalDate.of(2025, 6, 10),
                "Concluido",
                90.0,
                10.0
        );

        when(trainingRecordRepository.findByEmployeeId(employeeId))
                .thenReturn(List.of(treinamento));

        Double resultado =
                aiLiteracyService.calcularCompletionScore(employeeId, periodo);

        assertEquals(100.0, resultado);
    }

    @Test
    void deveConsiderarCursosMatriculadosAindaNaoConcluidos() {

        String employeeId = "C01-E0025";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        TrainingRecord cursoConcluido = new TrainingRecord(
                "T001",
                employeeId,
                "Curso de IA",
                "Fundamentos",
                LocalDate.of(2025, 7, 20),
                LocalDate.of(2025, 6, 10),
                "Concluido",
                90.0,
                10.0
        );

        TrainingRecord cursoPendente = new TrainingRecord(
                "T002",
                employeeId,
                "Curso de Machine Learning",
                "Machine Learning",
                null,
                LocalDate.of(2025, 7, 10),
                "Em andamento",
                null,
                15.0
        );

        when(trainingRecordRepository.findByEmployeeId(employeeId))
                .thenReturn(List.of(cursoConcluido, cursoPendente));

        Double resultado =
                aiLiteracyService.calcularCompletionScore(employeeId, periodo);

        assertEquals(50.0, resultado);
    }

    @Test
    void deveTransferirCursoPendenteParaConcluidoNoPeriodoSeguinte() {

        String employeeId = "C01-E0025";

        Periodo periodo1 = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        Periodo periodo2 = new Periodo(
                LocalDate.of(2025, 8, 30),
                LocalDate.of(2025, 11, 27)
        );

        TrainingRecord cursoA = new TrainingRecord(
                "T001",
                employeeId,
                "Curso de IA",
                "Fundamentos",
                LocalDate.of(2025, 7, 20),
                LocalDate.of(2025, 6, 10),
                "Concluido",
                90.0,
                10.0
        );

        TrainingRecord cursoB = new TrainingRecord(
                "T002",
                employeeId,
                "Curso de Machine Learning",
                "Machine Learning",
                LocalDate.of(2025, 9, 15),
                LocalDate.of(2025, 7, 10),
                "Concluido",
                85.0,
                15.0
        );

        when(trainingRecordRepository.findByEmployeeId(employeeId))
                .thenReturn(List.of(cursoA, cursoB));

        Double resultadoPeriodo1 =
                aiLiteracyService.calcularCompletionScore(employeeId, periodo1);

        Double resultadoPeriodo2 =
                aiLiteracyService.calcularCompletionScore(employeeId, periodo2);

        assertEquals(50.0, resultadoPeriodo1);
        assertEquals(100.0, resultadoPeriodo2);
    }

    @Test
    void deveRetornarNuloQuandoNaoHouverCursosNoPeriodo() {

        String employeeId = "C01-E0025";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        when(trainingRecordRepository.findByEmployeeId(employeeId))
                .thenReturn(List.of());

        Double resultado =
                aiLiteracyService.calcularCompletionScore(employeeId, periodo);

        assertEquals(null, resultado);
    }

    @Test
    void naoDeveConsiderarCursoConcluidoAntesDoPeriodo() {

        String employeeId = "C01-E0025";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        TrainingRecord cursoAntigo = new TrainingRecord(
                "T001",
                employeeId,
                "Curso antigo",
                "Fundamentos",
                LocalDate.of(2025, 5, 20),
                LocalDate.of(2025, 5, 1),
                "Concluido",
                90.0,
                10.0
        );

        when(trainingRecordRepository.findByEmployeeId(employeeId))
                .thenReturn(List.of(cursoAntigo));

        Double resultado =
                aiLiteracyService.calcularCompletionScore(employeeId, periodo);

        assertEquals(null, resultado);
    }

    @Test
    void deveCalcularTrainingScorePelaMediaDasNotas() {

        String employeeId = "C01-E0025";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        TrainingRecord cursoA = new TrainingRecord(
                "T001",
                employeeId,
                "Curso de IA",
                "Fundamentos",
                LocalDate.of(2025, 7, 20),
                LocalDate.of(2025, 6, 10),
                "Concluido",
                80.0,
                10.0
        );

        TrainingRecord cursoB = new TrainingRecord(
                "T002",
                employeeId,
                "Curso de Machine Learning",
                "Machine Learning",
                LocalDate.of(2025, 8, 10),
                LocalDate.of(2025, 7, 1),
                "Concluido",
                100.0,
                15.0
        );

        when(trainingRecordRepository.findByEmployeeId(employeeId))
                .thenReturn(List.of(cursoA, cursoB));

        Double resultado =
                aiLiteracyService.calcularTrainingScore(employeeId, periodo);

        assertEquals(90.0, resultado);
    }

    @Test
    void deveIgnorarCursosConcluidosForaDoPeriodoNoTrainingScore() {

        String employeeId = "C01-E0025";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        TrainingRecord cursoDentro = new TrainingRecord(
                "T001",
                employeeId,
                "Curso de IA",
                "Fundamentos",
                LocalDate.of(2025, 7, 20),
                LocalDate.of(2025, 6, 10),
                "Concluido",
                80.0,
                10.0
        );

        TrainingRecord cursoFora = new TrainingRecord(
                "T002",
                employeeId,
                "Curso antigo",
                "Machine Learning",
                LocalDate.of(2025, 5, 20),
                LocalDate.of(2025, 5, 1),
                "Concluido",
                100.0,
                15.0
        );

        when(trainingRecordRepository.findByEmployeeId(employeeId))
                .thenReturn(List.of(cursoDentro, cursoFora));

        Double resultado =
                aiLiteracyService.calcularTrainingScore(employeeId, periodo);

        assertEquals(80.0, resultado);
    }

    @Test
    void deveRetornarNuloQuandoNaoHouverCursoConcluidoNoTrainingScore() {

        String employeeId = "C01-E0025";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        TrainingRecord cursoPendente = new TrainingRecord(
                "T001",
                employeeId,
                "Curso de IA",
                "Fundamentos",
                null,
                LocalDate.of(2025, 6, 10),
                "Em andamento",
                null,
                10.0
        );

        when(trainingRecordRepository.findByEmployeeId(employeeId))
                .thenReturn(List.of(cursoPendente));

        Double resultado =
                aiLiteracyService.calcularTrainingScore(employeeId, periodo);

        assertEquals(null, resultado);
    }

    @Test
    void deveCalcularDiversityScorePelaQuantidadeDeCategorias() {

        String employeeId = "C01-E0025";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        TrainingRecord cursoA = new TrainingRecord(
                "T001",
                employeeId,
                "Curso de IA",
                "Fundamentos",
                LocalDate.of(2025, 7, 20),
                LocalDate.of(2025, 6, 10),
                "Concluido",
                90.0,
                10.0
        );

        TrainingRecord cursoB = new TrainingRecord(
                "T002",
                employeeId,
                "Curso de Machine Learning",
                "Machine Learning",
                LocalDate.of(2025, 8, 10),
                LocalDate.of(2025, 7, 1),
                "Concluido",
                85.0,
                15.0
        );

        when(trainingRecordRepository.findByEmployeeId(employeeId))
                .thenReturn(List.of(cursoA, cursoB));

        Double resultado =
                aiLiteracyService.calcularDiversityScore(employeeId, periodo);

        assertEquals(50.0, resultado);
    }

    @Test
    void deveContarCategoriaApenasUmaVezNoDiversityScore() {

        String employeeId = "C01-E0025";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        TrainingRecord cursoA = new TrainingRecord(
                "T001",
                employeeId,
                "Curso de IA 1",
                "Fundamentos",
                LocalDate.of(2025, 7, 20),
                LocalDate.of(2025, 6, 10),
                "Concluido",
                90.0,
                10.0
        );

        TrainingRecord cursoB = new TrainingRecord(
                "T002",
                employeeId,
                "Curso de IA 2",
                "Fundamentos",
                LocalDate.of(2025, 8, 10),
                LocalDate.of(2025, 7, 1),
                "Concluido",
                85.0,
                15.0
        );

        when(trainingRecordRepository.findByEmployeeId(employeeId))
                .thenReturn(List.of(cursoA, cursoB));

        Double resultado =
                aiLiteracyService.calcularDiversityScore(employeeId, periodo);

        assertEquals(25.0, resultado);
    }

    @Test
    void deveCalcularAiLiteracyScore() {

        String employeeId = "C01-E0025";

        Periodo periodo = new Periodo(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 29)
        );

        TrainingRecord cursoA = new TrainingRecord(
                "T001",
                employeeId,
                "Curso de IA",
                "Fundamentos",
                LocalDate.of(2025, 7, 20),
                LocalDate.of(2025, 6, 10),
                "Concluido",
                80.0,
                10.0
        );

        TrainingRecord cursoB = new TrainingRecord(
                "T002",
                employeeId,
                "Curso de Machine Learning",
                "Machine Learning",
                null,
                LocalDate.of(2025, 7, 10),
                "Em andamento",
                null,
                15.0
        );

        when(trainingRecordRepository.findByEmployeeId(employeeId))
                .thenReturn(List.of(cursoA, cursoB));

        Double resultado =
                aiLiteracyService.calcularAiLiteracyScore(employeeId, periodo);

        assertEquals(54.0, resultado);
    }
}