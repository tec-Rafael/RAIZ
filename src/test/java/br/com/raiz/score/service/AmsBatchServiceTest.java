package br.com.raiz.score.service;

import br.com.raiz.score.dto.AmsDto.*;
import br.com.raiz.score.repository.AmsSourceRepository.Fonte;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class AmsBatchServiceTest {
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
