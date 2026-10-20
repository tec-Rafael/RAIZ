package br.com.raiz.usuario.repository;

import br.com.raiz.usuario.domain.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, String> {

    Optional<Funcionario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByCompanyId(String companyId);

    List<Funcionario> findByJobRoleIn(List<String> jobRoles);
}
