package br.com.raiz.score.repository;

import br.com.raiz.score.domain.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PullRequestRepository extends JpaRepository<PullRequest, String> {

    List<PullRequest> findByEmployeeId(String employeeId);
}