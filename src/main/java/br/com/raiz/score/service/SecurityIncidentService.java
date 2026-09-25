package br.com.raiz.score.service;

import br.com.raiz.score.domain.Periodo;
import br.com.raiz.score.domain.SecurityEvent;
import br.com.raiz.score.repository.SecurityEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecurityIncidentService {

    private final SecurityEventRepository securityEventRepository;

    public SecurityIncidentService(SecurityEventRepository securityEventRepository) {
        this.securityEventRepository = securityEventRepository;
    }

    public List<SecurityEvent> buscarEventosDoFuncionario(String employeeId) {
        return securityEventRepository.findByEmployeeId(employeeId);
    }

    public Double calcularSecurityScore(String employeeId, Periodo periodo) {

        List<SecurityEvent> eventos =
                securityEventRepository.findByEmployeeId(employeeId);

        List<SecurityEvent> eventosNoPeriodo = eventos.stream()
                .filter(evento -> evento.getTimestamp() != null)
                .filter(evento -> !evento.getTimestamp().toLocalDate()
                        .isBefore(periodo.getInicio()))
                .filter(evento -> !evento.getTimestamp().toLocalDate()
                        .isAfter(periodo.getFim()))
                .toList();

        if (eventosNoPeriodo.isEmpty()) {
            return 100.0;
        }

        double penalidade = eventosNoPeriodo.stream()
                .mapToDouble(this::calcularPenalidade)
                .sum();

        return Math.max(0, 100 - penalidade);
    }

    private double calcularPenalidade(SecurityEvent evento) {

        if (evento.getSeverity() == null) {
            return 0;
        }

        return switch (evento.getSeverity().toLowerCase()) {
            case "baixa" -> 5;
            case "media" -> 10;
            case "alta" -> 20;
            case "critica" -> 40;
            default -> 0;
        };
    }
}