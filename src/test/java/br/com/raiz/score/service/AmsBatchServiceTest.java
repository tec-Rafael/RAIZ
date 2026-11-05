package br.com.raiz.score.service;

import br.com.raiz.score.dto.AmsDto.*;
import br.com.raiz.score.repository.AmsSourceRepository.Fonte;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class AmsBatchServiceTest {
    private Fonte dev(String id,String empresa,double review) {
        var p=new Pessoa(id,id,empresa,empresa,"D1","Equipe","colaborador","Desenvolvedor(a) Backend");
        boolean local=empresa.equals("C1");
        return new Fonte(p,100.0,80.0,80.0,80.0,80.0,1.0,1.0,
            local?15.0:30.0,local?2.0:3.0,review,local?10.0:20.0);
    }
    @Test void reviewUsaMesmaEmpresaSemMudarEscopoDosOutrosIndicadores() {
        var locais=List.of(dev("a","C1",40),dev("b","C1",60),dev("c","C1",80),dev("d","C1",100));
        var fontes=new java.util.ArrayList<>(locais);
        fontes.addAll(List.of(dev("e","C2",0),dev("f","C2",10),dev("g","C2",20),dev("h","C2",30)));
        var antes=AmsBatchService.calcular(fontes).get(0).calculo();
        var alteradas=new java.util.ArrayList<>(locais);
        alteradas.addAll(List.of(dev("e","C2",100),dev("f","C2",100),dev("g","C2",100),dev("h","C2",100)));
        var depois=AmsBatchService.calcular(alteradas).get(0).calculo();
        assertEquals(12.5,antes.delivery().get(2).notaRelativa());
        assertEquals(antes,depois); // Revisões de outra empresa não influenciam a pessoa a.
        assertEquals(DeliveryMetricsService.normalizar(40.0,DeliveryMetricsService.REVIEW_P10,
            DeliveryMetricsService.REVIEW_P90,true),antes.delivery().get(2).notaIndividual());
        // Issue, merge e volume continuam comparados com as oito pessoas do cargo.
        assertEquals(75,antes.delivery().get(0).notaRelativa());
        assertEquals(75,antes.delivery().get(1).notaRelativa());
        assertEquals(25,antes.delivery().get(3).notaRelativa());
    }
    @Test void outraEmpresaNaoCompletaMinimoDeQuatroPessoasParaReview() {
        var fontes=List.of(dev("a","C1",40),dev("b","C1",60),dev("c","C1",80),dev("d","C2",100));
        var topicos=AmsBatchService.calcular(fontes).get(0).calculo().delivery();
        assertNull(topicos.get(2).notaRelativa());
        assertNotNull(topicos.get(0).notaRelativa());
        assertNotNull(topicos.get(1).notaRelativa());
        assertNotNull(topicos.get(3).notaRelativa());
    }
    private Pessoa pessoa(String id,String cargo){return new Pessoa(id,id,"C1","Empresa","D1","Equipe","colaborador",cargo);}
    @Test void deliverySemIndicadoresFicaNulaERedistribuiPesoDoAms(){
        var f=new Fonte(pessoa("dev","Desenvolvedor(a) Backend"),100.0,80.0,null,null,null,null,null,null,null,null,null);
        var c=AmsBatchService.calcular(List.of(f)).get(0).calculo();
        assertEquals(60,c.nota(),1e-9);assertEquals(4,c.delivery().size());
        assertTrue(c.delivery().stream().allMatch(t->t.notaIndividual()==null&&t.notaRelativa()==null));
        assertNull(c.dimensoes().get(4).utilizada());
    }
    @Test void deliveryParcialUsaSomenteNotasPreenchidas(){
        var f=new Fonte(pessoa("dev","Desenvolvedor(a) Backend"),100.0,100.0,100.0,100.0,100.0,1.0,1.0,9.2,null,null,null);
        var c=AmsBatchService.calcular(List.of(f)).get(0).calculo();
        assertEquals(100,c.nota());assertEquals(100,c.dimensoes().get(4).utilizada());
        assertEquals(1,c.delivery().get(0).pesoIndividual());assertNull(c.delivery().get(1).notaIndividual());
    }
    @Test void preservaMediaPonderadaDeLiteracyComNotasAusentes(){
        assertEquals(87.5,AmsBatchService.media(Arrays.asList(80.0,100.0,null),new double[]{.5,.3,.2}),1e-9);
    }
    @Test void finopsMedianoENotaDoNaoDevSaoCalculadosSeparadamente(){
        var a=new Fonte(pessoa("a","Analista"),100.0,100.0,100.0,100.0,100.0,2.0,4.0,null,null,null,null);
        var b=new Fonte(pessoa("b","Analista"),100.0,100.0,100.0,100.0,100.0,4.0,8.0,null,null,null,null);
        var rows=AmsBatchService.calcular(List.of(a,b));
        assertEquals(100,rows.get(0).calculo().nota());
        assertEquals(93.75,rows.get(1).calculo().nota());
        assertTrue(rows.get(1).calculo().delivery().isEmpty());
    }
}
