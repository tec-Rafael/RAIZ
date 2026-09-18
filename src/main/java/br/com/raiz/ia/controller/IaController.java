package br.com.raiz.ia.controller;

import br.com.raiz.ia.dto.UtilizacaoIADto.CriarRequest;
import br.com.raiz.ia.dto.UtilizacaoIADto.Response;
import br.com.raiz.ia.service.UtilizacaoIAService;
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
@RequestMapping("/api/utilizacao-ia")
@Tag(name = "Utilização de IA", description = "Cadastro, consulta e remoção dos registros de utilização de IA.")
public class IaController {

    private final UtilizacaoIAService service;

    public IaController(UtilizacaoIAService service) {
        this.service = service;
    }

    @PostMapping("/criar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastra um novo registro de utilização de IA",
            description = "Cadastra um novo registro de utilização de IA no banco de dados."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Registro de utilização de IA criado com sucesso",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Já existe uma utilização de IA cadastrada com este ID"
            )
    })
    public Response criar(@Valid @RequestBody CriarRequest request) {
        return service.criar(request);
    }

    @GetMapping("/listar")
    @Operation(summary = "Lista todos os registros de utilização de IA cadastrados no banco")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = Response.class))
            )
    })
    public List<Response> listar() {
        return service.listar();
    }

    @GetMapping("/buscar/{id}")
    @Operation(summary = "Busca um registro de utilização de IA pelo usage_id")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Registro de utilização de IA encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Registro de utilização de IA não encontrado"
            )
    })
    public Response buscar(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @DeleteMapping("/deletar/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um registro de utilização de IA existente pelo usage_id")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Registro de utilização de IA removido com sucesso"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Registro de utilização de IA não encontrado"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Não é possível remover: existem registros vinculados a esta utilização de IA"
            )
    })
    public void deletar(@PathVariable String id) {
        service.deletar(id);
    }
}