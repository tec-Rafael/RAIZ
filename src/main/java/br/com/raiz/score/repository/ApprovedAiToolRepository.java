package br.com.raiz.score.repository;

import br.com.raiz.score.domain.ApprovedAiTool;
import br.com.raiz.score.domain.ApprovedAiToolId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovedAiToolRepository extends JpaRepository<ApprovedAiTool, ApprovedAiToolId> {

    List<ApprovedAiTool> findByIdCompanyId(String companyId);
}