package br.com.raiz.score.repository;

import br.com.raiz.score.domain.TrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, String> {

    @org.springframework.data.jpa.repository.Query(value="SELECT count(*) FROM arvore_conclusao WHERE funcionario=?1 AND dia BETWEEN ?2 AND ?3", nativeQuery=true)
    long cursosDaArvore(String employeeId, java.time.LocalDate inicio, java.time.LocalDate fim);
    List<TrainingRecord> findByEmployeeId(String employeeId);
}
