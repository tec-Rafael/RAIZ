package br.com.raiz.dashboard.service;

import br.com.raiz.score.dto.AmsDto.*;
import br.com.raiz.score.service.AmsCalculator;
import br.com.raiz.dashboard.service.AmsDashboardService.Consulta;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AmsDashboardServiceTest {
    Pessoa eu=new Pessoa("E1","Eu","C1","Empresa","D1","Equipe","gestor","Gestor");
    Pessoa colega=new Pessoa("E2","Colega","C1","Empresa","D1","Equipe","colaborador","Analista");
    Pessoa outraArea=new Pessoa("E3","Outra área","C1","Empresa","D2","Outra equipe","gestor","Gestor");
    Pessoa outraEmpresa=new Pessoa("E4","Outra empresa","C2","Empresa 2","D3","Equipe 3","gestor","Gestor");
    List<Pessoa> todos=List.of(eu,colega,outraArea,outraEmpresa);
    Consulta q(String perfil,String company,String gestor,String dept,String pessoa){return new Consulta(perfil,"E1",company,gestor,dept,pessoa,5,0,10,"","ams");}
    @Test void gestorSoVeDepartamentoEColaboradorSoVeSi(){
        assertEquals(List.of(eu,colega),AmsDashboardService.permitidas(todos,eu,"gestor"));
        assertEquals(List.of(eu),AmsDashboardService.permitidas(todos,eu,"colaborador"));
        var f=new Filtros(eu,List.of(eu,colega),List.of());
        assertThrows(ResponseStatusException.class,()->AmsDashboardService.selecionar(f,q("gestor","C2",null,null,null)));
        assertThrows(ResponseStatusException.class,()->AmsDashboardService.selecionar(f,q("gestor","C1",null,"D2",null)));
        assertThrows(ResponseStatusException.class,()->AmsDashboardService.selecionar(f,q("gestor","C1",null,null,"E3")));
    }
    @Test void administradorComparaEmpresasEMantemDependenciaDeGestorAreaPessoa(){
        var f=new Filtros(eu,AmsDashboardService.permitidas(todos,eu,"administrador"),List.of());
        assertEquals(4,AmsDashboardService.selecionar(f,q("administrador",null,null,null,null)).size());
        assertEquals(List.of(eu,colega),AmsDashboardService.selecionar(f,q("administrador","C1","E1",null,null)));
        assertThrows(ResponseStatusException.class,()->AmsDashboardService.selecionar(f,q("administrador","C1","E1","D2",null)));
        assertThrows(ResponseStatusException.class,()->AmsDashboardService.selecionar(f,q("administrador","C1","E4",null,null)));
        assertThrows(ResponseStatusException.class,()->AmsDashboardService.selecionar(f,q("administrador","C1","E2",null,null)));
    }
    @Test void agregadoExcluiAmsNuloMasContaAusenciasOriginais(){
        var valido=new Linha(eu,AmsCalculator.calcular(false,80.0,80.0,80.0,null,null,List.of()));
        var nulo=new Linha(colega,AmsCalculator.calcular(false,null,null,null,null,null,List.of()));
        var a=AmsDashboardService.agregar(List.of(valido,nulo));
        assertEquals(60,a.nota());assertEquals(1,a.validos());assertEquals(1,a.semAms());
        assertEquals(5,a.notasAusentes());assertEquals(8,a.dimensoesAplicaveis());
        var literacy=a.dimensoes().stream().filter(d->d.id().equals("literacy")).findFirst().orElseThrow();
        assertEquals(0,literacy.nota());assertEquals(2,literacy.zerosLiteracy());
    }
    @Test void empatesSaoExplicitosEDeterministicos(){
        var c=AmsCalculator.calcular(false,100.0,100.0,100.0,100.0,null,List.of());
        var a=AmsDashboardService.agregar(List.of(new Linha(colega,c),new Linha(eu,c)));
        assertEquals("E1",a.melhor().id());assertEquals(2,a.melhor().empatados());
    }
}
