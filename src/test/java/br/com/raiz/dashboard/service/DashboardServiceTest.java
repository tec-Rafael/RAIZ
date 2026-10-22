package br.com.raiz.dashboard.service;

import br.com.raiz.dashboard.dto.DashboardDto.*;
import br.com.raiz.dashboard.repository.DashboardRepository;
import br.com.raiz.dashboard.service.DashboardService.Consulta;
import br.com.raiz.score.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {
    private final Pessoa eu = new Pessoa("E1","Eu","C1","Empresa","D1","Equipe","GESTOR");
    private final Pessoa colega = new Pessoa("E2","Colega","C1","Empresa","D1","Equipe","COLABORADOR");
    private final Pessoa outraArea = new Pessoa("E3","Outra área","C1","Empresa","D2","Outra equipe","COLABORADOR");
    private final Pessoa outraEmpresa = new Pessoa("E4","Outra empresa","C2","Empresa 2","D3","Equipe 3","COLABORADOR");
    private final DashboardRepository repository = mock(DashboardRepository.class);
    private final DashboardService service = new DashboardService(repository,mock(AiLiteracyService.class),mock(GovernanceService.class),mock(SecurityIncidentService.class));
    private void base() {
        when(repository.pessoas()).thenReturn(List.of(eu,colega,outraArea,outraEmpresa));
        when(repository.areas("C1")).thenReturn(List.of(new Opcao("D1","Equipe"),new Opcao("D2","Outra equipe")));
        when(repository.ferramentas("C1")).thenReturn(List.of(new Opcao("T1","Ferramenta")));
        when(repository.primeiraData()).thenReturn(LocalDate.of(2025,6,1));
    }
    @Test void periodosSaoContiguosCom426DiasInclusiveAnoBissexto() {
        for (LocalDate inicio : List.of(LocalDate.of(2025,6,1),LocalDate.of(2024,1,1))) {
            var p = DashboardService.periodos(inicio);
            assertEquals(5,p.size());
            assertEquals(426,ChronoUnit.DAYS.between(p.get(0).inicio(),p.get(4).fim())+1);
            for (int i=0;i<5;i++) {
                assertEquals(i==4?66:90,p.get(i).dias());
                assertEquals(p.get(i).dias(),ChronoUnit.DAYS.between(p.get(i).inicio(),p.get(i).fim())+1);
                if(i>0) assertEquals(p.get(i-1).fim().plusDays(1),p.get(i).inicio());
            }
        }
    }
    @Test void semLogsNaoInventaPeriodos() {
        assertTrue(DashboardService.periodos(null).isEmpty());
        assertThrows(ResponseStatusException.class,() -> DashboardService.selecionarPeriodo(List.of(),5));
    }
    @Test void rejeitaPeriodoForaDoIntervalo() {
        var p = DashboardService.periodos(LocalDate.of(2025,6,1));
        assertThrows(ResponseStatusException.class,() -> DashboardService.selecionarPeriodo(p,0));
        assertThrows(ResponseStatusException.class,() -> DashboardService.selecionarPeriodo(p,6));
    }
    @Test void colaboradorVeSomenteSiMesmo() {
        base(); var f = service.filtros("colaborador","E1");
        assertEquals(List.of(eu),f.pessoas());
        assertThrows(ResponseStatusException.class,() -> DashboardService.resolverEscopo(q("colaborador","pessoal","E2",null),f));
        assertThrows(ResponseStatusException.class,() -> DashboardService.resolverEscopo(q("colaborador","empresa",null,null),f));
    }
    @Test void gestorVeDepartamentoEProprioPerfil() {
        base(); var f = service.filtros("gestor","E1");
        assertEquals(List.of(eu,colega),f.pessoas());
        assertEquals("D1",DashboardService.resolverEscopo(q("gestor","equipe",null,"D2"),f).departamento());
        assertEquals("E2",DashboardService.resolverEscopo(q("gestor","pessoal","E2",null),f).pessoa());
        assertThrows(ResponseStatusException.class,() -> DashboardService.resolverEscopo(q("gestor","pessoal","E3",null),f));
    }
    @Test void administradorIncluiSiMesmoMasNaoOutraEmpresa() {
        base(); var f = service.filtros("administrador","E1");
        assertEquals(List.of(eu,colega,outraArea),f.pessoas());
        assertEquals("E1",DashboardService.resolverEscopo(q("administrador","pessoal",null,null),f).pessoa());
        assertEquals("D2",DashboardService.resolverEscopo(q("administrador","area",null,"D2"),f).departamento());
        assertThrows(ResponseStatusException.class,() -> DashboardService.resolverEscopo(q("administrador","pessoal","E4",null),f));
        assertThrows(ResponseStatusException.class,() -> DashboardService.resolverEscopo(q("administrador","area",null,"D3"),f));
    }
    @Test void filtrosInvalidosNaoViraramConsultasIrrestritas() {
        base(); var f = service.filtros("administrador","E1");
        assertThrows(ResponseStatusException.class,() -> DashboardService.resolverEscopo(new Consulta("administrador","E1","empresa",null,null,"invalida",5),f));
        assertThrows(ResponseStatusException.class,() -> service.integrantes(q("administrador","empresa",null,null),"pessoa","","sql",0,10));
        assertThrows(ResponseStatusException.class,() -> service.integrantes(q("administrador","empresa",null,null),"pessoa","","nome",-1,10));
    }
    private Consulta q(String perfil,String escopo,String pessoa,String departamento) {
        return new Consulta(perfil,"E1",escopo,pessoa,departamento,null,5);
    }
}
