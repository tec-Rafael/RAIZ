package br.com.raiz.score.repository;

import br.com.raiz.score.domain.TrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, String> {

    List<TrainingRecord> findByEmployeeId(String employeeId);
}