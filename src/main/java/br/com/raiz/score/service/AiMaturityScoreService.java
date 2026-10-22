package br.com.raiz.score.service;

import br.com.raiz.dashboard.dto.DashboardDto;
import br.com.raiz.dashboard.repository.DashboardRepository;
import br.com.raiz.dashboard.service.DashboardService;
import br.com.raiz.score.domain.Periodo;
import br.com.raiz.score.dto.AmsDto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.temporal.ChronoUnit;

@Service
public class AiMaturityScoreService {
    private final AmsBatchService batch;
    private final DashboardRepository repository;
    public AiMaturityScoreService(AmsBatchService batch, DashboardRepository repository) {this.batch=batch;this.repository=repository;}
    public Individual calcularAMS(String employeeId, int numeroPeriodo) {
        var periodo=periodoDashboard(numeroPeriodo);
        var linha=buscar(employeeId,periodo);
        return new Individual(periodo,linha.pessoa(),linha.calculo());
    }
    public Periodo obterPeriodo(int numeroPeriodo) {
        var p=periodoDashboard(numeroPeriodo);
        return new Periodo(p.inicio(),p.fim());
    }
    private DashboardDto.Periodo periodoDashboard(int numeroPeriodo) {
        if(numeroPeriodo<1||numeroPeriodo>5)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Período deve estar entre 1 e 5.");
        var periodos=DashboardService.periodos(repository.primeiraData());
        if(periodos.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Não há logs para definir os períodos.");
        return periodos.get(numeroPeriodo-1);
    }
    public Calculo calcularAMS(String employeeId, Periodo periodo) {
        if(periodo==null||periodo.getInicio()==null||periodo.getFim()==null||periodo.getFim().isBefore(periodo.getInicio()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Período inválido.");
        return buscar(employeeId,new DashboardDto.Periodo(0,periodo.getInicio(),periodo.getFim(),
            (int)ChronoUnit.DAYS.between(periodo.getInicio(),periodo.getFim())+1,false)).calculo();
    }
    private Linha buscar(String id,DashboardDto.Periodo p) {
        return batch.carregar(p).stream().filter(l->l.pessoa().id().equals(id)).findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Funcionário não encontrado."));
    }
}
