package br.com.raiz.score.service;

import br.com.raiz.score.domain.Periodo;
import br.com.raiz.score.domain.TrainingRecord;
import br.com.raiz.score.repository.TrainingRecordRepository;
import br.com.raiz.score.dto.AiLiteracyScoreDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiLiteracyService {

    private final TrainingRecordRepository trainingRecordRepository;

    public AiLiteracyService(TrainingRecordRepository trainingRecordRepository) {
        this.trainingRecordRepository = trainingRecordRepository;
    }

    public List<TrainingRecord> buscarTreinamentosDoFuncionario(String employeeId) {
        return trainingRecordRepository.findByEmployeeId(employeeId);
    }

    public Double calcularCompletionScore(String employeeId, Periodo periodo) {

        List<TrainingRecord> treinamentos =
                trainingRecordRepository.findByEmployeeId(employeeId);

        // Cursos que já foram matriculados até o final do período
        // e ainda não foram concluídos até o final desse período.
        long matriculados = treinamentos.stream()
                .filter(t -> t.getEnrollmentDate() != null)
                .filter(t -> !t.getEnrollmentDate().isAfter(periodo.getFim()))
                .filter(t ->
                        t.getCompletionDate() == null ||
                                t.getCompletionDate().isAfter(periodo.getFim())
                )
                .count();

        // Cursos concluídos dentro do período analisado.
        long concluidos = treinamentos.stream()
                .filter(t -> "Concluido".equalsIgnoreCase(t.getCompletionStatus()))
                .filter(t -> t.getCompletionDate() != null)
                .filter(t -> !t.getCompletionDate().isBefore(periodo.getInicio()))
                .filter(t -> !t.getCompletionDate().isAfter(periodo.getFim()))
                .count();

        // Sem cursos para compor o cálculo.
        if (matriculados + concluidos == 0) {
            return null;
        }

        return ((double) concluidos / (matriculados + concluidos)) * 100;
    }

    public Double calcularTrainingScore(String employeeId, Periodo periodo) {

        List<TrainingRecord> treinamentos =
                trainingRecordRepository.findByEmployeeId(employeeId);

        List<TrainingRecord> concluidosNoPeriodo = treinamentos.stream()
                .filter(t -> "Concluido".equalsIgnoreCase(t.getCompletionStatus()))
                .filter(t -> t.getCompletionDate() != null)
                .filter(t -> !t.getCompletionDate().isBefore(periodo.getInicio()))
                .filter(t -> !t.getCompletionDate().isAfter(periodo.getFim()))
                .filter(t -> t.getScore() != null)
                .toList();

        if (concluidosNoPeriodo.isEmpty()) {
            return null;
        }

        return concluidosNoPeriodo.stream()
                .mapToDouble(TrainingRecord::getScore)
                .average()
                .orElse(0.0);
    }

    public Double calcularDiversityScore(String employeeId, Periodo periodo) {

        List<TrainingRecord> treinamentos =
                trainingRecordRepository.findByEmployeeId(employeeId);

        long categoriasConcluidas = treinamentos.stream()
                .filter(t -> "Concluido".equalsIgnoreCase(t.getCompletionStatus()))
                .filter(t -> t.getCompletionDate() != null)
                .filter(t -> !t.getCompletionDate().isBefore(periodo.getInicio()))
                .filter(t -> !t.getCompletionDate().isAfter(periodo.getFim()))
                .filter(t -> t.getCourseCategory() != null)
                .map(TrainingRecord::getCourseCategory)
                .distinct()
                .count();

        if (categoriasConcluidas == 0) {
            return null;
        }

        return Math.min((double) categoriasConcluidas / 4, 1.0) * 100;
    }

    public Double calcularAiLiteracyScore(String employeeId, Periodo periodo) {

        Double completionScore =
                calcularCompletionScore(employeeId, periodo);

        Double trainingScore =
                calcularTrainingScore(employeeId, periodo);

        Double diversityScore =
                calcularDiversityScore(employeeId, periodo);

        double soma = 0.0;
        double pesoDisponivel = 0.0;

        if (completionScore != null) {
            soma += 0.50 * completionScore;
            pesoDisponivel += 0.50;
        }

        if (trainingScore != null) {
            soma += 0.30 * trainingScore;
            pesoDisponivel += 0.30;
        }

        if (diversityScore != null) {
            soma += 0.20 * diversityScore;
            pesoDisponivel += 0.20;
        }

        return comCursos(pesoDisponivel == 0 ? null : soma / pesoDisponivel,
            trainingRecordRepository.cursosDaArvore(employeeId, periodo.getInicio(), periodo.getFim()));
    }

    static Double comCursos(Double base, long quantidade) {
        if (quantidade == 0) return base;
        return Math.min(100, (base == null ? 0 : base) + 5.0 * quantidade);
    }

    public AiLiteracyScoreDto calcularAiLiteracy(String employeeId, Periodo periodo) {

        Double completionScore =
                calcularCompletionScore(employeeId, periodo);

        Double trainingScore =
                calcularTrainingScore(employeeId, periodo);

        Double diversityScore =
                calcularDiversityScore(employeeId, periodo);

        Double aiLiteracyScore =
                calcularAiLiteracyScore(employeeId, periodo);

        return new AiLiteracyScoreDto(
                completionScore,
                trainingScore,
                diversityScore,
                aiLiteracyScore
        );
    }
}
