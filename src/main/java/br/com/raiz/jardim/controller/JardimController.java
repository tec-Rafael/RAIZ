package br.com.raiz.jardim.controller;

import br.com.raiz.jardim.dto.JardimDto.*;
import br.com.raiz.jardim.service.JardimService;
import br.com.raiz.jardim.service.JardimRankingService;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/jardim")
@Tag(name="Jardim RAIZ",description="Gamificação do próprio usuário, com calendário histórico, economia transacional e ranking resumido")
public class JardimController {
    private final JardimService jardim;
    private final JardimRankingService ranking;
    public JardimController(JardimService jardim,JardimRankingService ranking){this.jardim=jardim;this.ranking=ranking;}
    @GetMapping("/estado") public Estado estado(@RequestParam(required=false) Integer periodo,@RequestParam(required=false) Integer dia){return jardim.estado(periodo,dia);}
    @PostMapping("/avancar") public Atualizacao avancar(@Valid @RequestBody Avancar request){return jardim.avancar(request);}
    @PostMapping("/comprar") public Atualizacao comprar(@Valid @RequestBody Comprar request){return jardim.comprar(request);}
    @PostMapping("/adubar") public Atualizacao adubar(@Valid @RequestBody Adubar request){return jardim.aplicar(request);}
    @PostMapping("/zerar") public Atualizacao zerar(@Valid @RequestBody Aplicar request){return jardim.zerar(request);}
    @PostMapping("/equipar") public Atualizacao equipar(@Valid @RequestBody Equipar request){return jardim.equipar(request);}
    @PostMapping("/encerrar") public Atualizacao encerrar(@Valid @RequestBody Aplicar request){return jardim.encerrar(request);}
    @GetMapping("/ranking") public Ranking ranking(@RequestParam int periodo,@RequestParam int dia,
        @RequestParam(defaultValue="empresa") String escopo,@RequestParam(defaultValue="ams") String ordem,
        @RequestParam(defaultValue="todos") String grupo,@RequestParam(required=false) String companyId,@RequestParam(required=false) String departmentId){
        return ranking.ranking(periodo,dia,escopo,ordem,grupo,companyId,departmentId);
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> erro(ResponseStatusException ex){return ResponseEntity.status(ex.getStatusCode()).body(ProblemDetail.forStatusAndDetail(ex.getStatusCode(),ex.getReason()==null?"Operação não permitida.":ex.getReason()));}
}
