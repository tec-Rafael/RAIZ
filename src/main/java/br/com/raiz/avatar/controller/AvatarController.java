package br.com.raiz.avatar.controller;

import br.com.raiz.avatar.dto.AvatarDto.Avatar;
import br.com.raiz.avatar.dto.AvatarDto.Resposta;
import br.com.raiz.avatar.service.AvatarService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/avatar")
@Tag(name = "Avatar RAIZ", description = "Avatar em pixel art do usuário da sessão: visualização e edição no perfil")
public class AvatarController {
    private final AvatarService avatares;
    public AvatarController(AvatarService avatares) { this.avatares = avatares; }

    @GetMapping("/me") public Resposta meu() { return avatares.meu(); }
    @PutMapping("/me") public Resposta salvar(@RequestBody Avatar avatar) { return avatares.salvar(avatar); }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> erro(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ProblemDetail.forStatusAndDetail(ex.getStatusCode(),
            ex.getReason() == null ? "Operação não permitida." : ex.getReason()));
    }
}
