package br.com.raiz.jardim.service;

import br.com.raiz.dashboard.dto.DashboardDto.Periodo;
import br.com.raiz.jardim.dto.JardimDto.*;
import br.com.raiz.jardim.repository.JardimRepository;
import br.com.raiz.jardim.repository.JardimRepository.*;
import br.com.raiz.score.dto.AmsDto.Pessoa;
import br.com.raiz.usuario.domain.Posicao;
import br.com.raiz.usuario.service.SessaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class JardimServiceTest {
    JardimRepository repo=mock(JardimRepository.class);
    JardimAmsService ams=mock(JardimAmsService.class);
    SessaoService sessoes=mock(SessaoService.class);
    JardimService service;
    @BeforeEach void preparar(){
        when(sessoes.atual()).thenReturn(new SessaoService.Usuario("E1","Pessoa","pessoa@raiz.local",Posicao.COLABORADOR,"C1","D1","Cargo",Instant.now()));
        when(repo.bloquear("E1")).thenReturn(new Saldo(25,1,3,1,"nenhuma"));
        when(repo.progresso("E1",1)).thenReturn(new Progresso(1,0,0,false,null,null));
        when(ams.periodo(1)).thenReturn(new Periodo(1,LocalDate.of(2025,6,1),LocalDate.of(2025,8,29),90,false));
        var pessoa=new Pessoa("E1","Pessoa","C1","Empresa","D1","Área","colaborador","Cargo");
        when(ams.minha(eq("E1"),eq(1),anyInt())).thenReturn(new JardimAmsService.Nota(pessoa,90.0,"excelente",4,4,"Governança"));
        service=spy(new JardimService(repo,ams,sessoes));doReturn(null).when(service).estado(any(),any());
    }
    @Test void recompensaDiariaVemDoAMSCalculadoNoServidor(){
        when(repo.recompensar(anyString(),anyInt(),anyInt(),any(),anyInt(),any(),any())).thenReturn(true);
        var r=service.avancar(new Avancar(1,7,3L));
        assertEquals(56,r.moedasRecebidas());assertEquals(7,r.passos().size());
        verify(ams).minha("E1",1,1);verify(ams).minha("E1",1,7);
        verify(repo).avancar("E1",1,7,56);
    }
    @Test void dataJaRecompensadaNaoGeraSaldoNovamente(){
        when(repo.recompensar(anyString(),anyInt(),anyInt(),any(),anyInt(),any(),any())).thenReturn(false);
        var r=service.avancar(new Avancar(1,1,3L));assertEquals(0,r.moedasRecebidas());
        verify(repo).avancar("E1",1,1,0);
    }
    @Test void versaoAntigaNaoExecutaCompra(){
        var ex=assertThrows(ResponseStatusException.class,()->service.comprar(new Comprar("adubo",1,2L)));
        assertEquals(409,ex.getStatusCode().value());verify(repo,never()).comprar(anyString(),anyString(),anyInt(),anyInt());
    }
    @Test void custoDaCompraNaoERecebidoDoCliente(){
        service.comprar(new Comprar("adubo",2,3L));verify(repo).comprar("E1","adubo",2,20);
        verifyNoInteractions(ams);
    }
    @Test void saldoInsuficienteEItemInexistenteSaoRecusados(){
        assertThrows(ResponseStatusException.class,()->service.comprar(new Comprar("adubo",3,3L)));
        assertThrows(ResponseStatusException.class,()->service.comprar(new Comprar("item-forjado",1,3L)));
        verify(repo,never()).comprar(anyString(),anyString(),anyInt(),anyInt());
    }
    @Test void decoracaoNaoPodeSerCompradaDuasVezes(){
        when(repo.itens("E1")).thenReturn(Set.of("pedras"));
        assertThrows(ResponseStatusException.class,()->service.comprar(new Comprar("pedras",1,3L)));
    }
    @Test void adubaPeriodoSelecionadoInclusiveArquivado(){
        when(repo.progresso("E1",1)).thenReturn(new Progresso(1,90,0,true,90.0,"excelente"));
        service.aplicar(new Aplicar(1,3L));verify(repo).aplicar("E1",1,90);
    }
    @Test void diaZeroPodeReceberAduboSemAvancarNemPremiar(){
        service.aplicar(new Aplicar(1,3L));verify(repo).aplicar("E1",1,0);
        verify(repo,never()).avancar(anyString(),anyInt(),anyInt(),anyInt());
        verify(repo,never()).recompensar(anyString(),anyInt(),anyInt(),any(),anyInt(),any(),any());
    }
    @Test void loteDeAduboValidaEstoqueAntesDeConsumir(){
        assertThrows(ResponseStatusException.class,()->service.aplicar(new Adubar(1,3L,3)));
        verify(repo,never()).aplicar(anyString(),anyInt(),anyInt());
    }
    @Test void lotePodeLiberarEstagioDeUmaVez(){
        when(repo.bloquear("E1")).thenReturn(new Saldo(25,8,3,1,"nenhuma"));
        service.aplicar(new Adubar(1,3L,8));verify(repo,times(8)).aplicar("E1",1,0);
        verify(repo).conquistar("E1","primeiro_crescimento");
    }
    @Test void zerarDelegaReinicioSemConcederMoedasNaAcao(){
        service.zerar(new Aplicar(1,3L));verify(repo).zerar("E1",1);
        verify(repo,never()).comprar(anyString(),anyString(),anyInt(),anyInt());
        verify(repo,never()).avancar(anyString(),anyInt(),anyInt(),anyInt());
        verify(repo,never()).aplicar(anyString(),anyInt(),anyInt());
    }
    @Test void zerarComVersaoAntigaNaoAlteraArvore(){
        assertThrows(ResponseStatusException.class,()->service.zerar(new Aplicar(1,2L)));
        verify(repo,never()).zerar(anyString(),anyInt());
    }
    @Test void aplicarAduboMudaCrescimentoSemEscreverAMS(){
        when(repo.progresso("E1",1)).thenReturn(new Progresso(1,10,2,false,null,null));
        service.aplicar(new Aplicar(1,3L));verify(repo).aplicar("E1",1,10);verify(repo).conquistar("E1","primeiro_crescimento");
        verify(ams).periodo(1);
        verify(ams,never()).minha(anyString(),anyInt(),anyInt());
    }
    @Test void naoConsomeAduboAposEstagioMaximo(){
        when(repo.progresso("E1",1)).thenReturn(new Progresso(1,10,16,false,null,null));
        assertThrows(ResponseStatusException.class,()->service.aplicar(new Aplicar(1,3L)));
        verify(repo,never()).aplicar(anyString(),anyInt(),anyInt());
    }
    @Test void soGuardaArvoreAoCompletarTodosOsDias(){
        when(repo.progresso("E1",1)).thenReturn(new Progresso(1,89,3,false,null,null));
        assertThrows(ResponseStatusException.class,()->service.encerrar(new Aplicar(1,3L)));
        verify(repo,never()).encerrar(anyString(),anyInt(),any(),any());
    }
    @Test void adubarNaPreviaMantemODiaConsultadoSemAvancarOJogo(){
        when(repo.progresso("E1",1)).thenReturn(new Progresso(1,20,2,false,null,null));
        service.aplicar(new Adubar(1,3L,1,10));
        verify(repo).aplicar("E1",1,10);verify(service).estado(1,10);
        verify(repo,never()).avancar(anyString(),anyInt(),anyInt(),anyInt());
        verify(repo,never()).recompensar(anyString(),anyInt(),anyInt(),any(),anyInt(),any(),any());
    }
    @Test void diaZeroNaoAntecipaNotaDoFimDoPeriodo(){
        when(repo.carteira("E1")).thenReturn(new Saldo(25,1,3,1,"nenhuma"));
        var periodo=ams.periodo(1);
        when(ams.periodos()).thenReturn(java.util.List.of(periodo));
        when(ams.minha("E1",1,0)).thenReturn(null);
        doCallRealMethod().when(service).estado(any(),any());
        var e=service.estado(1,0);assertNull(e.arvore().ams());assertNull(e.arvore().nivel());
        verify(ams).minha("E1",1,0);verify(ams,never()).minha("E1",1,90);
    }
}
