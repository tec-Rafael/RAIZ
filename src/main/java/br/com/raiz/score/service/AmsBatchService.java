package br.com.raiz.score.service;

import br.com.raiz.dashboard.dto.DashboardDto.Periodo;
import br.com.raiz.score.dto.AmsDto.*;
import br.com.raiz.score.repository.AmsSourceRepository;
import br.com.raiz.score.repository.AmsSourceRepository.Fonte;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static br.com.raiz.score.service.DeliveryMetricsService.*;

@Service
public class AmsBatchService {
    private final AmsSourceRepository repository;
    private record Cache(Instant validade, List<Linha> linhas) { }
    private final Map<String,Cache> cache = new LinkedHashMap<>();
    public AmsBatchService(AmsSourceRepository repository) { this.repository=repository; }

    /** Cache interno de 30 segundos. O escopo de acesso é aplicado antes de qualquer resposta. */
    public synchronized List<Linha> carregar(Periodo periodo) {
        String chave=periodo.inicio()+"/"+periodo.fim();
        Cache atual=cache.get(chave);
        if(atual!=null && atual.validade().isAfter(Instant.now())) return atual.linhas();
        List<Linha> linhas=calcular(repository.carregar(periodo));
        cache.entrySet().removeIf(e->e.getValue().validade().isBefore(Instant.now()));
        if(cache.size()>=5) cache.remove(cache.keySet().iterator().next());
        cache.put(chave,new Cache(Instant.now().plusSeconds(30),linhas));
        return linhas;
    }

    static List<Linha> calcular(List<Fonte> fontes) {
        Double refToken=mediana(fontes,Fonte::custoToken),refRequest=mediana(fontes,Fonte::custoRequest);
        Map<String,List<Fonte>> cargos=fontes.stream().filter(f->AmsCalculator.desenvolvedor(f.pessoa().cargo()))
            .collect(Collectors.groupingBy(f->f.pessoa().cargo()));
        List<Linha> linhas=new ArrayList<>();
        for(Fonte f:fontes) {
            boolean dev=AmsCalculator.desenvolvedor(f.pessoa().cargo());
            List<Topico> topicos=List.of(); Double delivery=null;
            if(dev) {
                List<Fonte> grupo=cargos.get(f.pessoa().cargo());
                // O ajuste e a nota relativa de revisão usam a mesma empresa e cargo.
                List<Fonte> grupoReview=grupo.stream()
                    .filter(x->Objects.equals(x.pessoa().companyId(),f.pessoa().companyId())).toList();
                List<Double> individuais=Arrays.asList(normalizar(f.issueDias(),ISSUE_P10,ISSUE_P90,false),
                    normalizar(f.mergeDias(),MERGE_P10,MERGE_P90,false),normalizar(f.reviewRate(),REVIEW_P10,REVIEW_P90,true),
                    normalizar(f.volume(),PR_P10,PR_P90,true));
                List<Double> relativas=Arrays.asList(relativa(f.issueDias(),grupo,Fonte::issueDias,false),
                    relativa(f.mergeDias(),grupo,Fonte::mergeDias,false),relativa(ajustada(f,grupoReview),grupoReview,x->ajustada(x,grupoReview),true),
                    relativa(f.volume(),grupo,Fonte::volume,true));
                Double individual=mediaDelivery(individuais),relativo=mediaDelivery(relativas);
                if(individual==null) delivery=relativo;
                else if(relativo==null) delivery=individual;
                else delivery=.7*individual+.3*relativo;
                delivery=arredondar(delivery);
                String[] ids={"issue","merge","review","volume"};
                String[] nomes={"Resolução de issues","Integração de PRs","Taxa de revisão","Volume de PRs"};
                double[] pesos={.35,.25,.2,.2};
                List<Topico> itens=new ArrayList<>();
                for(int i=0;i<4;i++) itens.add(new Topico(ids[i],nomes[i],individuais.get(i),relativas.get(i),pesos[i],
                    pesoTopico(individuais,pesos,i),pesoTopico(relativas,pesos,i)));
                topicos=List.copyOf(itens);
            }
            Double literacy=media(Arrays.asList(f.completion(),f.training(),f.diversity()),new double[]{.5,.3,.2});
            Double finops=media(Arrays.asList(eficiencia(refToken,f.custoToken()),eficiencia(refRequest,f.custoRequest())),new double[]{.5,.5});
            linhas.add(new Linha(f.pessoa(),AmsCalculator.calcular(dev,f.seguranca(),f.governanca(),finops,literacy,delivery,topicos)));
        }
        return List.copyOf(linhas);
    }
    private static double pesoTopico(List<Double> notas,double[] pesos,int i) {
        if(notas.get(i)==null) return 0;
        double soma=0; for(int j=0;j<notas.size();j++) if(notas.get(j)!=null) soma+=pesos[j];
        return pesos[i]/soma;
    }
    private static Double mediaDelivery(List<Double> x) { return calcularMediaPonderada(x.get(0),x.get(1),x.get(2),x.get(3)); }
    static Double media(List<Double> notas,double[] pesos) {
        double total=0,peso=0;
        for(int i=0;i<notas.size();i++) if(notas.get(i)!=null){total+=notas.get(i)*pesos[i];peso+=pesos[i];}
        return peso==0?null:total/peso;
    }
    private static Double eficiencia(Double referencia,Double custo) { return referencia==null||custo==null||custo<=0?null:Math.min(100,referencia/custo*100); }
    private static Double mediana(List<Fonte> dados,Function<Fonte,Double> getter) {
        var validos=dados.stream().map(getter).filter(Objects::nonNull).sorted().toList();
        if(validos.isEmpty())return null; int meio=validos.size()/2;
        return validos.size()%2==0?(validos.get(meio-1)+validos.get(meio))/2:validos.get(meio);
    }
    private static Double ajustada(Fonte f,List<Fonte> grupo) {
        var media=grupo.stream().filter(x->!x.pessoa().id().equals(f.pessoa().id())).map(Fonte::reviewRate)
            .filter(Objects::nonNull).mapToDouble(Double::doubleValue).average();
        return media.isEmpty()||f.volume()==null||f.reviewRate()==null?null:(f.reviewRate()*f.volume()+5*media.getAsDouble())/(f.volume()+5);
    }
    private static Double relativa(Double nota,List<Fonte> grupo,Function<Fonte,Double> getter,boolean maiorMelhor) {
        return calcularNotaRelativa(nota,grupo.stream().map(getter).filter(Objects::nonNull).toList(),maiorMelhor);
    }
}
