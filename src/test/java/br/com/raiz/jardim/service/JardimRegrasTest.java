package br.com.raiz.jardim.service;

import br.com.raiz.score.service.AmsCalculator;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JardimRegrasTest {
    @Test void faixasTemLimitesExplicitos(){
        assertEquals("baixo",JardimRegras.nivel(59.99));assertEquals("medio",JardimRegras.nivel(60.0));
        assertEquals("medio",JardimRegras.nivel(69.99));assertEquals("alto",JardimRegras.nivel(70.0));
        assertEquals("alto",JardimRegras.nivel(84.99));assertEquals("excelente",JardimRegras.nivel(85.0));
        assertEquals("excelente",JardimRegras.nivel(100.0));assertNull(JardimRegras.nivel(null));
        assertThrows(IllegalArgumentException.class,()->JardimRegras.nivel(Double.NaN));
    }
    @Test void ausenciaNaoViraMoedaNemArvoreSaudavel(){
        var incompleto=AmsCalculator.calcular(false,100.0,null,null,null,null,List.of());
        assertFalse(JardimRegras.coberturaSuficiente(incompleto));
        assertEquals(2,JardimRegras.recompensaDiaria(null));
        assertEquals(2,JardimRegras.recompensaDiaria(59.9));
        assertEquals(4,JardimRegras.recompensaDiaria(60.0));
        assertEquals(6,JardimRegras.recompensaDiaria(70.0));
        assertEquals(8,JardimRegras.recompensaDiaria(85.0));
        assertTrue(JardimRegras.coberturaSuficiente(AmsCalculator.calcular(false,100.0,90.0,80.0,null,null,List.of())));
        assertFalse(JardimRegras.coberturaSuficiente(AmsCalculator.calcular(true,100.0,90.0,80.0,null,null,List.of())));
        assertTrue(JardimRegras.coberturaSuficiente(AmsCalculator.calcular(true,100.0,90.0,80.0,null,60.0,List.of())));
    }
    @Test void crescimentoEAcumuladoNaoCompraSaude(){
        assertEquals(1,JardimRegras.estagio(2));assertEquals(2,JardimRegras.estagio(3));
        assertEquals(2,JardimRegras.estagio(7));assertEquals(3,JardimRegras.estagio(8));assertEquals(4,JardimRegras.estagio(16));
        assertEquals("baixo",JardimRegras.nivel(30.0));assertEquals(10,JardimRegras.preco("adubo"));
        assertEquals(3,JardimRegras.metaAdubo(0));assertEquals(8,JardimRegras.metaAdubo(3));assertEquals(16,JardimRegras.metaAdubo(8));
    }
    @Test void recompensasSoDependemDaFaixa(){
        assertEquals(2,JardimRegras.moedas("baixo"));assertEquals(4,JardimRegras.moedas("medio"));
        assertEquals(6,JardimRegras.moedas("alto"));assertEquals(8,JardimRegras.moedas("excelente"));
        assertEquals(0,JardimRegras.preco("item-forjado"));
    }
}
