package br.com.raiz.score.repository;

import br.com.raiz.score.domain.AiModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiModelRepository extends JpaRepository<AiModel, String> {
}