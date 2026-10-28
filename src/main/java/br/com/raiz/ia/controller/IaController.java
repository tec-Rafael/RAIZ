package br.com.raiz.ia.controller;

import br.com.raiz.ia.dto.UtilizacaoIADto.CriarRequest;
import br.com.raiz.ia.dto.UtilizacaoIADto.Destaques;
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
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/utilizacao-ia")
@Tag(name = "Utilização de IA", description = "Cadastro, consulta e remoção dos registros de utilização de IA.")
public class IaController {

    private final UtilizacaoIAService service;
    private final br.com.raiz.usuario.service.SessaoService sessoes;

    public IaController(UtilizacaoIAService service,br.com.raiz.usuario.service.SessaoService sessoes) {
        this.service = service;
        this.sessoes = sessoes;
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
        sessoes.exigirAdmin();
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
    public Slice<Response> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        int paginaSegura = Math.max(page, 0);
        int tamanhoSeguro = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(
                paginaSegura,
                tamanhoSeguro,
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        return service.listar(pageable);
    }

    @GetMapping("/destaques")
    @Operation(
            summary = "Destaques de uso de IA (top N)",
            description = "Retorna apenas as IAs mais e menos usadas e os funcionários que mais e menos usam IA, "
                    + "já calculados no banco. Uso = soma de request_count. "
                    + "Use companyId para filtrar por empresa (opcional)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Destaques retornados com sucesso",
                    content = @Content(schema = @Schema(implementation = Destaques.class))
            )
    })
    public Destaques destaques(
            @RequestParam(required = false) String companyId,
            @RequestParam(defaultValue = "5") int limite) {

        sessoes.exigirAdmin();
        int limiteSeguro = Math.min(Math.max(limite, 1), 20);

        return service.destaques(companyId, limiteSeguro);
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
        var registro=service.buscarPorId(id);
        sessoes.exigirPessoa(registro.employeeId());
        return registro;
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
        sessoes.exigirAdmin();
        service.deletar(id);
    }
}
