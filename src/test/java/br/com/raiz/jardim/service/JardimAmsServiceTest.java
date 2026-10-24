package br.com.raiz.jardim.service;

import br.com.raiz.dashboard.dto.DashboardDto.Periodo;
import br.com.raiz.dashboard.repository.DashboardRepository;
import br.com.raiz.score.service.AmsBatchService;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JardimAmsServiceTest {
    @Test void janelaTerminaNoDiaHistoricoECacheNaoVazaParaOutroDia(){
        var repo=mock(DashboardRepository.class);var batch=mock(AmsBatchService.class);
        when(repo.primeiraData()).thenReturn(LocalDate.of(2025,6,1));
        when(batch.carregar(any())).thenReturn(List.of());
        var service=new JardimAmsService(repo,batch);
        service.notas(1,10);service.notas(1,10);service.notas(1,11);
        verify(batch,times(1)).carregar(new Periodo(1,LocalDate.of(2025,6,1),LocalDate.of(2025,6,10),10,true));
        verify(batch).carregar(new Periodo(1,LocalDate.of(2025,6,1),LocalDate.of(2025,6,11),11,true));
    }
    @Test void periodoCompletoUsaAMesmaJanelaDoDashboard(){
        var repo=mock(DashboardRepository.class);var batch=mock(AmsBatchService.class);
        when(repo.primeiraData()).thenReturn(LocalDate.of(2025,6,1));when(batch.carregar(any())).thenReturn(List.of());
        var service=new JardimAmsService(repo,batch);service.notas(1,90);
        verify(batch).carregar(new Periodo(1,LocalDate.of(2025,6,1),LocalDate.of(2025,8,29),90,false));
        assertEquals(66,service.periodo(5).dias());assertThrows(RuntimeException.class,()->service.notas(5,67));
        service.notas(1,0);verifyNoMoreInteractions(batch);
    }
}
