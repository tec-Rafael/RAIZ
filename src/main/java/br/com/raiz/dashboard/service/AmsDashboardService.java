package br.com.raiz.dashboard.service;

import br.com.raiz.dashboard.dto.DashboardDto.Periodo;
import br.com.raiz.dashboard.repository.DashboardRepository;
import br.com.raiz.score.dto.AmsDto.*;
import br.com.raiz.score.repository.AmsSourceRepository;
import br.com.raiz.score.service.AmsBatchService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly=true)
public class AmsDashboardService {
    private final DashboardRepository dashboard;
    private final AmsSourceRepository repository;
    private final AmsBatchService batch;
    public AmsDashboardService(DashboardRepository dashboard,AmsSourceRepository repository,AmsBatchService batch) {
        this.dashboard=dashboard;this.repository=repository;this.batch=batch;
    }
    public List<Pessoa> usuarios(){return repository.pessoas();}
    public Filtros filtros(String perfil,String usuarioId){
        List<Pessoa> pessoas=repository.pessoas();
        Pessoa usuario=referencia(pessoas,usuarioId);
        return new Filtros(usuario,permitidas(pessoas,usuario,perfil),DashboardService.periodos(dashboard.primeiraData()));
    }
    public record Consulta(String perfil,String usuarioId,String companyId,String gestorId,String departmentId,
                            String employeeId,int periodo,int page,int size,String busca,String ordem) { }
    public Painel painel(Consulta q){
        if(q.page()<0||q.size()<1||q.size()>100||q.busca().length()>150||!Set.of("ams","nome").contains(q.ordem()))throw erro("Paginação, busca ou ordenação inválida.");
        Filtros filtros=filtros(q.perfil(),q.usuarioId());
        Periodo periodo=DashboardService.selecionarPeriodo(filtros.periodos(),q.periodo());
        List<Pessoa> selecionadas=selecionar(filtros,q);
        Set<String> ids=selecionadas.stream().map(Pessoa::id).collect(Collectors.toSet());
        List<List<Linha>> periodos=filtros.periodos().stream().map(p->batch.carregar(p).stream()
            .filter(l->ids.contains(l.pessoa().id())).toList()).toList();
        List<Ponto> evolucao=new ArrayList<>();
        for(int i=0;i<5;i++)evolucao.add(ponto(filtros.periodos().get(i),periodos.get(i)));
        List<Linha> atual=periodos.get(q.periodo()-1);
        Agregado resumo=agregar(atual);
        List<Ponto> destaque=new ArrayList<>();
        if(resumo.melhor()!=null){
            for(int i=0;i<5;i++)destaque.add(ponto(filtros.periodos().get(i),periodos.get(i).stream()
                .filter(l->l.pessoa().id().equals(resumo.melhor().id())).toList()));
        }
        Comparator<Linha> comparator=q.ordem().equals("nome")?Comparator.comparing(l->l.pessoa().nome()):
            Comparator.comparing((Linha l)->l.calculo().nota(),Comparator.nullsLast(Comparator.reverseOrder()));
        List<Linha> encontradas=atual.stream().filter(l->l.pessoa().nome().toLowerCase(Locale.ROOT).contains(q.busca().toLowerCase(Locale.ROOT)))
            .sorted(comparator.thenComparing(l->l.pessoa().id())).toList();
        var pagina=new Pagina(encontradas.stream().skip((long)q.page()*q.size()).limit(q.size()).toList(),encontradas.size(),q.page(),q.size());
        List<Faixa> distribuicao=new ArrayList<>();
        for(int i=0;i<5;i++){
            final int inicio=i*20;
            int quantidade=(int)atual.stream().map(l->l.calculo().nota()).filter(Objects::nonNull)
                .filter(n->n>=inicio && (inicio==80?n<=100:n<inicio+20)).count();
            distribuicao.add(new Faixa(inicio+"–"+(inicio+20)+(inicio==80?" (incl. 100)":""),quantidade));
        }
        distribuicao.add(new Faixa("AMS indisponível",resumo.semAms()));
        return new Painel(periodo,resumo,atual.size()==1?atual.get(0).calculo():null,evolucao,
            q.perfil().equals("administrador")?grupos(periodos,filtros.periodos(),p->p.companyId(),Pessoa::empresa):List.of(),
            q.perfil().equals("colaborador")?List.of():grupos(periodos,filtros.periodos(),p->p.companyId()+"/"+p.departmentId(),
                p->p.empresa()+" · "+Objects.toString(p.departamento(),"Sem departamento")),destaque,pagina,distribuicao,
            "Média aritmética dos AMS disponíveis, com peso igual por pessoa. AI Literacy ausente usa zero no AMS e continua identificada como ausente. Confira a cobertura e as notas utilizadas na composição.");
    }
    /** Linhas AMS dos colegas de área do usuário (mesma empresa e departamento, sem ele) no período.
     *  Uso interno do assistente de IA: o colaborador nunca recebe essas linhas, só médias e contagens anônimas. */
    public List<Linha> colegasDeArea(String usuarioId,int periodo){
        List<Pessoa> pessoas=repository.pessoas();
        Pessoa u=referencia(pessoas,usuarioId);
        if(u.departmentId()==null)return List.of();
        List<Periodo> periodos=DashboardService.periodos(dashboard.primeiraData());
        DashboardService.selecionarPeriodo(periodos,periodo);
        return batch.carregar(periodos.get(periodo-1)).stream()
            .filter(l->!l.pessoa().id().equals(u.id())&&Objects.equals(l.pessoa().companyId(),u.companyId())
                &&Objects.equals(l.pessoa().departmentId(),u.departmentId())).toList();
    }
    /** Agregado (média, cobertura e médias por dimensão) de um conjunto de linhas. */
    public static Agregado agregado(List<Linha> linhas){return agregar(linhas);}
    /** Pessoas do recorte com as mesmas validações do painel (usado pelo assistente de IA). */
    public List<Pessoa> pessoasDaConsulta(Consulta q){return selecionar(filtros(q.perfil(),q.usuarioId()),q);}
    public static List<Pessoa> permitidas(List<Pessoa> pessoas,Pessoa u,String perfil){
        if(!Set.of("colaborador","gestor","administrador").contains(perfil))throw erro("Perfil inválido.");
        return pessoas.stream().filter(p->switch(perfil){
            case "colaborador"->p.id().equals(u.id());
            case "gestor"->p.id().equals(u.id())||(Objects.equals(p.companyId(),u.companyId())&&u.departmentId()!=null&&Objects.equals(p.departmentId(),u.departmentId()));
            default->true;
        }).toList();
    }
    static List<Pessoa> selecionar(Filtros f,Consulta q){
        List<Pessoa> pessoas=f.pessoas();
        String company=limpo(q.companyId()),gestor=limpo(q.gestorId()),dept=limpo(q.departmentId()),employee=limpo(q.employeeId());
        if(company!=null&&pessoas.stream().noneMatch(p->p.companyId().equals(company)))throw negado();
        if(company!=null)pessoas=pessoas.stream().filter(p->p.companyId().equals(company)).toList();
        if(gestor!=null){
            Pessoa g=pessoas.stream().filter(p->p.id().equals(gestor)).findFirst().orElseThrow(AmsDashboardService::negado);
            if(!"gestor".equalsIgnoreCase(g.posicao())&&!(q.perfil().equals("gestor")&&g.id().equals(f.usuario().id())))throw negado();
            if(g.departmentId()==null)throw erro("Gestor sem departamento cadastrado.");
            pessoas=pessoas.stream().filter(p->p.companyId().equals(g.companyId())&&Objects.equals(p.departmentId(),g.departmentId())).toList();
        }
        if(dept!=null){
            if(pessoas.stream().noneMatch(p->Objects.equals(p.departmentId(),dept)))throw negado();
            pessoas=pessoas.stream().filter(p->Objects.equals(p.departmentId(),dept)).toList();
        }
        if(employee!=null){
            if(pessoas.stream().noneMatch(p->p.id().equals(employee)))throw negado();
            pessoas=pessoas.stream().filter(p->p.id().equals(employee)).toList();
        }
        return pessoas;
    }
    static Agregado agregar(List<Linha> linhas){
        Double nota=media(linhas.stream().map(l->l.calculo().nota()).toList());
        int validos=(int)linhas.stream().filter(l->l.calculo().nota()!=null).count();
        List<Media> dimensoes=new ArrayList<>();
        for(String id:List.of("security","governance","finops","literacy","delivery")){
            var itens=linhas.stream().flatMap(l->l.calculo().dimensoes().stream()).filter(d->d.id().equals(id)).toList();
            var notas=itens.stream().map(Dimensao::utilizada).toList();
            dimensoes.add(new Media(id,itens.isEmpty()?id:itens.get(0).nome(),media(notas),
                (int)notas.stream().filter(Objects::nonNull).count(),(int)itens.stream().filter(d->d.registrada()==null).count(),
                (int)itens.stream().filter(d->d.estado().equals("zero_por_ausencia")).count()));
        }
        var ordenadas=linhas.stream().filter(l->l.calculo().nota()!=null).sorted(Comparator.comparing((Linha l)->l.calculo().nota()).reversed()
            .thenComparing(l->l.pessoa().id())).toList();
        Destaque melhor=null;
        if(!ordenadas.isEmpty()){
            var primeiro=ordenadas.get(0);
            int empates=(int)ordenadas.stream().filter(l->Math.abs(l.calculo().nota()-primeiro.calculo().nota())<1e-9).count();
            melhor=new Destaque(primeiro.pessoa().id(),primeiro.pessoa().nome(),primeiro.calculo().nota(),empates);
        }
        return new Agregado(nota,linhas.size(),validos,linhas.size()-validos,
            linhas.stream().mapToInt(l->l.calculo().notasAusentes()).sum(),linhas.stream().mapToInt(l->l.calculo().dimensoesAplicaveis()).sum(),dimensoes,melhor);
    }
    private static Double media(List<Double> notas){return notas.stream().filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().stream().boxed().findFirst().orElse(null);}
    private static Ponto ponto(Periodo p,List<Linha> linhas){return new Ponto(p,agregar(linhas),linhas.size()==1?linhas.get(0).calculo():null);}
    private static List<Grupo> grupos(List<List<Linha>> periodos,List<Periodo> datas,Function<Pessoa,String> chave,Function<Pessoa,String> nome){
        Map<String,String> grupos=new TreeMap<>();periodos.get(0).forEach(l->grupos.put(chave.apply(l.pessoa()),nome.apply(l.pessoa())));
        List<Grupo> result=new ArrayList<>();
        grupos.forEach((id,n)->{
            List<Ponto> pontos=new ArrayList<>();
            for(int i=0;i<5;i++)pontos.add(ponto(datas.get(i),periodos.get(i).stream().filter(l->chave.apply(l.pessoa()).equals(id)).toList()));
            result.add(new Grupo(id,n,pontos));
        });return result;
    }
    private static Pessoa referencia(List<Pessoa> pessoas,String id){return pessoas.stream().filter(p->p.id().equals(id)).findFirst()
        .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Pessoa de referência não encontrada."));}
    private static String limpo(String s){return s==null||s.isBlank()?null:s;}
    private static ResponseStatusException negado(){return new ResponseStatusException(HttpStatus.FORBIDDEN,"Filtro fora da visão demonstrada.");}
    private static ResponseStatusException erro(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
}
