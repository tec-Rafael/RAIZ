package br.com.raiz.score.repository;

import br.com.raiz.score.domain.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, String> {

    List<SecurityEvent> findByEmployeeId(String employeeId);
}