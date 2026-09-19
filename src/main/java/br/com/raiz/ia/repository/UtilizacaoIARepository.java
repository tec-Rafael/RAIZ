package br.com.raiz.ia.repository;

import br.com.raiz.ia.domain.UtilizacaoIA;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilizacaoIARepository
        extends JpaRepository<UtilizacaoIA, String> {}