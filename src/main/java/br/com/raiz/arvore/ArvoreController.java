package br.com.raiz.arvore;

import br.com.raiz.usuario.service.SessaoService;
import br.com.raiz.jardim.service.JardimAmsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/arvore")
public class ArvoreController {
    private final JdbcTemplate db;
    private final SessaoService sessao;
    private final JardimAmsService ams;
    private final org.springframework.context.ApplicationEventPublisher eventos;
    public record CursoConcluido(String funcionario) {}
    public ArvoreController(JdbcTemplate db, SessaoService sessao, JardimAmsService ams, org.springframework.context.ApplicationEventPublisher eventos) {
        this.eventos=eventos;
        this.db=db; this.sessao=sessao; this.ams=ams;
    }

    public record Novo(@Min(1) int nivel, @NotBlank @Size(max=100) String titulo,
        @NotBlank @Size(max=12000) String aula, @NotBlank @Size(max=500) String pergunta,
        @NotBlank @Size(max=250) String correta, @NotBlank @Size(max=250) String alternativa, @Pattern(regexp="semente|broto|folha|bolota") String logo) {}
    public record Resposta(@NotBlank @Size(max=250) String resposta) {}
    public record Curso(long id,int nivel,String titulo,String logo,boolean concluido,boolean aberto,String aula,String pergunta,List<String> opcoes) {}

    private void serializar(SessaoService.Usuario u) {
        db.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", r -> {}, u.companyId()+"/"+u.departmentId());
    }
    private List<Curso> cursos(SessaoService.Usuario u) {
        var linhas=db.queryForList("""
            SELECT c.*, x.curso IS NOT NULL AS concluido FROM arvore_curso c
            LEFT JOIN arvore_conclusao x ON x.curso=c.id AND x.funcionario=?
            WHERE c.empresa IS NULL OR (c.empresa=? AND c.departamento=?) ORDER BY c.nivel,c.id
            """,u.employeeId(),u.companyId(),u.departmentId());
        int atual=linhas.stream().filter(r->!(boolean)r.get("concluido")).mapToInt(r->(int)r.get("nivel")).min().orElse(Integer.MAX_VALUE);
        return linhas.stream().map(r->{
            int nivel=(int)r.get("nivel"); boolean feito=(boolean)r.get("concluido"), aberto=feito||nivel<=atual;
            var opcoes=new ArrayList<String>();
            if(aberto){opcoes.add((String)r.get("correta"));opcoes.add((String)r.get("alternativa"));Collections.shuffle(opcoes);}
            return new Curso((long)r.get("id"),nivel,(String)r.get("titulo"),(String)r.get("logo"),feito,aberto,
                aberto?(String)r.get("aula"):null,aberto?(String)r.get("pergunta"):null,opcoes);
        }).toList();
    }
    @GetMapping
    public Map<String,Object> painel(){
        var u=sessao.atual(); var p=ams.periodo(5);
        var tokens=db.queryForObject("SELECT coalesce(sum(coalesce(input_tokens,0)+coalesce(output_tokens,0)),0) FROM ai_usage_logs WHERE employee_id=? AND timestamp>=? AND timestamp<?",Long.class,u.employeeId(),p.inicio(),p.fim().plusDays(1));
        var entregas=db.queryForObject("SELECT count(*) FROM pull_requests x JOIN projects j ON j.project_id=x.project_id WHERE x.employee_id=? AND j.company_id=? AND x.merged_at>=? AND x.merged_at<?",Long.class,u.employeeId(),u.companyId(),p.inicio(),p.fim().plusDays(1));
        var feitos=db.queryForObject("SELECT count(*) FROM arvore_conclusao WHERE funcionario=?",Long.class,u.employeeId());
        return Map.of("cursos",cursos(u),"gestor",u.gestor()&&u.departmentId()!=null,"tokens",tokens,"entregas",entregas,"feitos",feitos);
    }
    @PostMapping("/cursos") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public void publicar(@Valid @RequestBody Novo n){
        var u=sessao.atual();
        if(!u.gestor()||u.departmentId()==null) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Somente o gestor do departamento pode publicar.");
        serializar(u);
        int max=db.queryForObject("SELECT coalesce(max(nivel),0) FROM arvore_curso WHERE empresa IS NULL OR (empresa=? AND departamento=?)",Integer.class,u.companyId(),u.departmentId());
        if(n.nivel()>max+1 || n.correta().strip().equalsIgnoreCase(n.alternativa().strip()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use níveis consecutivos e alternativas diferentes.");
        int inseridos=db.update("INSERT INTO arvore_curso(empresa,departamento,autor,nivel,titulo,aula,pergunta,correta,alternativa,logo) VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING",
            u.companyId(),u.departmentId(),u.employeeId(),n.nivel(),n.titulo().strip(),n.aula().strip(),n.pergunta().strip(),n.correta().strip(),n.alternativa().strip(),n.logo()==null?"semente":n.logo());
        if(inseridos==0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Este curso já existe neste nível.");
    }
    @PostMapping("/cursos/{id}/concluir") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void concluir(@PathVariable long id,@Valid @RequestBody Resposta resposta){
        var u=sessao.atual(); serializar(u);
        var curso=cursos(u).stream().filter(c->c.id()==id).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(curso.concluido()) return;
        if(!curso.aberto()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Conclua os níveis anteriores.");
        var correta=db.queryForObject("SELECT correta FROM arvore_curso WHERE id=?",String.class,id);
        if(!resposta.resposta().equals(correta)) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Releia a aula e tente novamente.");
        db.update("INSERT INTO arvore_conclusao(funcionario,curso,dia) VALUES(?,?,?) ON CONFLICT DO NOTHING",u.employeeId(),id,ams.periodo(5).fim());
        eventos.publishEvent(new CursoConcluido(u.employeeId()));
    }
}
