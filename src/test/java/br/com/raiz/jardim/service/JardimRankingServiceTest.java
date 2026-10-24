package br.com.raiz.jardim.service;

import br.com.raiz.jardim.dto.JardimDto.Opcao;
import br.com.raiz.jardim.repository.JardimRepository;
import br.com.raiz.score.dto.AmsDto.Pessoa;
import br.com.raiz.usuario.domain.Posicao;
import br.com.raiz.usuario.service.SessaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JardimRankingServiceTest {
    JardimAmsService ams=mock(JardimAmsService.class);
    JardimRepository repo=mock(JardimRepository.class);
    SessaoService sessoes=mock(SessaoService.class);
    JardimRankingService service;
    @BeforeEach void preparar(){
        when(sessoes.atual()).thenReturn(new SessaoService.Usuario("E1","Ana","ana@raiz.local",Posicao.GESTOR,"C1","D1","Cargo",Instant.now()));
        when(repo.empresas()).thenReturn(List.of(new Opcao("C1","Minha empresa"),new Opcao("C2","Outra empresa")));
        when(repo.areas("C1")).thenReturn(List.of(new Opcao("D1","Minha área"),new Opcao("D2","Outra área")));
        when(repo.participantes()).thenReturn(Set.of("E1","E2","E3"));
        when(repo.estagios(1,10)).thenReturn(Map.of("E1",1,"E2",4));
        when(ams.notas(1,10)).thenReturn(List.of(nota("E1","Ana","D1",86.001,"excelente"),nota("E2","Bianca","D1",86.004,"excelente"),nota("E3","Carlos","D2",90.0,"excelente")));
        service=new JardimRankingService(ams,repo,sessoes);
    }
    JardimAmsService.Nota nota(String id,String nome,String area,Double ams,String nivel){return new JardimAmsService.Nota(new Pessoa(id,nome,"C1","Empresa",area,"Área","colaborador","Analista"),ams,nivel,4,4,"Governança");}
    @Test void naoLiberaOutraEmpresaNemOutroDepartamento(){
        assertEquals(403,assertThrows(ResponseStatusException.class,()->service.ranking(1,10,"empresa","ams","todos","C2",null)).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->service.ranking(1,10,"departamento","ams","todos","C1","D2")).getStatusCode().value());
    }
    @Test void empatesCompartilhamPosicaoETamanhoNaoDesempata(){
        var r=service.ranking(1,10,"departamento","ams","todos","C1","D1");
        assertEquals(2,r.destaques().size());assertEquals(1,r.destaques().get(0).posicao());assertEquals(1,r.destaques().get(1).posicao());
        assertEquals("Ana",r.destaques().get(0).nome());assertEquals(1,r.destaques().get(0).estagio());
        assertEquals(4,r.destaques().get(1).estagio());assertNotNull(r.minhaPosicao());
        assertEquals(1,r.empresas().size());assertEquals(1,r.areas().size());
    }
    @Test void primeiroPeriodoNaoInventaEvolucao(){
        var r=service.ranking(1,10,"empresa","evolucao","todos","C1",null);
        assertTrue(r.destaques().isEmpty());assertNull(r.minhaPosicao());assertEquals(3,r.floresta().comDados());
    }
    @Test void notasSemCoberturaNaoEntramNoRankingMasContinuamNaFloresta(){
        when(ams.notas(1,10)).thenReturn(List.of(nota("E1","Ana","D1",50.0,null)));
        var r=service.ranking(1,10,"departamento","ams","todos","C1","D1");
        assertTrue(r.destaques().isEmpty());assertNull(r.floresta().ams());assertEquals(1,r.floresta().semDados());assertNull(r.floresta().arvores().get(0).ams());
    }
    @Test void diaZeroMostraArvoresSemAnteciparNotas(){
        when(ams.notas(1,1)).thenReturn(java.util.List.of(nota("E1","Ana","D1",86.0,"excelente"),nota("E2","Bianca","D1",70.0,"alto")));
        when(repo.estagios(1,0)).thenReturn(java.util.Map.of("E2",4));
        var r=service.ranking(1,0,"departamento","ams","todos","C1","D1");
        assertEquals(2,r.floresta().pessoas());assertEquals(4,r.floresta().arvores().get(0).estagio());
        assertEquals(0,r.floresta().comDados());assertNull(r.floresta().ams());assertTrue(r.destaques().isEmpty());
    }
}
