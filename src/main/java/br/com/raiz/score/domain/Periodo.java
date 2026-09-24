package br.com.raiz.score.domain;

import java.time.LocalDate;

public class Periodo {

    private final LocalDate inicio;
    private final LocalDate fim;

    public Periodo(LocalDate inicio, LocalDate fim) {
        this.inicio = inicio;
        this.fim = fim;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public LocalDate getFim() {
        return fim;
    }
}