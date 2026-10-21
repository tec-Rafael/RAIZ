package br.com.raiz.score.dto;

import br.com.raiz.score.domain.Periodo;
import java.time.LocalDate;

public record PeriodoResponseDto(
        int numero,
        LocalDate inicio,
        LocalDate fim
) {
    public PeriodoResponseDto(int numero, Periodo periodo) {
        this(numero, periodo.getInicio(), periodo.getFim());
    }
}