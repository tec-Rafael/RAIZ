package br.com.raiz.jardim.service;

import br.com.raiz.jardim.dto.JardimDto.*;
import br.com.raiz.jardim.repository.JardimRepository;
import br.com.raiz.usuario.service.SessaoService;
import br.com.raiz.score.service.AmsCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static br.com.raiz.jardim.service.JardimAmsService.erro;

@Service
public class JardimRankingService {
    private final JardimAmsService ams;
    private final JardimRepository repo;
    private final SessaoService sessoes;
    public JardimRankingService(JardimAmsService ams,JardimRepository repo,SessaoService sessoes){this.ams=ams;this.repo=repo;this.sessoes=sessoes;}
    private record Candidato(JardimAmsService.Nota nota,Double evolucao){ }
    @Transactional(readOnly=true)
    public Ranking ranking(int periodo,int dia,String escopo,String ordem,String grupo,String companyId,String departmentId){
        var u=sessoes.atual();
        if(!Set.of("empresa","departamento").contains(escopo)||!Set.of("ams","evolucao").contains(ordem)||!Set.of("todos","dev","demais").contains(grupo))throw erro("Filtro de ranking inválido.");
        String company=companyId==null||companyId.isBlank()?u.companyId():companyId;
        if(!u.admin()&&!company.equals(u.companyId()))throw SessaoService.proibido();
        var empresas=repo.empresas().stream().filter(e->u.admin()||e.id().equals(u.companyId())).toList();
        if(empresas.stream().noneMatch(e->e.id().equals(company)))throw erro("Empresa inválida.");
        var areas=repo.areas(company).stream().filter(a->u.admin()||Objects.equals(a.id(),u.departmentId())).toList();
        String dept=departmentId==null||departmentId.isBlank()?(company.equals(u.companyId())?u.departmentId():null):departmentId;
        if(escopo.equals("departamento")){
            if(!u.admin()&&!Objects.equals(dept,u.departmentId()))throw SessaoService.proibido();
            final String departamento=dept;
            if(dept==null||areas.stream().noneMatch(a->a.id().equals(departamento)))throw erro("Selecione uma área válida.");
        }
        final String departamento=dept;
        Set<String> participantes=repo.participantes();
        // Dia zero mostra participantes e estágios, sem antecipar notas ou recompensas.
        var base=ams.notas(periodo,dia==0?1:dia);
        if(dia==0)base=base.stream().map(n->new JardimAmsService.Nota(n.pessoa(),null,null,0,n.aplicaveis(),null)).toList();
        var notas=base.stream().filter(n->participantes.contains(n.pessoa().id()))
            .filter(n->n.pessoa().companyId().equals(company))
            .filter(n->escopo.equals("empresa")||Objects.equals(n.pessoa().departmentId(),departamento))
            .filter(n->grupo.equals("todos")||AmsCalculator.desenvolvedor(n.pessoa().cargo())==grupo.equals("dev")).toList();
        Map<String,JardimAmsService.Nota> anteriores=periodo>1&&dia>0?ams.notas(periodo-1,Math.min(dia,ams.periodo(periodo-1).dias())).stream()
            .collect(Collectors.toMap(n->n.pessoa().id(),Function.identity())):Map.of();
        var estagios=repo.estagios(periodo,dia);
        List<Candidato> elegiveis=notas.stream().filter(n->n.nivel()!=null).map(n->{
            var antes=anteriores.get(n.pessoa().id());
            Double delta=antes!=null&&antes.nivel()!=null?arredondar(n.ams()-antes.ams()):null;
            return new Candidato(n,delta);
        }).toList();
        Comparator<Candidato> sort=Comparator.comparing((Candidato c)->ordem.equals("ams")?arredondar(c.nota().ams()):c.evolucao(),Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(c->c.nota().pessoa().nome()).thenComparing(c->c.nota().pessoa().id());
        var ordenados=elegiveis.stream().filter(c->ordem.equals("ams")||c.evolucao()!=null).sorted(sort).toList();
        List<Posicao> classificados=new ArrayList<>();Double anterior=null;int posicao=0;
        for(int i=0;i<ordenados.size();i++){
            var c=ordenados.get(i);Double valor=ordem.equals("ams")?arredondar(c.nota().ams()):c.evolucao();
            if(!Objects.equals(valor,anterior))posicao=i+1;
            anterior=valor;
            classificados.add(linha(c.nota(),c.evolucao(),posicao,u.employeeId(),estagios));
        }
        Double media=elegiveis.isEmpty()?null:elegiveis.stream().mapToDouble(c->c.nota().ams()).average().orElseThrow();
        var bosque=notas.stream().sorted(Comparator.comparingInt((JardimAmsService.Nota n)->estagios.getOrDefault(n.pessoa().id(),1)).reversed()
            .thenComparing(n->n.nivel()==null?null:n.ams(),Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(n->n.pessoa().nome()))
            .limit(12).map(n->linha(n,null,0,u.employeeId(),estagios)).toList();
        var floresta=new Floresta(notas.size(),elegiveis.size(),notas.size()-elegiveis.size(),media,JardimRegras.nivel(media),bosque);
        String criterio="A floresta mostra uma amostra de até 12 árvores, priorizando o crescimento. O ranking continua ordenado pelo indicador escolhido. Somente funcionários ativos, sem o administrador, com ao menos 75% das dimensões aplicáveis registradas. Empates usam a nota exibida com duas casas e compartilham posição. Moedas e adubos não influenciam a classificação.";
        if(ordem.equals("evolucao"))criterio+=" Evolução frente ao mesmo número de dias do período anterior; P1 não tem comparação.";
        return new Ranking(escopo,ordem,periodo,dia,criterio,classificados.stream().limit(10).toList(),classificados.stream().filter(Posicao::eu).findFirst().orElse(null),floresta,empresas,areas,company,escopo.equals("departamento")?dept:null);
    }
    private static double arredondar(double n){return Math.round(n*100)/100.0;}
    private static Posicao linha(JardimAmsService.Nota n,Double delta,int posicao,String eu,Map<String,Integer> estagios){
        return new Posicao(posicao,n.pessoa().nome(),n.nivel()==null?null:arredondar(n.ams()),delta,n.nivel(),estagios.getOrDefault(n.pessoa().id(),1),n.pessoa().id().equals(eu));
    }
}
