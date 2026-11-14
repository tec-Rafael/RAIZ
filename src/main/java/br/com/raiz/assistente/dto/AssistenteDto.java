package br.com.raiz.assistente.dto;

import java.util.List;

/** Contratos do assistente do dashboard (caixa de pergunta do personagem). */
public final class AssistenteDto {
    private AssistenteDto() { }

    /** Pergunta e os mesmos filtros do dashboard; o Java revalida tudo com as regras de acesso. */
    public record Pergunta(String pergunta, Integer periodo, String companyId, String gestorId,
                           String departmentId, String employeeId) { }

    public record Resposta(String resposta, List<String> fontes, String modelo, Long tempoMs) { }
}
