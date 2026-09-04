package br.com.raiz.usuario.dto;

import br.com.raiz.usuario.domain.Posicao;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class FuncionarioDto {

    private FuncionarioDto() {
    }

    /** Credenciais de login — funcionalidade já existente no módulo, mantida como está. */
    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String senha
    ) {
    }

    public record LoginResponse(String employeeId, String nome, String email, Posicao posicao) {
    }

    /**
     * Dados para cadastro de um novo funcionário.
     * companyId e departmentId NÃO fazem parte do request: são herdados do
     * gestor responsável (gestorId), nunca do que o cliente informar — assim
     * um gestor só consegue cadastrar gente no próprio departamento.
     */
    public record CriarRequest(

            @NotBlank(message = "gestorId é obrigatório")
            @Size(max = 64)
            String gestorId,

            @NotBlank(message = "nome é obrigatório")
            @Size(max = 150)
            String nome,

            @NotBlank(message = "email é obrigatório")
            @Email(message = "email em formato inválido")
            @Size(max = 150)
            String email,

            @NotBlank(message = "senha é obrigatória")
            @Size(min = 6, max = 100, message = "senha deve ter entre 6 e 100 caracteres")
            String senha,

            Posicao posicao,

            @Size(max = 255)
            String jobRole,

            @Size(max = 100)
            String seniority,

            LocalDate hireDate,

            @Size(max = 100)
            String costCenter
    ) {
    }

    public record Response(
            String employeeId,
            String companyId,
            String departmentId,
            String jobRole,
            String seniority,
            LocalDate hireDate,
            String costCenter,
            Boolean active,
            String nome,
            String email,
            Posicao posicao
    ) {
    }
}
