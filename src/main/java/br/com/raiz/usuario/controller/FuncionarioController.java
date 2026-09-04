package br.com.raiz.usuario.controller;

import br.com.raiz.usuario.dto.FuncionarioDto.CriarRequest;
import br.com.raiz.usuario.dto.FuncionarioDto.LoginRequest;
import br.com.raiz.usuario.dto.FuncionarioDto.LoginResponse;
import br.com.raiz.usuario.dto.FuncionarioDto.Response;
import br.com.raiz.usuario.service.FuncionarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/funcionario")
@Tag(name = "Funcionário", description = "Cadastro, consulta e remoção de funcionários. "
        + "Apenas um funcionário com posição GESTOR pode cadastrar novos funcionários, "
        + "sempre no mesmo departamento em que o gestor atua.")
public class FuncionarioController {

    private final FuncionarioService service;

    public FuncionarioController(FuncionarioService service) {
        this.service = service;
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica um funcionário já cadastrado por e-mail e senha.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado com sucesso",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "E-mail ou senha ausentes/malformados"),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos"),
            @ApiResponse(responseCode = "403", description = "Funcionário inativo")
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/criar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastra um novo funcionário",
            description = "Somente um funcionário com posição GESTOR (identificado pelo campo gestorId) "
                    + "pode cadastrar novos funcionários. company_id e department_id não são recebidos do "
                    + "cliente: são sempre herdados do gestor responsável, encontrado no banco."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Funcionário criado com sucesso",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (campos obrigatórios ausentes ou malformados)"),
            @ApiResponse(responseCode = "403", description = "O gestorId informado não corresponde a um funcionário com posição GESTOR"),
            @ApiResponse(responseCode = "404", description = "gestorId informado não existe no banco"),
            @ApiResponse(responseCode = "409", description = "Já existe um funcionário cadastrado com este e-mail")
    })
    public Response criar(@Valid @RequestBody CriarRequest request) {
        return service.criar(request);
    }

    @GetMapping("/listar")
    @Operation(summary = "Lista todos os funcionários cadastrados no banco")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    public List<Response> listar() {
        return service.listar();
    }

    @GetMapping("/buscar/{id}")
    @Operation(summary = "Busca um funcionário pelo employee_id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funcionário encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "Funcionário não encontrado")
    })
    public Response buscar(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @DeleteMapping("/deletar/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um funcionário existente pelo employee_id")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Funcionário removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Funcionário não encontrado"),
            @ApiResponse(responseCode = "409", description = "Não é possível remover: existem issues/pull requests vinculados a este funcionário")
    })
    public void deletar(@PathVariable String id) {
        service.deletar(id);
    }
}
