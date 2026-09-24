package br.com.raiz.score.service;

import br.com.raiz.score.domain.Periodo;
import br.com.raiz.score.domain.SecurityEvent;
import br.com.raiz.score.repository.SecurityEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityIncidentServiceTest {

    private SecurityEventRepository securityEventRepository;
    private SecurityIncidentService securityIncidentService;

    @BeforeEach
    void setUp() {
        securityEventRepository = mock(SecurityEventRepository.class);
        securityIncidentService =
                new SecurityIncidentService(securityEventRepository);
    }

    // ---------------------------------------------------------
    // 1. Dois eventos de severidade Alta
    // Esperado: 100 - 20 - 20 = 60
    // ---------------------------------------------------------
    @Test
    void deveCalcularScoreComDoisEventosDeAltaSeveridade() {

        SecurityEvent evento1 = criarEvento(
                "SEC00050",
                "C13-E0025",
                "Alta",
                LocalDateTime.of(2025, 9, 19, 9, 35, 28)
        );

        SecurityEvent evento2 = criarEvento(
                "SEC00051",
                "C13-E0025",
                "Alta",
                LocalDateTime.of(2025, 11, 8, 14, 57, 22)
        );

        when(securityEventRepository.findByEmployeeId("C13-E0025"))
                .thenReturn(List.of(evento1, evento2));

        Periodo periodo = new Periodo(
                LocalDateTime.of(2025, 8, 30, 0, 0).toLocalDate(),
                LocalDateTime.of(2025, 11, 27, 0, 0).toLocalDate()
        );

        Double resultado =
                securityIncidentService.calcularSecurityScore(
                        "C13-E0025",
                        periodo
                );

        assertEquals(60.0, resultado);
    }

    // ---------------------------------------------------------
    // 2. Evento Critical
    // Esperado: 100 - 40 = 60
    // ---------------------------------------------------------
    @Test
    void deveAplicarPenalidadeDeEventoCritico() {

        SecurityEvent evento = criarEvento(
                "SEC00100",
                "C01-E0001",
                "Critica",
                LocalDateTime.of(2025, 7, 10, 10, 0)
        );

        when(securityEventRepository.findByEmployeeId("C01-E0001"))
                .thenReturn(List.of(evento));

        Periodo periodo = new Periodo(
                LocalDateTime.of(2025, 6, 1, 0, 0).toLocalDate(),
                LocalDateTime.of(2025, 8, 29, 0, 0).toLocalDate()
        );

        Double resultado =
                securityIncidentService.calcularSecurityScore(
                        "C01-E0001",
                        periodo
                );

        assertEquals(60.0, resultado);
    }

    // ---------------------------------------------------------
    // 3. Evento Media
    // Esperado: 100 - 10 = 90
    // ---------------------------------------------------------
    @Test
    void deveAplicarPenalidadeDeEventoMedio() {

        SecurityEvent evento = criarEvento(
                "SEC00101",
                "C01-E0002",
                "Media",
                LocalDateTime.of(2025, 7, 15, 10, 0)
        );

        when(securityEventRepository.findByEmployeeId("C01-E0002"))
                .thenReturn(List.of(evento));

        Periodo periodo = new Periodo(
                LocalDateTime.of(2025, 6, 1, 0, 0).toLocalDate(),
                LocalDateTime.of(2025, 8, 29, 0, 0).toLocalDate()
        );

        Double resultado =
                securityIncidentService.calcularSecurityScore(
                        "C01-E0002",
                        periodo
                );

        assertEquals(90.0, resultado);
    }

    // ---------------------------------------------------------
    // 4. Evento Baixa
    // Esperado: 100 - 5 = 95
    // ---------------------------------------------------------
    @Test
    void deveAplicarPenalidadeDeEventoBaixo() {

        SecurityEvent evento = criarEvento(
                "SEC00102",
                "C01-E0003",
                "Baixa",
                LocalDateTime.of(2025, 7, 20, 10, 0)
        );

        when(securityEventRepository.findByEmployeeId("C01-E0003"))
                .thenReturn(List.of(evento));

        Periodo periodo = new Periodo(
                LocalDateTime.of(2025, 6, 1, 0, 0).toLocalDate(),
                LocalDateTime.of(2025, 8, 29, 0, 0).toLocalDate()
        );

        Double resultado =
                securityIncidentService.calcularSecurityScore(
                        "C01-E0003",
                        periodo
                );

        assertEquals(95.0, resultado);
    }

    // ---------------------------------------------------------
    // 5. Sem eventos no período
    // Esperado: 100
    // ---------------------------------------------------------
    @Test
    void deveRetornar100QuandoNaoExistemEventosNoPeriodo() {

        when(securityEventRepository.findByEmployeeId("C01-E0004"))
                .thenReturn(List.of());

        Periodo periodo = new Periodo(
                LocalDateTime.of(2025, 6, 1, 0, 0).toLocalDate(),
                LocalDateTime.of(2025, 8, 29, 0, 0).toLocalDate()
        );

        Double resultado =
                securityIncidentService.calcularSecurityScore(
                        "C01-E0004",
                        periodo
                );

        assertEquals(100.0, resultado);
    }

    // ---------------------------------------------------------
    // 6. Evento fora do período não deve ser considerado
    // ---------------------------------------------------------
    @Test
    void deveIgnorarEventosForaDoPeriodo() {

        SecurityEvent eventoAntes = criarEvento(
                "SEC00200",
                "C01-E0005",
                "Critica",
                LocalDateTime.of(2025, 5, 20, 10, 0)
        );

        SecurityEvent eventoDentro = criarEvento(
                "SEC00201",
                "C01-E0005",
                "Alta",
                LocalDateTime.of(2025, 7, 20, 10, 0)
        );

        when(securityEventRepository.findByEmployeeId("C01-E0005"))
                .thenReturn(List.of(eventoAntes, eventoDentro));

        Periodo periodo = new Periodo(
                LocalDateTime.of(2025, 6, 1, 0, 0).toLocalDate(),
                LocalDateTime.of(2025, 8, 29, 0, 0).toLocalDate()
        );

        Double resultado =
                securityIncidentService.calcularSecurityScore(
                        "C01-E0005",
                        periodo
                );

        // Apenas o evento Alta entra:
        // 100 - 20 = 80
        assertEquals(80.0, resultado);
    }

    // ---------------------------------------------------------
    // 7. Combinação de severidades
    // Baixa = 5
    // Media = 10
    // Alta = 20
    // Critica = 40
    // Total = 75
    // Score = 25
    // ---------------------------------------------------------
    @Test
    void deveCalcularCombinacaoDeSeveridades() {

        SecurityEvent baixa = criarEvento(
                "SEC00300",
                "C01-E0006",
                "Baixa",
                LocalDateTime.of(2025, 7, 1, 10, 0)
        );

        SecurityEvent media = criarEvento(
                "SEC00301",
                "C01-E0006",
                "Media",
                LocalDateTime.of(2025, 7, 2, 10, 0)
        );

        SecurityEvent alta = criarEvento(
                "SEC00302",
                "C01-E0006",
                "Alta",
                LocalDateTime.of(2025, 7, 3, 10, 0)
        );

        SecurityEvent critica = criarEvento(
                "SEC00303",
                "C01-E0006",
                "Critica",
                LocalDateTime.of(2025, 7, 4, 10, 0)
        );

        when(securityEventRepository.findByEmployeeId("C01-E0006"))
                .thenReturn(List.of(
                        baixa,
                        media,
                        alta,
                        critica
                ));

        Periodo periodo = new Periodo(
                LocalDateTime.of(2025, 6, 1, 0, 0).toLocalDate(),
                LocalDateTime.of(2025, 8, 29, 0, 0).toLocalDate()
        );

        Double resultado =
                securityIncidentService.calcularSecurityScore(
                        "C01-E0006",
                        periodo
                );

        assertEquals(25.0, resultado);
    }

    // ---------------------------------------------------------
    // 8. Score não pode ficar abaixo de zero
    // ---------------------------------------------------------
    @Test
    void scoreNaoDeveFicarAbaixoDeZero() {

        SecurityEvent critica1 = criarEvento(
                "SEC00400",
                "C01-E0007",
                "Critica",
                LocalDateTime.of(2025, 7, 1, 10, 0)
        );

        SecurityEvent critica2 = criarEvento(
                "SEC00401",
                "C01-E0007",
                "Critica",
                LocalDateTime.of(2025, 7, 2, 10, 0)
        );

        SecurityEvent critica3 = criarEvento(
                "SEC00402",
                "C01-E0007",
                "Critica",
                LocalDateTime.of(2025, 7, 3, 10, 0)
        );

        when(securityEventRepository.findByEmployeeId("C01-E0007"))
                .thenReturn(List.of(
                        critica1,
                        critica2,
                        critica3
                ));

        Periodo periodo = new Periodo(
                LocalDateTime.of(2025, 6, 1, 0, 0).toLocalDate(),
                LocalDateTime.of(2025, 8, 29, 0, 0).toLocalDate()
        );

        Double resultado =
                securityIncidentService.calcularSecurityScore(
                        "C01-E0007",
                        periodo
                );

        // 3 × 40 = 120 de penalidade.
        // O score deve ser limitado a 0.
        assertEquals(0.0, resultado);
    }

    // ---------------------------------------------------------
    // Método auxiliar para criar eventos
    // ---------------------------------------------------------
    private SecurityEvent criarEvento(
            String eventId,
            String employeeId,
            String severity,
            LocalDateTime timestamp) {

        return new SecurityEvent(
                eventId,
                "C01",
                employeeId,
                timestamp,
                "AI001",
                "Teste",
                severity,
                "POL001",
                "Bloqueado"
        );
    }
}