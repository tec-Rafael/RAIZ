package br.com.raiz.assistente.controller;

import br.com.raiz.assistente.dto.AssistenteDto.Pergunta;
import br.com.raiz.assistente.dto.AssistenteDto.Resposta;
import br.com.raiz.assistente.service.AssistenteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/assistente")
@Tag(name = "Assistente RAIZ", description = "Perguntas sobre os dados do dashboard, respondidas pelos agentes de IA (Ollama)")
public class AssistenteController {
    private final AssistenteService assistente;
    public AssistenteController(AssistenteService assistente) { this.assistente = assistente; }

    @PostMapping("/perguntar")
    public Resposta perguntar(@RequestBody Pergunta pergunta) { return assistente.perguntar(pergunta); }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> erro(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ProblemDetail.forStatusAndDetail(ex.getStatusCode(),
            ex.getReason() == null ? "Operação não permitida." : ex.getReason()));
    }
}
