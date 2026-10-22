package br.com.raiz.score.service;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AmsCalculatorTest {
    @Test void devComCincoDimensoesTem20PorCentoCada(){
        var c=AmsCalculator.calcular(true,100.0,80.0,60.0,40.0,20.0,List.of());
        assertEquals(60,c.nota(),1e-9);assertEquals(5,c.dimensoes().size());
        c.dimensoes().forEach(d->assertEquals(.2,d.pesoEfetivo(),1e-9));
    }
    @Test void naoDevNaoIncluiDeliveryMesmoSeForInformado(){
        var c=AmsCalculator.calcular(false,100.0,80.0,60.0,40.0,100.0,List.of());
        assertEquals(70,c.nota(),1e-9);assertEquals(4,c.dimensoes().size());
        assertTrue(c.dimensoes().stream().noneMatch(d->d.id().equals("delivery")));assertTrue(c.delivery().isEmpty());
        assertEquals(70,AmsCalculator.calcular(false,100.0,80.0,60.0,40.0,Double.NaN,List.of()).nota());
    }
    @Test void literacyNulaEntraComoZeroEPreservaAusenciaOriginal(){
        var c=AmsCalculator.calcular(false,80.0,80.0,80.0,null,null,List.of());
        assertEquals(60,c.nota(),1e-9);
        var l=c.dimensoes().get(3);assertNull(l.registrada());assertEquals(0,l.utilizada());
        assertEquals(.25,l.pesoEfetivo());assertEquals("zero_por_ausencia",l.estado());assertEquals(1,c.notasAusentes());
    }
    @Test void dimensoesAusentesRedistribuemPesosSemVirarZero(){
        var c=AmsCalculator.calcular(true,90.0,null,null,30.0,null,List.of());
        assertEquals(60,c.nota());assertEquals(.5,c.dimensoes().get(0).pesoEfetivo());
        assertNull(c.dimensoes().get(1).utilizada());assertEquals(0,c.dimensoes().get(1).pesoEfetivo());
    }
    @Test void todasAusentesNaoProduzemAmsArtificial(){
        var c=AmsCalculator.calcular(true,null,null,null,null,null,List.of());
        assertNull(c.nota());assertEquals(5,c.notasAusentes());
        assertTrue(c.dimensoes().stream().allMatch(d->d.pesoEfetivo()==0));
    }
    @Test void zeroRegistradoEValido(){
        var c=AmsCalculator.calcular(false,null,null,null,0.0,null,List.of());
        assertEquals(0,c.nota());assertEquals(1,c.dimensoes().get(3).pesoEfetivo());
    }
    @Test void somaDasContribuicoesFechaSemArredondamentoPrematuro(){
        var c=AmsCalculator.calcular(true,33.333,74.725,null,null,82.125,List.of());
        assertEquals(1,c.dimensoes().stream().mapToDouble(d->d.pesoEfetivo()).sum(),1e-9);
        assertEquals(c.nota(),c.dimensoes().stream().filter(d->d.contribuicao()!=null).mapToDouble(d->d.contribuicao()).sum(),1e-9);
    }
    @Test void rejeitaNotasInvalidas(){
        for(double n:new double[]{-1,101,Double.NaN,Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->AmsCalculator.calcular(false,n,null,null,null,null,List.of()));
    }
    @Test void classificaCargoSemConfundirQaDevopsOuGestaoComDesenvolvedor(){
        for(String cargo:List.of("Desenvolvedor(a) Backend","Desenvolvedor(a) Frontend","Engenheiro(a) de Software","Software Engineer"))assertTrue(AmsCalculator.desenvolvedor(cargo));
        for(String cargo:List.of("Analista de QA","Engenheiro(a) DevOps","Gestor","Analista de Marketing"))assertFalse(AmsCalculator.desenvolvedor(cargo));
        assertFalse(AmsCalculator.desenvolvedor(null));
    }
}
