package br.com.raiz.jardim.service;

import br.com.raiz.dashboard.dto.DashboardDto.Periodo;
import br.com.raiz.dashboard.repository.DashboardRepository;
import br.com.raiz.dashboard.service.DashboardService;
import br.com.raiz.score.dto.AmsDto.Pessoa;
import br.com.raiz.score.service.AmsBatchService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service
public class JardimAmsService {
    public record Nota(Pessoa pessoa,Double ams,String nivel,int registradas,int aplicaveis,String oportunidade) { }
    private record Cache(Instant ate,List<Nota> notas) { }
    private final DashboardRepository dashboard;
    private final AmsBatchService batch;
    private final Map<String,Cache> cache=new LinkedHashMap<>();
    public JardimAmsService(DashboardRepository dashboard,AmsBatchService batch){this.dashboard=dashboard;this.batch=batch;}
    public List<Periodo> periodos(){return DashboardService.periodos(dashboard.primeiraData());}
    public Periodo periodo(int id){
        if(id<1||id>5)throw erro("Período deve estar entre 1 e 5.");
        var p=periodos();
        if(p.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Ainda não há dados para o jardim.");
        return p.get(id-1);
    }
    /** Janela acumulada até o dia histórico escolhido, usando as mesmas regras do dashboard. */
    public synchronized List<Nota> notas(int periodo,int dia){
        var p=periodo(periodo);
        if(dia<0||dia>p.dias())throw erro("Dia fora do período.");
        if(dia==0)return List.of();
        String chave=periodo+"/"+dia;
        var salvo=cache.get(chave);
        if(salvo!=null&&salvo.ate().isAfter(Instant.now()))return salvo.notas();
        var janela=new Periodo(p.id(),p.inicio(),p.inicio().plusDays(dia-1),dia,dia<p.dias()||p.parcial());
        var notas=batch.carregar(janela).stream().map(l->{
            var c=l.calculo();
            int registradas=(int)c.dimensoes().stream().filter(d->d.registrada()!=null).count();
            String oportunidade=c.dimensoes().stream().filter(d->d.registrada()!=null)
                .min(Comparator.comparingDouble(d->d.registrada())).map(d->d.nome()).orElse(null);
            return new Nota(l.pessoa(),c.nota(),JardimRegras.coberturaSuficiente(c)?JardimRegras.nivel(c.nota()):null,registradas,c.dimensoesAplicaveis(),oportunidade);
        }).toList();
        cache.entrySet().removeIf(e->e.getValue().ate().isBefore(Instant.now()));
        if(cache.size()>=32)cache.remove(cache.keySet().iterator().next());
        cache.put(chave,new Cache(Instant.now().plusSeconds(120),notas));
        return notas;
    }
    public Nota minha(String id,int periodo,int dia){
        if(dia==0)return null;
        return notas(periodo,dia).stream().filter(n->n.pessoa().id().equals(id)).findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Funcionário não encontrado."));
    }
    public static ResponseStatusException erro(String msg){return new ResponseStatusException(HttpStatus.BAD_REQUEST,msg);}
}
