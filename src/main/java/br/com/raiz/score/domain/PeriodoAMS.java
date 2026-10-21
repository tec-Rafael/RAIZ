package br.com.raiz.score.domain;

import java.time.LocalDate;

public class PeriodoAMS {

    private PeriodoAMS() {
        // Impede a instanciação da classe
    }

    public static Periodo obterPeriodo(int numeroPeriodo) {

        LocalDate inicioBase = LocalDate.of(2025, 6, 1);
        LocalDate fimBase = LocalDate.of(2026, 7, 31);

        if (numeroPeriodo < 1 || numeroPeriodo > 5) {
            throw new IllegalArgumentException(
                    "Período inválido. Informe um número entre 1 e 5."
            );
        }

        LocalDate inicio = inicioBase.plusDays(
                (numeroPeriodo - 1L) * 90
        );

        LocalDate fim = inicio.plusDays(89);

        if (fim.isAfter(fimBase)) {
            fim = fimBase;
        }

        return new Periodo(inicio, fim);
    }
}