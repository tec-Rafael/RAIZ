package br.com.raiz.score.repository;

import br.com.raiz.score.domain.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, String> {

    List<Issue> findByEmployeeId(String employeeId);
}